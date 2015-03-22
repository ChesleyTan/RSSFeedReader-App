package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.widget.TextView;

public class Utils {
    /* Usage note: User should always check if the result is not null */
    public static TextView getTitleTextView(Activity context) {
        int titleId = context.getResources().getIdentifier("action_bar_title", "id", "android");
        return (TextView) context.findViewById(titleId);
    }

    public static void setActivityTitle(Activity context, String title) {
        TextView titleView = getTitleTextView(context);
        if (title != null) {
            context.setTitle(title);
            titleView.setTextColor(context.getResources().getColor((R.color.AppPrimaryTextColor)));
        }
    }
}
