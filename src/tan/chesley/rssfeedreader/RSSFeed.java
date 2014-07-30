package tan.chesley.rssfeedreader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class RSSFeed extends FragmentActivity {
	
	public RSSFeed() {
		//Log.e("Instance", "Instance: RSS Feed activity created.");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rssfeed);
		FragmentManager fragMan = getSupportFragmentManager();
		Fragment theFragment = fragMan.findFragmentById(R.id.container);
		if (theFragment == null) {
			//Log.e("Instance", "Instance: New HeadlinesFragment created by RSSFeed.");
			fragMan.beginTransaction()
			.add(R.id.container, new HeadlinesFragment()).commit();
		}

	}

	public void syncFeeds(View v) {
		if (HeadlinesFragment.getInstance() != null) {
			HeadlinesFragment.getInstance().syncFeeds();
		}
		else {
			//Log.e("Instance", "Instance: headlinesFragment not found, cannot sync.");
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
