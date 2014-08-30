package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick (DialogInterface dialogInterface, int i) {
                    if (i == DialogInterface.BUTTON_POSITIVE) {
                        if (HeadlinesFragment.getInstance() != null) {
                            HeadlinesFragment.getInstance().clearAllData();
                        }
                    }
                }
            };
            builder.setMessage(getResources().getString(R.string.confirm_clearAllArticles))
                   .setPositiveButton(getResources().getString(R.string.yes), onClickListener)
                   .setNegativeButton(getResources().getString(R.string.cancel), onClickListener)
                   .show();
            return true;
        }
        else if (id == R.id.action_mark_all_read) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick (DialogInterface dialogInterface, int i) {
                    if (i == DialogInterface.BUTTON_POSITIVE) {
                        if (HeadlinesFragment.getInstance() != null) {
                            HeadlinesFragment.getInstance().markAllRead();
                        }
                    }
                }
            };
            builder.setMessage(getResources().getString(R.string.confirm_markAllRead))
                   .setPositiveButton(getResources().getString(R.string.yes), onClickListener)
                   .setNegativeButton(getResources().getString(R.string.cancel), onClickListener)
                   .show();
            return true;
        }
        else if (id == R.id.action_go_to_top) {
            if (HeadlinesFragment.getInstance() != null) {
                HeadlinesFragment.getInstance().goToTop();
            }
            return true;
        }
		return super.onOptionsItemSelected(item);
	}

}
