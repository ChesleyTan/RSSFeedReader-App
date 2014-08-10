package tan.chesley.rssfeedreader;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

public class SettingsActivity extends Activity {

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