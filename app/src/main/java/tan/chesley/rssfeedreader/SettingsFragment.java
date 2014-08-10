package tan.chesley.rssfeedreader;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;


public class SettingsFragment extends PreferenceFragment {
	
	private ListPreference sortFeedByListPreference;

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
	}
	
}