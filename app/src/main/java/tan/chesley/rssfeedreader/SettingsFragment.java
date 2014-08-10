package tan.chesley.rssfeedreader;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;


public class SettingsFragment extends PreferenceFragment {
	
	private ListPreference sortFeedByListPreference;
    private ListPreference feedDateFormatPreference;
    private NumberPickerDialogPreference maxArticleNumberPickerDialogPreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		sortFeedByListPreference = (ListPreference) findPreference("pref_feedSortBy_type");
		sortFeedByListPreference.setSummary(sortFeedByListPreference.getEntry());
		sortFeedByListPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				ListPreference pref = (ListPreference) preference;
				pref.setValue(newValue.toString());
				pref.setSummary(pref.getEntry());
				return false;
			}
			
		});
        feedDateFormatPreference = (ListPreference) findPreference("pref_feedDateFormat_type");
        feedDateFormatPreference.setSummary(feedDateFormatPreference.getEntry());
        feedDateFormatPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange (Preference preference, Object newValue) {
                ListPreference pref = (ListPreference) preference;
                pref.setValue(newValue.toString());
                pref.setSummary(pref.getEntry());
                return false;
            }
        });
        maxArticleNumberPickerDialogPreference = (NumberPickerDialogPreference) findPreference("pref_id_maxArticleNumberDialogPicker");
        maxArticleNumberPickerDialogPreference.setSummary(Integer.toString(PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(SettingsActivity.KEY_PREF_MAX_ARTICLE_NUMBER, getResources().getInteger(R.integer.max_article_number_default))));
	}
	
}