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
import android.util.Log;
import android.widget.Toast;

// Credits to Alex Lockwood for original model of a task fragment
public class TaskFragment extends Fragment {

	public static final long SYNC_TIMEOUT = 5000; // Timeout in milliseconds for syncing
	public static final String TASK_COMPLETE = "tan.chesley.rssfeedreader.taskcomplete";

	public static interface TaskCallbacks {

		void onPreExecute();

		void onProgressUpdate();

		void onCancelled();

		void onPostExecute();
	}

	private String[] FEEDS;
	private TaskCallbacks mCallbacks;
	private GetRssFeedTask mTask;
	private InputStream feedStream;
	private boolean aborted = false;
	private boolean taskCompleted = false;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		FEEDS = new SourcesManager(activity).getSources();
		mCallbacks = (TaskCallbacks) ((RSSFeed) activity)
				.getHeadlinesFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		if (savedInstanceState != null) {
			taskCompleted = savedInstanceState.getBoolean(TASK_COMPLETE);
		}
		else {
			if (!taskCompleted) {
				Log.e("TaskFragment", "Starting new sync task.");
				mTask = new GetRssFeedTask();
				mTask.execute(FEEDS);
				new Handler().postDelayed(new Runnable() {

					public void run() {
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
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(TASK_COMPLETE, taskCompleted);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		abortInputStreams();
		if (mTask != null) {
			mTask.cancel(true);
		}
		//Log.e("TaskFragment","Destroying TaskFragment.");
	}

	public void abortInputStreams() {
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
		long longRequestTime = 4000;
		private RSSHandler myRSSHandler;

		@Override
		protected Void doInBackground(String... urls) {
			try {

				SAXParserFactory mySAXParserFactory = SAXParserFactory
						.newInstance();
				SAXParser mySAXParser = mySAXParserFactory.newSAXParser();
				XMLReader myXMLReader = mySAXParser.getXMLReader();
				myRSSHandler = new RSSHandler(this, FEEDS.length, getActivity());
				myXMLReader.setContentHandler(myRSSHandler);
				InputSource myInputSource;
				URL url;
				for (String s : urls) {
					if (isCancelled())
						break;
					publishProgress((Void) null);
					url = new URL(s);
					feedStream = url.openStream();
					if (aborted || isCancelled()) {
						break;
					}
					Log.e("Feed", "Syncing feed " + s);
					myInputSource = new InputSource(feedStream);
					try {
						if (aborted || isCancelled()) {
							break;
						}
						myRSSHandler.notifyCurrentSource(url.toString());
						myXMLReader.parse(myInputSource);
					} catch (SAXException e) {
						if (aborted) {
							break;
						}
						continue;
					}
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
				Log.e("Feed", "Cannot connect to RSS feed!");
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				Log.e("Feed", "Cannot connect to RSS feed!");
			} catch (SAXException e) {
				e.printStackTrace();
				Log.e("Feed", "Cannot connect to RSS feed!");
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("Feed", "Cannot connect to RSS feed!");
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mCallbacks != null) {
				mCallbacks.onPreExecute();
			}
		}

		@Override
		protected void onPostExecute(Void v) {
			if (mCallbacks != null && !taskCompleted) {
				deliverData();
				mCallbacks.onPostExecute();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mCallbacks != null && !taskCompleted) {
				deliverData();
				mCallbacks.onCancelled();
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			if (System.currentTimeMillis() - startTime > longRequestTime) {
				if (!isCancelled()) {
					if (mCallbacks != null) {
						mCallbacks.onProgressUpdate();
					}
				}
			}
		}

		private void deliverData() {
			if (myRSSHandler == null) {
				Log.e("TaskFragment", "Data delivery failed... myRSSHandler was null!");
				return;
			}
			if (mCallbacks == null) {
				Log.e("TaskFragment", "Data delivery failed... mCallbacks was null!");
				return;
			}
			((HeadlinesFragment) mCallbacks).setRssData(myRSSHandler.getData());
			taskCompleted = true;
		}
	}
}
