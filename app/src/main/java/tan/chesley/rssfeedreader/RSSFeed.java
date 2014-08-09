package tan.chesley.rssfeedreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class RSSFeed extends FragmentActivity {

	public RSSFeed() {
		// Log.e("Instance", "Instance: RSS Feed activity created.");
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_rssfeed);
		FragmentManager fragMan = getSupportFragmentManager();
		Fragment theFragment = fragMan.findFragmentById(R.id.container);
		if (theFragment == null) {
			// Log.e("Instance",
			// "Instance: New HeadlinesFragment created by RSSFeed.");
			fragMan.beginTransaction()
					.add(R.id.container, new HeadlinesFragment()).commit();
		}

	}

	public HeadlinesFragment getHeadlinesFragment() {
		return (HeadlinesFragment) getSupportFragmentManager()
				.findFragmentById(R.id.container);
	}

	public void syncFeeds(View v) {
		if (HeadlinesFragment.getInstance() != null) {
			HeadlinesFragment.getInstance().syncFeeds();
		}
		else {
			// Log.e("Instance",
			// "Instance: headlinesFragment not found, cannot sync.");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rssfeed, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		else if (id == R.id.action_refresh) {
			if (HeadlinesFragment.getInstance() != null) {
				HeadlinesFragment.getInstance().updateFeedView();
			}
			else {
				// Log.e("Instance",
				// "Instance: headlinesFragment not found, cannot sync.");
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
