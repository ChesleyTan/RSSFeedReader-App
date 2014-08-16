package tan.chesley.rssfeedreader;

import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class HeadlinesFragment extends ListFragment implements
                                                    TaskFragment.TaskCallbacks {

    public static final String PARSED_FEED_DATA = "tan.chesley.rssfeedreader.parsedfeeddata";
    public static final String ARTICLE_ID = "tan.chesley.rssfeedreader.articleid";
    public static final String TASK_FRAGMENT = "tan.chesley.rssfeedreader.taskfragment";
    public static final String RESTORED_DATA_FROM_DB = "tan.chesley.rssfeedreader.restoreddata";
    public static final String SYNCING = "tan.chesley.rssfeedreader.syncing";
    public static final int ARTICLE_VIEW_INTENT = 0;
    public static ArrayList<MyMap> data = new ArrayList<MyMap>();
    private static HeadlinesFragment singleton;
    private HeadlinesAdapter adapter;
    private TaskFragment mTaskFragment;
    private boolean restoredDataFromDB = false;
    private boolean syncing = false;
    private boolean resumingFromArticleViewActivity = false;
    private ProgressBar syncProgressBar;
    private LinearLayout syncProgressBarContainer;

    public HeadlinesFragment () {
        singleton = this;
    }

    public static HeadlinesFragment getInstance () {
        return singleton;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleton = this;

        int titleId = getResources().getIdentifier("action_bar_title", "id",
                                                   "android");
        TextView title = (TextView) getActivity().findViewById(titleId);
        if (title != null) {
            getActivity().setTitle(getResources().getString(R.string.feeds));
            title.setTextColor(getResources().getColor(
                (R.color.AppPrimaryTextColor)));
        }

        if (savedInstanceState != null) {
            restoredDataFromDB = savedInstanceState.getBoolean(RESTORED_DATA_FROM_DB);
            syncing = savedInstanceState.getBoolean(SYNCING);
        }

        //Log.e("HeadlinesFragment", "Instance: Calling onCreate method.");
    }

    @Override
    public void onResume () {
        super.onResume();
        if (!resumingFromArticleViewActivity && data != null) {
            updateFeedView();
        }
        resumingFromArticleViewActivity = false;
        //Log.e("HeadlinesFragment", "Instance: Calling onResume method.");
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        // Log.e("Instance", "Instance: Inflating new fragment.");
        View view = inflater.inflate(R.layout.fragment_rssfeed, container,
                                     false);
        syncProgressBar = (ProgressBar) view.findViewById(R.id.syncProgressBar);
        syncProgressBarContainer = (LinearLayout) view
            .findViewById(R.id.progressBarContainer);
        Assert.assertNotNull(syncProgressBar);
        Assert.assertNotNull(syncProgressBarContainer);
        if (savedInstanceState != null) {
            ArrayList<MyMap> tmp = savedInstanceState
                .getParcelableArrayList(PARSED_FEED_DATA);
            if (tmp == null) {
                Log.e("Instance", "Instance: no saved data found.");
            }
            else {
                // restored ArrayList actually contains HashMaps rather than
                // MyMaps which can lead to a ClassCastException later on if
                // the HashMaps are not manually converted to MyMaps
                data.clear();
                for (HashMap<String, RSSDataBundle> m : tmp) {
                    data.add(MyMap.createFromHashMap(m));
                }
                Log.e("Instance", "Restored Instance State.");
                updateFeedView();
            }
        }
        if (!restoredDataFromDB && data.size() == 0) {
            restoredDataFromDB = true;
            for (RSSDataBundle rdBundle : new RSSDataBundleOpenHelper(getActivity()).getBundles()) {
                data.add(MyMap.createFromRSSDataBundle(rdBundle));
            }
            updateFeedView();
            Log.e("HeadlinesFragment", "Restored databased data.");
        }
        return view;
    }

    @Override
    public void onStart () {
        super.onStart();
        if (syncing) {
            toggleProgressBar();
        }
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        // Log.e("HeadlinesFragment", "Saving instance state.");
        outState.putParcelableArrayList(PARSED_FEED_DATA, clone(data));
        outState.putBoolean(RESTORED_DATA_FROM_DB, restoredDataFromDB);
        outState.putBoolean(SYNCING, syncing);
    }

    public void syncFeeds () {
        if (!syncing) {
            // Check if connected to network
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                showToast("Could not connect to network", Toast.LENGTH_SHORT);
                return;
            }
            // Use syncing flag to prevent calling multiple sync tasks
            // concurrently
            syncing = true;
            // Show progress bar
            toggleProgressBar();
            // Create retained TaskFragment that will execute the sync AsyncTask
            mTaskFragment = new TaskFragment();
            getActivity().getFragmentManager().beginTransaction()
                         .add(mTaskFragment, TASK_FRAGMENT).commit();
            showToast("Syncing feeds...", Toast.LENGTH_LONG);
        }
    }

    public void updateFeedView () {
        // Sort headlines by a given criteria before updating the screen
        int sortType = Integer.parseInt(PreferenceManager
                                            .getDefaultSharedPreferences(getActivity()).getString(
                "pref_feedSortBy_type", "0"));
        filterOutdated(data);
        sortHeadlinesBy(sortType, data);
        updateListAdapter();
    }

    private void updateListAdapter () {
        if (getListAdapter() == null) {
            adapter = new HeadlinesAdapter(clone(data));
            setListAdapter(adapter);
        }
        else {
            // If adapter already exists, update its data and notify it of
            // changes
            adapter.clear();
            for (MyMap m : data) {
                adapter.add(m);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private class HeadlinesAdapter extends ArrayAdapter<MyMap> {

        public static final int SORT_BY_NONE = 0;
        public static final int SORT_BY_TITLE = 1;
        public static final int SORT_BY_SOURCE = 2;
        public static final int SORT_BY_DATE = 3;

        public HeadlinesAdapter (ArrayList<MyMap> myData) {
            super(getActivity(), R.layout.feed_list_item, myData);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                    R.layout.feed_list_item, parent, false);
            }
            HashMap<String, RSSDataBundle> dataMap = getItem(position);
            TextView headlineTextView = (TextView) convertView
                .findViewById(android.R.id.text1);
            TextView articleTextView = (TextView) convertView
                .findViewById(android.R.id.text2);
            TextView sourceTextView = (TextView) convertView
                .findViewById(R.id.sourceTextView);
            TextView dateTextView = (TextView) convertView
                .findViewById(R.id.dateTextView);
            RSSDataBundle rdBundle = dataMap.values().iterator().next();
            headlineTextView.setText(rdBundle.getTitle());
            sourceTextView.setText(rdBundle.getSourceTitle());
            dateTextView.setText(rdBundle.getUserPreferredDateFormat(getContext()));
            articleTextView.setText(rdBundle.getDescription());
            return convertView;
        }
    }

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        HashMap<String, RSSDataBundle> map = adapter.getItem(position);
        // Start new ArticleView activity, passing to it the id of the clicked
        // article
        Intent intent = new Intent(getActivity(), ArticleView.class);
        intent.putExtra(ARTICLE_ID, map.values().iterator().next().getId());
        startActivityForResult(intent, ARTICLE_VIEW_INTENT);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        // Receive current article index from ArticleView activity
        if (requestCode == ARTICLE_VIEW_INTENT) {
            resumingFromArticleViewActivity = true;
            Assert.assertNotNull(data);
            getListView().setSelection(
                data.getIntExtra(ArticleView.ARTICLE_SELECTED_KEY, 0));
        }
    }

    public ArrayList<MyMap> getRssData () {
        return data;
    }

    public void setRssData (final ArrayList<MyMap> in, boolean appendToExistingData) {
        // Called by TaskFragment to update the RSS data fetched by RSSHandler
        if (appendToExistingData) {
            data.addAll(in);
        }
        else {
            data = in;
        }
        Runnable databaseData = new Runnable() {
            @Override
            public void run () {
                RSSDataBundleOpenHelper dbHelper = new RSSDataBundleOpenHelper(getActivity());
                for (MyMap map : in) {
                    dbHelper.addBundle(map.values().iterator().next());
                }
            }
        };
        Thread databaseDataThread = new Thread(databaseData);
        databaseDataThread.start();

    }

    public void showToast (String s, int toastDurationFlag) {
        Toast toast = Toast.makeText(getActivity(), s,
                                     toastDurationFlag);
        TextView toastTextView = (TextView) toast.getView().findViewById(
            android.R.id.message);
        toastTextView.setTextColor(getResources().getColor(
            R.color.AppPrimaryTextColor));
        toast.getView().getBackground().setAlpha(180);
        toast.show();
    }

    public ArrayList<MyMap> filterOutdated(ArrayList<MyMap> list) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean enforceAgeLimit = prefs.getBoolean("pref_articleAgeLimitCheckBox", false);
        if (enforceAgeLimit) {
            long articleAgeLimit = RSSHandler.MILLISECONDS_IN_A_DAY * prefs.getInt(SettingsActivity.KEY_PREF_ARTICLE_AGE_LIMIT, getResources().getInteger(R.integer.article_age_limit_default));
            Iterator<MyMap> iterator = list.iterator();
            while (iterator.hasNext()) {
                long now = System.currentTimeMillis();
                long articlePubDate = iterator.next().values().iterator().next().getAge();
                if (now - articlePubDate > articleAgeLimit) {
                    iterator.remove();
                }
                else if (articlePubDate > now) {
                    iterator.remove();
                }
            }
        }
        return list;
    }

    public static ArrayList<MyMap> sortHeadlinesBy (final int sortCriteria,
                                                    ArrayList<MyMap> list) {
        Collections.sort(list, new Comparator<MyMap>() {

            public int compare (MyMap first, MyMap second) {
                if (sortCriteria == HeadlinesAdapter.SORT_BY_SOURCE) {
                    String firstTitle = first.values().iterator().next()
                                             .getSource();
                    String secondTitle = second.values().iterator().next()
                                               .getSource();
                    return firstTitle.compareTo(secondTitle);
                }
                else if (sortCriteria == HeadlinesAdapter.SORT_BY_TITLE) {
                    String firstTitle = first.values().iterator().next()
                                             .getTitle();
                    String secondTitle = second.values().iterator().next()
                                               .getTitle();
                    return firstTitle.compareTo(secondTitle);
                }
                else if (sortCriteria == HeadlinesAdapter.SORT_BY_DATE) {
                    Calendar firstCalendar = first.values().iterator().next()
                                                  .getCalendar();
                    Calendar secondCalendar = second.values().iterator().next()
                                                    .getCalendar();
                    return -1 * firstCalendar.compareTo(secondCalendar);
                }
                return 0;
            }
        });
        return list;
    }

    public void toggleProgressBar () {
        if (syncProgressBar.getVisibility() == View.GONE && syncing) {
            syncProgressBarContainer.setVisibility(View.VISIBLE);
            syncProgressBar.setVisibility(View.VISIBLE);
            getListView().setVisibility(View.GONE);
        }
        else if (syncProgressBar.getVisibility() == View.VISIBLE
            && !syncing) {
            syncProgressBarContainer.setVisibility(View.GONE);
            syncProgressBar.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPreExecute () {

    }

    @Override
    public void onProgressUpdate () {
        showToast("Still working...", Toast.LENGTH_SHORT);
    }

    @Override
    public void onCancelled () {
        Log.e("Feed", "Feed sync timeout reached. Thread stopped.");
        feedSyncTaskFinishedCallback();
    }

    @Override
    public void onPostExecute () {
        feedSyncTaskFinishedCallback();
    }

    public void feedSyncTaskFinishedCallback () {
        // Update list adapter
        updateFeedView();
        syncing = false;
        // Hide the progress bar
        toggleProgressBar();
        // Remove the TaskFragment
        FragmentManager fragMan = getActivity().getFragmentManager();
        fragMan.beginTransaction()
               .remove(fragMan.findFragmentByTag(TASK_FRAGMENT))
               .addToBackStack(null).commit();
    }

    private ArrayList<MyMap> clone (ArrayList<MyMap> a) {
        ArrayList<MyMap> newList = new ArrayList<MyMap>();
        for (MyMap m : a) {
            newList.add(m);
        }
        return newList;
    }
}
