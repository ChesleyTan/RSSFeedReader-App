package tan.chesley.rssfeedreader;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

public class Toaster {
    private static Toast toast;
    public static void showToast (Context context, String s, int toastDurationFlag) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, s,
                               toastDurationFlag);
        TextView toastTextView = (TextView) toast.getView().findViewById(
            android.R.id.message);
        toastTextView.setTextColor(context.getResources().getColor(
            R.color.AppPrimaryTextColor));
        toast.getView().setBackgroundColor(context.getResources().getColor(R.color.AppDefaultBackgroundColor));
        toast.getView().getBackground().setAlpha(180);
        toast.show();
    }

}
