package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    public static final String KEY_PREF_MAX_ARTICLE_NUMBER = "tan.chesley.rssfeedreader.keyprefmaxarticlenumber";
    public static final String KEY_PREF_ARTICLE_AGE_LIMIT = "tan.chesley.rssfeedreader.keyprefarticleagelimit";
    public static final String KEY_PREF_SYNC_TIMEOUT = "tan.chesley.rssfeedreader.synctimeout";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

		int	titleId = getResources().getIdentifier("action_bar_title", "id",
					"android");
		TextView title = (TextView) findViewById(titleId);
		if (title != null) {
			setTitle(R.string.settingsTitle);
			title.setTextColor(getResources().getColor((R.color.AppPrimaryTextColor)));
		}
	}

	
}