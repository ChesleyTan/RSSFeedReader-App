package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

public class AutosyncTimePickerDialog extends TimePickerDialogPreference {
    private TimePicker timePicker;
    private Activity mActivity;

    public AutosyncTimePickerDialog (Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mActivity = (Activity) context;
    }

    @Override
    protected void onBindDialogView (View view) {
        super.onBindDialogView(view);
        timePicker = (TimePicker) view.findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int millis = prefs.getInt(SettingsActivity.KEY_PREF_AUTOSYNC_INTERVAL, 0);
        int hour = millis / 3600000;
        int minute = millis % 3600000 / 60000;
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(minute);
    }

    @Override
    public void onClick (DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            int interval = timePicker.getCurrentHour() * 3600000 + timePicker.getCurrentMinute() * 60000;
            if (interval < 60000) {
                Toaster.showToast(mActivity, getContext().getResources().getString(R.string.invalidIntervalUnderOneMinute), Toast.LENGTH_LONG);
                interval = 60000;
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.edit().putInt(SettingsActivity.KEY_PREF_AUTOSYNC_INTERVAL, interval).apply();
        }
    }
}
