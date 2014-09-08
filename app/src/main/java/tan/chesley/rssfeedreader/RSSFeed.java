package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class RSSFeed extends Activity {

	public RSSFeed() {
		// Log.e("Instance", "Instance: RSS Feed activity created.");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        //Log.e("RSSFeed", "Calling onCreate");
        BrightnessControl.toggleBrightness(getApplicationContext(), this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_rssfeed);
		FragmentManager fragMan = getFragmentManager();
		Fragment theFragment = fragMan.findFragmentById(R.id.container);
        // If this is the first run and the content fragment has not been created yet
		if (theFragment == null) {
            // If this is the first run and the autosync service is not running, clear any obsolete notifications
            if (RssSyncService.getInstance() == null) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(RssSyncService.SYNC_STATUS_NOTIFICATION_ID);
            }
			//Log.e("RSSFeed", "Instance: New HeadlinesFragment created by RSSFeed.");
			fragMan.beginTransaction()
					.add(R.id.container, new HeadlinesFragment()).commit();
		}
	}

	public HeadlinesFragment getHeadlinesFragment() {
		return (HeadlinesFragment) getFragmentManager()
				.findFragmentById(R.id.container);
	}

    // Used to create dynamic menu
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rssfeed, menu);
        if (!(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("pref_autosync", false))) {
            // Start autosync service button is disabled, but visible
            menu.findItem(R.id.action_start_autosync_service).setEnabled(false);
            menu.findItem(R.id.action_stop_autosync_service).setVisible(false);
        }
        else if (RssSyncService.getInstance() == null) {
            menu.findItem(R.id.action_start_autosync_service).setVisible(true);
            menu.findItem(R.id.action_stop_autosync_service).setVisible(false);
        }
        // Handles case when autosync is disabled, but the service is still running
        if (RssSyncService.getInstance() != null) {
            menu.findItem(R.id.action_start_autosync_service).setVisible(false);
            menu.findItem(R.id.action_stop_autosync_service).setVisible(true);
        }
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
        else if (id == R.id.action_lights_off_mode) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean lightsOffMode = prefs.getBoolean(BrightnessControl.LIGHTS_OFF_MODE, false);
            prefs.edit().putBoolean(BrightnessControl.LIGHTS_OFF_MODE, !lightsOffMode).apply();
            if (lightsOffMode) {
                Toaster.showAlternateToast(this, getResources().getString(R.string.lightsOffMode_disabled), "", getResources().getDrawable(R.drawable.ic_action_brightness_high), Toast.LENGTH_SHORT);
            }
            else {
                Toaster.showAlternateToast(this, getResources().getString(R.string.lightsOffMode_enabled), "", getResources().getDrawable(R.drawable.ic_action_brightness_low), Toast.LENGTH_SHORT);
            }
            BrightnessControl.toggleBrightness(getApplicationContext(), this);
        }
        else if (id == R.id.action_stop_autosync_service) {
            if (RssSyncService.getInstance() != null) {
                // RssSyncService.cancel() breaks the while loop that does the syncing
                RssSyncService.getInstance().cancel();
            }
            Intent intent = new Intent(this, RssSyncService.class);
            stopService(intent);
            Toast.makeText(this, getResources().getString(R.string.serviceStopped), Toast.LENGTH_SHORT).show();
            Log.e("Stopped service: ", "Autosync");
        }
        else if (id == R.id.action_start_autosync_service) {
            Intent intent = new Intent(this, RssSyncService.class);
            startService(intent);
        }
		return super.onOptionsItemSelected(item);
	}

}
