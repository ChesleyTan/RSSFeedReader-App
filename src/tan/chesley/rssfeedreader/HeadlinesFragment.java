package tan.chesley.rssfeedreader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
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

public class HeadlinesFragment extends ListFragment implements
		TaskFragment.TaskCallbacks {

	public static final String PARSED_FEED_DATA = "tan.chesley.rssfeedreader.parsedfeeddata";
	public static final String ARTICLE_ID = "tan.chesley.rssfeedreader.articleid";
	public static final String TASK_FRAGMENT = "tan.chesley.rssfeedreader.taskfragment";
	public static final String SYNCING = "tan.chesley.rssfeedreader.syncing";
	public static final int RSS_ARTICLE_COUNT_MAX = 20;
	public static final int ARTICLE_VIEW_INTENT = 0;
	public static ArrayList<MyMap> data = new ArrayList<MyMap>();
	private static HeadlinesFragment singleton;
	private HeadlinesAdapter adapter;
	private TaskFragment mTaskFragment;
	private boolean syncing = false;
	private Button updateButton;
	private ProgressBar syncProgressBar;
	private LinearLayout syncProgressBarContainer;

	public HeadlinesFragment() {
		singleton = this;
	}

	public static HeadlinesFragment getInstance() {
		return singleton;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		singleton = this;
		if (savedInstanceState != null) {
			syncing = savedInstanceState.getBoolean(SYNCING);
		}
		// Log.e("HeadlinesFragment", "Instance: Calling onCreate method.");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateButton = (Button) view.findViewById(R.id.updateButton);
		syncProgressBar = (ProgressBar) view.findViewById(R.id.syncProgressBar);
		syncProgressBarContainer = (LinearLayout) view.findViewById(R.id.progressBarContainer);
		Assert.assertNotNull(updateButton);
		Assert.assertNotNull(syncProgressBar);
		Assert.assertNotNull(syncProgressBarContainer);
		if (syncing) {
			toggleProgressBar();
		}
		if (savedInstanceState != null) {
			ArrayList<MyMap> tmp = savedInstanceState
					.getParcelableArrayList(PARSED_FEED_DATA);
			if (tmp == null) {
				// Log.e("Instance", "Instance: no saved data found.");
			}
			else {
				data = tmp;
				updateListAdapter();
				// Log.e("Instance", "Restored Instance State.");
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Log.e("HeadlinesFragment", "Saving instance state.");
		outState.putParcelableArrayList(PARSED_FEED_DATA, data);
		outState.putBoolean(SYNCING, syncing);
	}

	public void syncFeeds() {
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
			getActivity().getSupportFragmentManager().beginTransaction()
					.add(mTaskFragment, TASK_FRAGMENT).commit();
			showToast("Syncing feeds...", Toast.LENGTH_LONG);
		}
	}

	private void updateFeedView() {
		// Sort headlines by a given criteria before updating the screen
		sortHeadlinesBy(HeadlinesAdapter.SORT_BY_DATE, data);
		updateListAdapter();
	}

	private void updateListAdapter() {
		if (getListAdapter() == null) {
			adapter = new HeadlinesAdapter(data);
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

		public HeadlinesAdapter(ArrayList<MyMap> myData) {
			super(getActivity(), R.layout.feed_list_item, myData);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						R.layout.feed_list_item, null);
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
			dateTextView.setText(rdBundle.getFormattedDate());
			articleTextView.setText(rdBundle.getDescription());
			return convertView;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Log.e("Instance", "Instance: Inflating new fragment.");
		View rootView = inflater.inflate(R.layout.fragment_rssfeed, container,
				false);
		return rootView;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HashMap<String, RSSDataBundle> map = adapter.getItem(position);
		// Start new ArticleView activity, passing to it the id of the clicked
		// article
		Intent intent = new Intent(getActivity(), ArticleView.class);
		intent.putExtra(ARTICLE_ID, map.values().iterator().next().getId());
		startActivityForResult(intent, ARTICLE_VIEW_INTENT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Receive current article index from ArticleView activity
		if (requestCode == ARTICLE_VIEW_INTENT) {
			Assert.assertNotNull(data);
			getListView().setSelection(
					data.getIntExtra(ArticleView.ARTICLE_SELECTED_KEY, 0));
		}
	}

	public ArrayList<MyMap> getRssData() {
		return data;
	}

	public void setRssData(ArrayList<MyMap> in) {
		// Called by TaskFragment to update the RSS data fetched by RSSHandler
		data = in;
	}

	@SuppressLint("NewApi")
	public void showToast(String s, int toastDurationFlag) {
		Toast toast = Toast.makeText(getActivity().getApplicationContext(), s,
				toastDurationFlag);
		TextView toastTextView = (TextView) toast.getView().findViewById(android.R.id.message);
		toastTextView.setTextColor(getResources().getColor(R.color.AppPrimaryTextColor));
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
			toast.getView().getBackground().setAlpha(180);
		}
		toast.show();
	}

	public static ArrayList<MyMap> sortHeadlinesBy(final int sortCriteria,
			ArrayList<MyMap> list) {
		Collections.sort(list, new Comparator<MyMap>() {

			public int compare(MyMap first, MyMap second) {
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
				else if (sortCriteria == HeadlinesAdapter.SORT_BY_NONE) {}
				return 0;
			}
		});
		return list;
	}

	public void toggleProgressBar() {
		if (syncProgressBar.getVisibility() == View.GONE && syncing == true) {
			syncProgressBar.setIndeterminate(true);
			syncProgressBar.setVisibility(View.VISIBLE);
			syncProgressBarContainer.setVisibility(View.VISIBLE);
			updateButton.setVisibility(View.GONE);
		}
		else if (syncProgressBar.getVisibility() == View.VISIBLE
				&& syncing == false) {
			syncProgressBar.setVisibility(View.GONE);
			syncProgressBarContainer.setVisibility(View.GONE);
			updateButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPreExecute() {

	}

	@Override
	public void onProgressUpdate() {
		showToast("Still working...", Toast.LENGTH_SHORT);
	}

	@Override
	public void onCancelled() {
		Log.e("Feed", "Feed sync timeout reached. Thread stopped.");
		feedSyncTaskFinishedCallback();
	}

	@Override
	public void onPostExecute() {
		feedSyncTaskFinishedCallback();
	}

	public void feedSyncTaskFinishedCallback() {
		// Update list adapter
		updateFeedView();
		syncing = false;
		// Hide the progress bar
		toggleProgressBar();
		// Remove the TaskFragment
		FragmentManager fragMan = getActivity().getSupportFragmentManager();
		fragMan.beginTransaction()
				.remove(fragMan.findFragmentByTag(TASK_FRAGMENT))
				.addToBackStack(null).commit();
	}
}
