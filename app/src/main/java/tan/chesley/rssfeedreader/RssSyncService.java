package tan.chesley.rssfeedreader;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class RssSyncService extends Service{

    private final Service parent = this;
    private long counter = 0;

    @Override
    public void onCreate () {
        super.onCreate();
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run () {
                while (true) {
                    handler.post(new Runnable() {
                        @Override
                        public void run () {
                            //Toast.makeText(parent, Long.toString(counter += 5), Toast.LENGTH_SHORT).show();

                            LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

                            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                                PixelFormat.TRANSLUCENT
                            );

                            params.gravity = Gravity.RIGHT | Gravity.TOP;
                            View myview = li.inflate(R.layout.toast_alternate_layout, null);

                            wm.addView(myview, params);

                        }
                    });
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    parent.stopSelf();
                }
            }
        }).start();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }
}
