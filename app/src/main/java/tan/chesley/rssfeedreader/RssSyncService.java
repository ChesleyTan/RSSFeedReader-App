package tan.chesley.rssfeedreader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

public class RssSyncService extends Service {

    public static int SYNC_STATUS_NOTIFICATION_ID = 7962;
    private static RssSyncService singleton;
    private final Service parent = this;
    private static boolean cancelled;
    private int waited = 0;

    @Override
    public void onCreate () {
        super.onCreate();
        singleton = this;
        final Handler handler = new Handler(Looper.getMainLooper());

        // Create notification to show that this service is running
        Intent clickIntent = new Intent(getApplicationContext(), RSSFeed.class);
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification;

        // Modify task stack to allow the user to return to the home screen when pressing the back button
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addParentStack(RSSFeed.class);
        stackBuilder.addNextIntent(clickIntent);
        PendingIntent pendingClickIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_launcher)
               .setContentTitle(getResources().getString(R.string.rssSyncService))
               .setContentText(getResources().getString(R.string.serviceIsCurrentlyRunning))
               .setOngoing(true)
               .setWhen(0)
               .setPriority(Notification.PRIORITY_MIN)
               .setContentIntent(pendingClickIntent);

        notification = builder.build();
        notificationManager.notify(SYNC_STATUS_NOTIFICATION_ID, notification);

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
                    int SYNC_TIMEOUT = 1000 * PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                                               .getInt(SettingsActivity.KEY_PREF_SYNC_TIMEOUT,
                                                                       getResources().getInteger(R.integer.sync_timeout_default));
                    taskFragment.setFEEDS(parent, FEEDS);
                    taskFragment.setSYNC_TIMEOUT(parent, SYNC_TIMEOUT);
                    taskFragment.setContext(parent, getApplicationContext());
                    TaskFragment.GetRssFeedTask getRssFeedTask = taskFragment.new GetRssFeedTask() {
                        @Override
                        protected void onPostExecute (Void v) {
                            // Constrain the database size
                            new RSSDataBundleOpenHelper(getApplicationContext()).constrainDatabaseSize(getApplicationContext());
                            final HeadlinesFragment headlinesFragment = HeadlinesFragment.getInstance();
                            if (headlinesFragment != null && headlinesFragment.getActivity() != null) {
                                // Run this on the main thread
                                handler.post(new Runnable() {
                                    @Override
                                    public void run () {
                                        headlinesFragment.setRssData(getFetchedData(), true);
                                        headlinesFragment.updateFeedView();
                                        ArticleView.notifyPagerAdapterDataSetChanged();
                                    }
                                });
                                super.onPostExecute(v);
                            }
                        }
                    };
                    getRssFeedTask.execute(FEEDS);

                    try {
                        while (waited < interval) {
                            builder.setContentText(getResources().getString(R.string.minutesToNextSync) + " " + Integer.toString((interval - waited) / 60000));
                            notificationManager.notify(SYNC_STATUS_NOTIFICATION_ID, builder.build());
                            waited += 60000;
                            Thread.sleep(60000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        waited = 0;
                    }
                }
                // Stop when syncing loop ends
                cleanUp();
                stopSelf();
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
        cleanUp();
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        cleanUp();
    }

    public void cleanUp() {
        singleton = null;
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(SYNC_STATUS_NOTIFICATION_ID);
    }

    public static RssSyncService getInstance() {
        return singleton;
    }
}
