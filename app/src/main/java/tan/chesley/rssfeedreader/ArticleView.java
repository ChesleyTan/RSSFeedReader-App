package tan.chesley.rssfeedreader;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils.TruncateAt;
import android.text.method.SingleLineTransformationMethod;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ArticleView extends FragmentActivity {

	public static final String ARTICLE_SELECTED_KEY = "tan.chesley.rssfeedreader.articleselected";
	public static final String RSS_DATA_KEY = "tan.chesley.rssfeedreader.rssdata";
	private ViewPager theViewPager;
	private ArrayList<RSSDataBundle> rssData;
	private ArticleViewPagerChangeListener viewPagerPageChangeListener;
	private FragmentStatePagerAdapter viewPagerAdapter;
	private TextView title;

	public class ArticleViewPagerChangeListener implements
			ViewPager.OnPageChangeListener {

		@Override
		public void onPageSelected(int arg0) {
            RSSDataBundle rdBundle = rssData.get(arg0);
			setTitle(rdBundle.getTitle());
            // Mark article as read
            RSSDataBundle.markAsRead(getApplicationContext(), rdBundle);
			if (title != null) {
				title.scrollTo(0, 0);
			}
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			rssData = HeadlinesFragment.getInstance().getRssData();
		}
		else {
			rssData = savedInstanceState.getParcelableArrayList(RSS_DATA_KEY);
		}
		theViewPager = new ViewPager(this);
		theViewPager.setId(R.id.viewPager);
		setContentView(theViewPager);

		theViewPager
				.setOnPageChangeListener(viewPagerPageChangeListener = new ArticleViewPagerChangeListener());
		FragmentManager fragMan = getSupportFragmentManager();
		theViewPager
				.setAdapter(viewPagerAdapter = new FragmentStatePagerAdapter(
						fragMan) {

					@Override
					public Fragment getItem(int arg0) {
						return ArticleViewFragment.newArticleViewFragment(rssData.get(arg0));
					}

					@Override
					public int getCount() {
						return rssData.size();
					}

				});
		String uuid = getIntent().getStringExtra(HeadlinesFragment.ARTICLE_ID);

		for (int i = 0; i < rssData.size(); i++) {
			RSSDataBundle rdBundle = rssData.get(i);
			if (rdBundle.getId().equals(uuid)) {
				theViewPager.setCurrentItem(i);
				// Explicitly call the page change listener to set
				// the action bar title appropriately
				viewPagerPageChangeListener.onPageSelected(i);
				break;
			}
		}

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
		int	titleId = getResources().getIdentifier("action_bar_title", "id",
					"android");
		title = (TextView) findViewById(titleId);

		if (title != null) {
			title.setEllipsize(TruncateAt.MARQUEE);
			title.setMarqueeRepeatLimit(-1);
			title.setHorizontallyScrolling(true);
			title.setFocusable(true);
			title.setFocusableInTouchMode(true);
			title.requestFocus();
			title.setTransformationMethod(SingleLineTransformationMethod
					.getInstance());
			title.setTextColor(getResources().getColor((R.color.AppPrimaryTextColor)));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(RSS_DATA_KEY, rssData);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				//Log.e("ArticleView", "Up button selected.");
				// Finish activity to return the current item to HeadlinesFragment
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void finish() {
		Intent intent = new Intent();
		intent.putExtra(ARTICLE_SELECTED_KEY, theViewPager.getCurrentItem());
		setResult(Activity.RESULT_OK, intent);
		//Log.e("ArticleView", "Activity Finished.");
		super.finish();
	}

}
