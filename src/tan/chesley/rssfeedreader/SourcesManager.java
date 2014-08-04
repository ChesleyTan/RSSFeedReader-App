package tan.chesley.rssfeedreader;

import java.util.HashSet;

import android.content.Context;
import android.content.SharedPreferences;


public class SourcesManager {
	
	private SharedPreferences mSharedPreferences;
	private String key_sourcesPreferenceFile;
	
	public SourcesManager(Context context) {
		key_sourcesPreferenceFile = context.getString(R.string.keySourcesPreferencesFile);
		mSharedPreferences = context.getSharedPreferences(key_sourcesPreferenceFile, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.clear();
		editor.putInt("0", 7);
		editor.putString("1", "http://rss.cnn.com/rss/cnn_world.rss");
		editor.putString("2", "http://rss.cnn.com/rss/cnn_tech.rss");
		editor.putString("3", "http://news.feedzilla.com/en_us/headlines/top-news/world-news.rss");
		editor.putString("4", "http://news.feedzilla.com/en_us/headlines/science/top-stories.rss");
		editor.putString("5", "http://news.feedzilla.com/en_us/headlines/technology/top-stories.rss");
		editor.putString("6", "http://news.feedzilla.com/en_us/headlines/programming/top-stories.rss");
		editor.putString("7", "http://www.reddit.com/.rss");
		editor.commit();
	}
	
	public void addSource(String s) {
		int size = mSharedPreferences.getInt("0", 0);
		mSharedPreferences.edit().putString(Integer.toString(size + 1), s).commit();
	}
	
	public void removeSource(String s) {
		int size = mSharedPreferences.getInt("0", 0);
		HashSet<String> sources = new HashSet<String>();
		for (int i = 1;i <= size;i++) {
			String tmp = mSharedPreferences.getString(Integer.toString(i), "");
			if (!tmp.equals(s)) {
				sources.add(tmp);
			}
		}
		size = sources.size();
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.clear();
		editor.putInt("0", size);
		for (int i = 1;i <= size;i++) {
			editor.putString(Integer.toString(i), sources.iterator().next());
		}
		editor.commit();
	}
	
	public String[] getSources() {
		int size = mSharedPreferences.getInt("0", 0);
		String[] sources = new String[size];
		for (int i = 1;i <= size;i++) {
			sources[i-1] = mSharedPreferences.getString(Integer.toString(i), ""); 
		}
		return sources;
	}
}
