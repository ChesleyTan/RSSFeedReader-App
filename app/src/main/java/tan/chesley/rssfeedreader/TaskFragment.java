package tan.chesley.rssfeedreader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

// Credits to Alex Lockwood for original model of a task fragment
public class TaskFragment extends Fragment {

    public long SYNC_TIMEOUT; // Timeout in milliseconds for syncing
    public static final String TASK_COMPLETE = "tan.chesley.rssfeedreader.taskcomplete";

    public static interface TaskCallbacks {

        void onPreExecute ();

        void onProgressUpdate ();

        void onCancelled ();

        void onPostExecute ();
    }

    private String[] FEEDS;
    private TaskCallbacks mCallbacks;
    private GetRssFeedTask mTask;
    private InputStream feedStream;
    private boolean aborted = false;
    private boolean taskCompleted = false;

    @Override
    public void onAttach (Activity activity) {
        super.onAttach(activity);
        FEEDS = new SourcesOpenHelper(activity).getEnabledSources();
        mCallbacks = (TaskCallbacks) ((RSSFeed) activity)
            .getHeadlinesFragment();
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        SYNC_TIMEOUT = 1000 * PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(SettingsActivity.KEY_PREF_SYNC_TIMEOUT, getResources().getInteger(R.integer.sync_timeout_default));
        if (savedInstanceState != null) {
            taskCompleted = savedInstanceState.getBoolean(TASK_COMPLETE);
        }
        else {
            if (!taskCompleted) {
                Log.e("TaskFragment", "Starting new sync task.");
                mTask = new GetRssFeedTask();
                mTask.execute(FEEDS);
                new Handler().postDelayed(new Runnable() {

                    public void run () {
                        if (mTask.getStatus() != AsyncTask.Status.FINISHED) {
                            abortInputStreams();
                            mTask.cancel(true);
                            mTask.onCancelled();
                            if (mCallbacks != null) {
                                ((HeadlinesFragment) mCallbacks).showToast(
                                    "Sync connection timeout",
                                    Toast.LENGTH_SHORT);
                            }
                        }
                        else {
                            Log.e("Feed", "Feed sync completed successfully.");
                        }
                    }
                }, SYNC_TIMEOUT);
            }
        }
    }

    @Override
    public void onDetach () {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TASK_COMPLETE, taskCompleted);
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        abortInputStreams();
        if (mTask != null) {
            mTask.cancel(true);
        }
        //Log.e("TaskFragment","Destroying TaskFragment.");
    }

    public void abortInputStreams () {
        if (feedStream != null) {
            try {
                aborted = true;
                Log.e("Feed", "InputStream interrupted.");
                feedStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class GetRssFeedTask extends AsyncTask<String, Void, Void> {

        long startTime = System.currentTimeMillis();
        final long longRequestTime = 4000;
        private RSSHandler myRSSHandler;

        @Override
        protected Void doInBackground (String... urls) {
            SAXParserFactory mySAXParserFactory = SAXParserFactory
                .newInstance();
            SAXParser mySAXParser = null;
            XMLReader myXMLReader = null;
            try {
                mySAXParser = mySAXParserFactory.newSAXParser();
                myXMLReader = mySAXParser.getXMLReader();
            } catch (SAXException e) {
                e.printStackTrace();
                Log.e("Feed", "Could not connect to RSS feed! Error 1");
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                Log.e("Feed", "Could not connect to RSS feed! Error 2.");
            }
            myRSSHandler = new RSSHandler(this, FEEDS.length, SYNC_TIMEOUT, getActivity());
            myXMLReader.setContentHandler(myRSSHandler);
            InputSource myInputSource;
            URL url = null;
            for (String s : urls) {
                // Stop syncing if the task has been aborted or cancelled
                if (aborted || isCancelled()) {
                    break;
                }
                // Callback to show Toast to update user of progress
                publishProgress((Void) null);
                Log.e("Feed", "Syncing feed " + s);
                // Try to create a new URL from the String
                try {
                    url = new URL(s);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.e("Feed", "Cannot connect to RSS feed! Error 3.");
                    continue;
                }
                // Try to open an InputStream with the URL
                try {
                    feedStream = url.openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Feed", "Cannot connect to RSS feed! Error 4.");
                    continue;
                }
                if (aborted || isCancelled()) {
                    break;
                }
                // Create a new InputSource using the InputStream
                myInputSource = new InputSource(feedStream);

                    if (aborted || isCancelled()) {
                        break;
                    }
                    // Notify the RSSHandler of the current source's URL
                    myRSSHandler.notifyCurrentSource(url.toString());
                    // Try to parse XML from the InputSource
                    try {
                        myXMLReader.parse(myInputSource);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("Feed", "Cannot connect to RSS feed! Error 5.");
                    } catch (SAXException e) {
                        e.printStackTrace();
                        Log.e("Feed", "Cannot connect to RSS feed! Error 6.");
                        if (aborted || isCancelled()) {
                            break;
                        }
                        else {
                            continue;
                        }
                    }
            }
            return null;
        }

            @Override
            protected void onPreExecute () {
                super.onPreExecute();
                if (mCallbacks != null) {
                    mCallbacks.onPreExecute();
                }
            }

            @Override
            protected void onPostExecute (Void v){
                if (mCallbacks != null && !taskCompleted) {
                    deliverData();
                    mCallbacks.onPostExecute();
                }
            }

            @Override
            protected void onCancelled () {
                super.onCancelled();
                if (mCallbacks != null && !taskCompleted) {
                    deliverData();
                    mCallbacks.onCancelled();
                }
            }

            @Override
            protected void onProgressUpdate (Void...values){
                super.onProgressUpdate(values);
                if (System.currentTimeMillis() - startTime > longRequestTime) {
                    if (!isCancelled()) {
                        if (mCallbacks != null && !taskCompleted) {
                            mCallbacks.onProgressUpdate();
                        }
                    }
                }
            }

        private void deliverData () {
            if (myRSSHandler == null) {
                Log.e("TaskFragment", "Data delivery failed... myRSSHandler was null!");
                return;
            }
            if (mCallbacks == null) {
                Log.e("TaskFragment", "Data delivery failed... mCallbacks was null!");
                return;
            }
            ((HeadlinesFragment) mCallbacks).setRssData(myRSSHandler.getData(), true);
            taskCompleted = true;
        }
    }
}
