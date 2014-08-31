package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Toaster {
    private static Toast toast;
    public static void showToast (Activity activity, String s, int toastDurationFlag) {
        if (toast != null) {
            toast.cancel();
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        View v = inflater.inflate(R.layout.toast_layout, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
        TextView textView = (TextView) v.findViewById(R.id.text);
        textView.setText(s);
        toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.setDuration(toastDurationFlag);
        toast.setView(v);
        toast.show();
    }

}
