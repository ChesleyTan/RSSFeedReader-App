package tan.chesley.rssfeedreader;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RssSyncService extends Service{

    private final Service parent = this;

    @Override
    public void onCreate () {
        super.onCreate();
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run () {
                handler.post(new Runnable() {
                    @Override
                    public void run () {
                        Toast.makeText(parent, "Starting sync service", Toast.LENGTH_SHORT).show();
                        TaskFragment taskFragment = new TaskFragment();
                        String[] FEEDS = new SourcesOpenHelper(getApplicationContext()).getEnabledSources();
                        int SYNC_TIMEOUT = 1000 * PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(SettingsActivity.KEY_PREF_SYNC_TIMEOUT, getResources().getInteger(R.integer.sync_timeout_default));
                        taskFragment.setFEEDS(parent, FEEDS);
                        taskFragment.setSYNC_TIMEOUT(parent, SYNC_TIMEOUT);
                        taskFragment.setContext(parent, getApplicationContext());
                        TaskFragment.GetRssFeedTask getRssFeedTask = taskFragment.new GetRssFeedTask() {
                            @Override
                            protected void onPostExecute (Void v) {
                                if (HeadlinesFragment.getInstance() != null) {
                                    HeadlinesFragment.getInstance().setRssData(getFetchedData(), true);
                                    HeadlinesFragment.getInstance().updateFeedView();
                                    super.onPostExecute(v);
                                }
                            }
                        };
                        getRssFeedTask.execute(FEEDS);
                        Toast.makeText(parent, "Finished starting sync service", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.e("Launching", "Service");
        //return START_STICKY;
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }
}
