package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        //toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.setDuration(toastDurationFlag);
        toast.setView(v);
        toast.show();
    }

    public static void showAlternateToast(Activity activity, String title, String text, Drawable image, int toastDurationFlag) {
        Toast toast = new Toast(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View v = inflater.inflate(R.layout.toast_alternate_layout, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
        TextView titleTextView = (TextView) v.findViewById(R.id.title);
        TextView textView = (TextView) v.findViewById(R.id.text);
        ImageView imageView = (ImageView) v.findViewById(R.id.toast_image);
        imageView.setImageDrawable(image);
        titleTextView.setText(Html.fromHtml(title));
        textView.setText(Html.fromHtml(text));
        toast = new Toast(activity.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(toastDurationFlag);
        toast.setView(v);
        toast.show();
    }

}
