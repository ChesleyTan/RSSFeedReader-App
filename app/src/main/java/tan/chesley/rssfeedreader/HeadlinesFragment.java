package tan.chesley.rssfeedreader;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.Iterator;

public class HeadlinesFragment extends ListFragment implements
                                                    TaskFragment.TaskCallbacks {

    public static final String PARSED_FEED_DATA = "tan.chesley.rssfeedreader.parsedfeeddata";
    public static final String ARTICLE_ID = "tan.chesley.rssfeedreader.articleid";
    public static final String TASK_FRAGMENT = "tan.chesley.rssfeedreader.taskfragment";
    public static final String RESTORED_DATA_FROM_DB = "tan.chesley.rssfeedreader.restoreddata";
    public static final String SYNCING = "tan.chesley.rssfeedreader.syncing";
    public static final int ARTICLE_VIEW_INTENT = 0;
    public static ArrayList<RSSDataBundle> data = new ArrayList<RSSDataBundle>();
    private static HeadlinesFragment singleton;
    private HeadlinesAdapter adapter;
    private TaskFragment mTaskFragment;
    private boolean restoredDataFromDB = false;
    private boolean syncing = false;
    private boolean resumingFromArticleViewActivity = false;
    private ProgressBar syncProgressBar;
    private LinearLayout syncProgressBarContainer;
    private LinearLayout action_goToPreviousUnread;
    private LinearLayout action_goToNextUnread;
    private Toast toast;

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

        getActivity().getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
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
        // If resuming from preference activity, update the ListView
        if (!syncing && !resumingFromArticleViewActivity && data != null) {
            int maxDbSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(SettingsActivity.KEY_PREF_MAX_DATABASE_SIZE, getResources().getInteger(R.integer.max_database_size_default));
            new RSSDataBundleOpenHelper(getActivity()).constrainDatabaseSize(maxDbSize);
            updateFeedView();
            toggleBottomActionBar();
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
        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_bottomActionBarCheckBox", true)) {
            attachBottomActionBarListeners(view);
        }
        else {
            view.findViewById(R.id.bottomActionBar).setVisibility(View.GONE);
        }
        if (savedInstanceState != null) {
            ArrayList<RSSDataBundle> tmp = savedInstanceState
                .getParcelableArrayList(PARSED_FEED_DATA);
            if (tmp == null) {
                //Log.e("Instance", "Instance: no saved data found.");
            }
            else {
                data.clear();
                for (RSSDataBundle rdBundle : tmp) {
                    data.add(rdBundle);
                }
                // Log.e("Instance", "Restored Instance State.");
                updateFeedView();
            }
        }
        if (!restoredDataFromDB && data.size() == 0) {
            restoredDataFromDB = true;
            for (RSSDataBundle rdBundle : new RSSDataBundleOpenHelper(getActivity()).getBundles()) {
                data.add(rdBundle);
            }
            updateFeedView();
            Log.e("HeadlinesFragment", "Restored databased data.");
        }
        return view;
    }

    @Override
    public void onStart () {
        super.onStart();

        // Fade-in animation
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setFillAfter(true);
        animation.setDuration(500);
        getActivity().findViewById(android.R.id.content).startAnimation(animation);

        // Allow ListView to receive events originating from a child view
        registerForContextMenu(getListView());
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
            for (RSSDataBundle rdBundle : data) {
                adapter.add(rdBundle);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private class HeadlinesAdapter extends ArrayAdapter<RSSDataBundle> {

        public static final int SORT_BY_NONE = 0;
        public static final int SORT_BY_TITLE = 1;
        public static final int SORT_BY_SOURCE = 2;
        public static final int SORT_BY_DATE = 3;

        public HeadlinesAdapter (ArrayList<RSSDataBundle> myData) {
            super(getActivity(), R.layout.feed_list_item, myData);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                    R.layout.feed_list_item, parent, false);
            }
            RSSDataBundle rdBundle = getItem(position);
            TextView headlineTextView = (TextView) convertView
                .findViewById(android.R.id.text1);
            TextView articleTextView = (TextView) convertView
                .findViewById(android.R.id.text2);
            TextView sourceTextView = (TextView) convertView
                .findViewById(R.id.sourceTextView);
            TextView dateTextView = (TextView) convertView
                .findViewById(R.id.dateTextView);
            headlineTextView.setText(rdBundle.getTitle());
            sourceTextView.setText(rdBundle.getSourceTitle());
            dateTextView.setText(rdBundle.getUserPreferredDateFormat(getContext()));
            articleTextView.setText(rdBundle.getDescription(getContext()));
            // Toggle the color indicator to signify if read/unread
            if (!rdBundle.isRead()) {
                convertView.findViewById(R.id.colorIndicator).setVisibility(View.VISIBLE);
            }
            else {
                convertView.findViewById(R.id.colorIndicator).setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        RSSDataBundle rdBundle = adapter.getItem(position);
        // Start new ArticleView activity, passing to it the id of the clicked article
        Intent intent = new Intent(getActivity(), ArticleView.class);
        intent.putExtra(ARTICLE_ID, rdBundle.getId());
        startActivityForResult(intent, ARTICLE_VIEW_INTENT);
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int listViewIndex = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        menu.setHeaderTitle(((RSSDataBundle)getListView().getItemAtPosition(listViewIndex)).getTitle());
        String[] contextMenuItems = getResources().getStringArray(R.array.feed_list_item_context_menu);
        for (int i = 0;i < contextMenuItems.length;i++) {
            menu.add(Menu.NONE, i, i, contextMenuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected (MenuItem item) {
        // Get position of the item
        int listViewIndex = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        int contextMenuIndex = item.getItemId();
        // If the user chose "mark as read"
        if (contextMenuIndex == 0) {
            RSSDataBundle rdBundle = (RSSDataBundle) getListAdapter().getItem(listViewIndex);
            // Mark the article as read
            RSSDataBundle.markAsRead(getActivity(), rdBundle);
            // Redraw the ListView
            adapter.notifyDataSetChanged();
        }
        // If the user chooses "Open in browser"
        if (contextMenuIndex == 1) {
            RSSDataBundle rdBundle = (RSSDataBundle) getListAdapter().getItem(listViewIndex);
            // Mark the article as read
            RSSDataBundle.markAsRead(getActivity(), rdBundle);
            String url = rdBundle.getLink();
            // Log.e("URL Open", "URL: " + url);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        return true;
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        // Receive current article index from ArticleView activity
        if (requestCode == ARTICLE_VIEW_INTENT) {
            resumingFromArticleViewActivity = true;
            Assert.assertNotNull(data);
            // Force redraw of the ListView to update the Views for read/unread articles
            adapter.notifyDataSetChanged();
            getListView().setSelection(
                data.getIntExtra(ArticleView.ARTICLE_SELECTED_KEY, 0));
        }
    }

    public ArrayList<RSSDataBundle> getRssData () {
        return data;
    }

    public void setRssData (final ArrayList<RSSDataBundle> in, boolean appendToExistingData) {
        // Called by TaskFragment to update the RSS data fetched by RSSHandler
        if (appendToExistingData) {
            data.addAll(in);
        }
        else {
            data = in;
        }
    }

    public void showToast (String s, int toastDurationFlag) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(getActivity(), s,
                                     toastDurationFlag);
        TextView toastTextView = (TextView) toast.getView().findViewById(
            android.R.id.message);
        toastTextView.setTextColor(getResources().getColor(
            R.color.AppPrimaryTextColor));
        toast.getView().setBackgroundColor(getResources().getColor(R.color.AppDefaultBackgroundColor));
        toast.getView().getBackground().setAlpha(180);
        toast.show();
    }

    public ArrayList<RSSDataBundle> filterOutdated(ArrayList<RSSDataBundle> list) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean enforceAgeLimit = prefs.getBoolean("pref_articleAgeLimitCheckBox", false);
        if (enforceAgeLimit) {
            long articleAgeLimit = RSSHandler.MILLISECONDS_IN_A_DAY * prefs.getInt(SettingsActivity.KEY_PREF_ARTICLE_AGE_LIMIT, getResources().getInteger(R.integer.article_age_limit_default));
            Iterator<RSSDataBundle> iterator = list.iterator();
            while (iterator.hasNext()) {
                long now = System.currentTimeMillis();
                long articlePubDate = iterator.next().getAge();
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

    public static ArrayList<RSSDataBundle> sortHeadlinesBy (final int sortCriteria,
                                                    ArrayList<RSSDataBundle> list) {
        Collections.sort(list, new Comparator<RSSDataBundle>() {

            public int compare (RSSDataBundle first, RSSDataBundle second) {
                if (sortCriteria == HeadlinesAdapter.SORT_BY_SOURCE) {
                    String firstTitle = first.getSource();
                    String secondTitle = second.getSource();
                    return firstTitle.compareTo(secondTitle);
                }
                else if (sortCriteria == HeadlinesAdapter.SORT_BY_TITLE) {
                    String firstTitle = first.getTitle();
                    String secondTitle = second.getTitle();
                    return firstTitle.compareTo(secondTitle);
                }
                else if (sortCriteria == HeadlinesAdapter.SORT_BY_DATE) {
                    Calendar firstCalendar = first.getCalendar();
                    Calendar secondCalendar = second.getCalendar();
                    return -1 * firstCalendar.compareTo(secondCalendar);
                }
                return 0;
            }
        });
        return list;
    }

    public void toggleProgressBar() {
        if (syncProgressBar.getVisibility() == View.GONE && syncing) {
            syncProgressBarContainer.setVisibility(View.VISIBLE);
            syncProgressBar.setVisibility(View.VISIBLE);
            getListView().setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottomActionBar).setVisibility(View.GONE);
        }
        else if (syncProgressBar.getVisibility() == View.VISIBLE
            && !syncing) {
            syncProgressBarContainer.setVisibility(View.GONE);
            syncProgressBar.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_bottomActionBarCheckBox", true)) {
                getActivity().findViewById(R.id.bottomActionBar).setVisibility(View.VISIBLE);
            }
        }
    }

    public void toggleBottomActionBar() {
        View bottomActionBar = getActivity().findViewById(R.id.bottomActionBar);
        if (bottomActionBar.getVisibility() == View.GONE && PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_bottomActionBarCheckBox", true)) {
            attachBottomActionBarListeners(getView());
            bottomActionBar.setVisibility(View.VISIBLE);
        }
        else if (bottomActionBar.getVisibility() == View.VISIBLE && !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_bottomActionBarCheckBox", true)) {
            bottomActionBar.setVisibility(View.GONE);
        }
    }

    public void attachBottomActionBarListeners(View view) {
        action_goToPreviousUnread = (LinearLayout) view.findViewById(R.id.action_previous_unread);
        action_goToNextUnread = (LinearLayout) view.findViewById(R.id.action_next_unread);
        action_goToPreviousUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                goToPreviousUnread();
            }
        });
        action_goToNextUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                goToNextUnread();
            }
        });
        action_goToPreviousUnread.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick (View view) {
                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.goToPreviousUnread), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.LEFT, 0, getResources().getDimensionPixelOffset(R.dimen.bottom_action_bar_height));
                toast.show();
                return true;
            }
        });
        action_goToNextUnread.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick (View view) {
                Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.goToNextUnread), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 0, getResources().getDimensionPixelOffset(R.dimen.bottom_action_bar_height));
                toast.show();
                return true;
            }
        });
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

    private ArrayList<RSSDataBundle> clone (ArrayList<RSSDataBundle> a) {
        ArrayList<RSSDataBundle> newList = new ArrayList<RSSDataBundle>();
        for (RSSDataBundle rdBundle : a) {
            newList.add(rdBundle);
        }
        return newList;
    }

    /* Methods triggered by menu items */

    public void clearAllData() {
        new RSSDataBundleOpenHelper(getActivity()).clearAllData();
        data = new ArrayList<RSSDataBundle>();
        updateFeedView();
    }

    public void markAllRead() {
        for (RSSDataBundle rdBundle : data) {
            RSSDataBundle.markAsRead(getActivity(), rdBundle);
        }
        adapter.notifyDataSetChanged();
    }

    public void goToNextUnread() {
        if (data.size() == 0) {
            return;
        }
        int currentIndex = getListView().getFirstVisiblePosition();
        View v = getListView().getChildAt(0);
        int offset = (v == null) ? 0 : v.getTop();
        int itemHeight = (v == null) ? 0 : v.getMeasuredHeight();
        // Case when ListView.getFirstVisiblePosition() returns an incorrect position
        // (occurs when the return value is one position above the actual first visible position)
        if (-1 * itemHeight == offset) {
            currentIndex++;
        }
        boolean foundNext = false;
        // Scroll ListView to next unread article
        for (int i = currentIndex + 1;i < data.size();i++) {
            if (!data.get(i).isRead()) {
                //getListView().smoothScrollToPosition(i, 0);
                getListView().setSelection(i);
                foundNext = true;
                break;
            }
        }
        if (!foundNext) {
            showToast(getResources().getString(R.string.noUnreadArticlesFound), Toast.LENGTH_SHORT);
        }
    }

    public void goToPreviousUnread() {
        if (data.size() == 0) {
            return;
        }
        int currentIndex = getListView().getFirstVisiblePosition();
        View v = getListView().getChildAt(0);
        int offset = (v == null) ? 0 : v.getTop();
        int itemHeight = (v == null) ? 0 : v.getMeasuredHeight();
        // Case when ListView.getFirstVisiblePosition() returns an incorrect position
        // (occurs when the return value is one position above the actual first visible position)
        if (-1 * itemHeight == offset) {
            currentIndex++;
        }
        boolean foundPrevious = false;
        // Scroll ListView to next unread article
        for (int i = currentIndex - 1;i >= 0;i--) {
            if (!data.get(i).isRead()) {
                //getListView().smoothScrollToPosition(i, 0);
                getListView().setSelection(i);
                foundPrevious = true;
                break;
            }
        }
        if (!foundPrevious) {
            showToast(getResources().getString(R.string.noUnreadArticlesFound), Toast.LENGTH_SHORT);
        }
    }
}
