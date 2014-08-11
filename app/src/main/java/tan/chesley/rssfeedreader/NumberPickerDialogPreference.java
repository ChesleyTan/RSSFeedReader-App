package tan.chesley.rssfeedreader;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerDialogPreference extends DialogPreference{

    public NumberPickerDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.number_picker_dialog);
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
