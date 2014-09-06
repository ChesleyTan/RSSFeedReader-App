package tan.chesley.rssfeedreader;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class RssSyncService extends Service {

    private static RssSyncService singleton;
    private final Service parent = this;
    private static boolean cancelled;

    @Override
    public void onCreate () {
        super.onCreate();
        singleton = this;
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run () {
                // Run this on the main thread
                handler.post(new Runnable() {
                                 @Override
                                 public void run () {
                                     Toast.makeText(parent, getResources().getString(R.string.startingSyncService), Toast.LENGTH_SHORT).show();
                                 }
                             }
                );
                while (!cancelled && PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("pref_autosync", false)) {
                    int interval = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.KEY_PREF_AUTOSYNC_INTERVAL, 0);
                    if (interval < 60000) {
                        // Prevent intervals below 1 minute
                        interval = 60000;
                    }

                    // TODO testing
                    interval = 10000;

                    // Run this on the main thread
                    handler.post(new Runnable() {
                                     @Override
                                     public void run () {
                                         Toast.makeText(parent, getResources().getString(R.string.syncing), Toast.LENGTH_SHORT).show();
                                     }
                                 }
                    );
                    TaskFragment taskFragment = new TaskFragment();
                    String[] FEEDS = new SourcesOpenHelper(getApplicationContext()).getEnabledSources();
                    int SYNC_TIMEOUT = 1000 * PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.KEY_PREF_SYNC_TIMEOUT, getResources().getInteger(R.integer.sync_timeout_default));
                    taskFragment.setFEEDS(parent, FEEDS);
                    taskFragment.setSYNC_TIMEOUT(parent, SYNC_TIMEOUT);
                    taskFragment.setContext(parent, getApplicationContext());
                    TaskFragment.GetRssFeedTask getRssFeedTask = taskFragment.new GetRssFeedTask() {
                        @Override
                        protected void onPostExecute (Void v) {
                            final HeadlinesFragment headlinesFragment = HeadlinesFragment.getInstance();
                            if (headlinesFragment != null && headlinesFragment.getActivity() != null) {
                                // Run this on the main thread
                                handler.post(new Runnable() {
                                    @Override
                                    public void run () {
                                        headlinesFragment.setRssData(getFetchedData(), true);
                                        headlinesFragment.updateFeedView();
                                    }
                                });
                                super.onPostExecute(v);
                            }
                        }
                    };
                    getRssFeedTask.execute(FEEDS);

                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.e("Launching", "Service");
        cancelled = false;
        //return START_STICKY;
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    public void cancel() {
        cancelled = true;
        singleton = null;
    }

    public static RssSyncService getInstance() {
        return singleton;
    }
}
