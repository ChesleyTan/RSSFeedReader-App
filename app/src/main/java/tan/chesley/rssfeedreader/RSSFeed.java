package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

public class RSSFeed extends Activity {

	public RSSFeed() {
		// Log.e("Instance", "Instance: RSS Feed activity created.");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // TODO is this setContentView necessary?
		setContentView(R.layout.activity_rssfeed);
		FragmentManager fragMan = getFragmentManager();
		Fragment theFragment = fragMan.findFragmentById(R.id.container);
		if (theFragment == null) {
			// Log.e("Instance",
			// "Instance: New HeadlinesFragment created by RSSFeed.");
			fragMan.beginTransaction()
					.add(R.id.container, new HeadlinesFragment()).commit();
		}

	}

	public HeadlinesFragment getHeadlinesFragment() {
		return (HeadlinesFragment) getFragmentManager()
				.findFragmentById(R.id.container);
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
				HeadlinesFragment.getInstance().syncFeeds();
			}
			else {
				// Log.e("Instance",
				// "Instance: headlinesFragment not found, cannot sync.");
			}
			return true;
		}
        else if (id == R.id.action_clear) {
            if (HeadlinesFragment.getInstance() != null) {
                RSSDataBundleOpenHelper dbHelper = new RSSDataBundleOpenHelper(getApplicationContext());
                dbHelper.clearAllData();
                HeadlinesFragment.getInstance().setRssData(new ArrayList<RSSDataBundle>(), false);
                HeadlinesFragment.getInstance().updateFeedView();
            }
            return true;
        }
		return super.onOptionsItemSelected(item);
	}

}
