package tan.chesley.rssfeedreader;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class TimePickerDialogPreference extends DialogPreference{
    public TimePickerDialogPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setDialogLayoutResource(R.layout.time_picker_dialog);
    }

    @Override
    protected void onBindDialogView (View view) {
        super.onBindDialogView(view);
    }

    @Override
    public void onClick (DialogInterface dialog, int which) {
        super.onClick(dialog, which);
    }
}
