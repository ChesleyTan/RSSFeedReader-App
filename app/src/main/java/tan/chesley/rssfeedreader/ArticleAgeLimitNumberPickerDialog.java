package tan.chesley.rssfeedreader;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class ArticleAgeLimitNumberPickerDialog extends NumberPickerDialogPreference {
    private NumberPicker dialogNumberPicker;
    public ArticleAgeLimitNumberPickerDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindDialogView (View view) {
        super.onBindDialogView(view);
        dialogNumberPicker = (NumberPicker) view.findViewById(R.id.dialogNumberPicker);
        dialogNumberPicker.setMinValue(getContext().getResources().getInteger(R.integer.article_age_limit_minimum));
        dialogNumberPicker.setMaxValue(getContext().getResources().getInteger(R.integer.article_age_limit_maximum));
        dialogNumberPicker.setValue(PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(SettingsActivity.KEY_PREF_ARTICLE_AGE_LIMIT,
                                                                                                       getContext().getResources().getInteger(R.integer.article_age_limit_default)));
    }

    @Override
    public void onClick (DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(SettingsActivity.KEY_PREF_ARTICLE_AGE_LIMIT, dialogNumberPicker.getValue()).commit();
            setSummary(Integer.toString(dialogNumberPicker.getValue()));
        }
    }
}
