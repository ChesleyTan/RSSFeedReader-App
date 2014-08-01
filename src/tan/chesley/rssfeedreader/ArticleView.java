package tan.chesley.rssfeedreader;

import java.util.ArrayList;

import junit.framework.Assert;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils.TruncateAt;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.widget.TextView;

public class ArticleView extends FragmentActivity {
	public static final String ARTICLE_SELECTED_KEY = "tan.chesley.rssfeedreader.articleselected";
	public static final String INITIALLY_CREATED_KEY = "tan.chesley.rssfeedreader.initiallycreated";
	private ViewPager theViewPager;
	private ArrayList<MyMap> rssData;
	private ArticleViewPagerChangeListener viewPagerPageChangeListener;
	private TextView title;
	private boolean initiallyCreated = false;

	public class ArticleViewPagerChangeListener implements
			ViewPager.OnPageChangeListener {

		@Override
		public void onPageSelected(int arg0) {
			String headline = rssData.get(arg0).keySet().iterator().next();
			setTitle(headline);
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
		theViewPager = new ViewPager(this);
		theViewPager.setId(R.id.viewPager);
		setContentView(theViewPager);
		theViewPager
				.setOnPageChangeListener(viewPagerPageChangeListener = new ArticleViewPagerChangeListener());
		if (savedInstanceState != null) {
			initiallyCreated = savedInstanceState.getBoolean(INITIALLY_CREATED_KEY);
		}
		if (!initiallyCreated) {
			Log.e("debug", "ArticleView activity not yet created.");
			Log.e("debug", rssData == null ? "rssData is null" : "rssData is set");
			Log.e("debug", HeadlinesFragment.getInstance() == null ? "headlinesFragment is null" : "headlinesFragment is set");
			initiallyCreated = true;
			// TODO debug why headlinesFragment is occasionally null when returning to activity after it is stopped
			Assert.assertNotNull(HeadlinesFragment.getInstance());
			if (HeadlinesFragment.getInstance() == null) {
				return;
			}
			rssData = HeadlinesFragment.getInstance().getRssData();
			FragmentManager fragMan = getSupportFragmentManager();
			theViewPager.setAdapter(new FragmentStatePagerAdapter(fragMan) {

				@Override
				public Fragment getItem(int arg0) {
					MyMap data = rssData.get(arg0);
					RSSDataBundle rdBundle = data.values().iterator().next();
					return ArticleViewFragment.newArticleViewFragment(
							rdBundle.getTitle(), rdBundle);
				}

				@Override
				public int getCount() {
					return rssData.size();
				}

			});

			String uuid = getIntent().getStringExtra(
					HeadlinesFragment.ARTICLE_ID);

			for (int i = 0; i < rssData.size(); i++) {
				MyMap map = rssData.get(i);
				if (map.values().iterator().next().getId().equals(uuid)) {
					theViewPager.setCurrentItem(i);
					// Explicitly call the page change listener to set
					// the action bar title appropriately
					viewPagerPageChangeListener.onPageSelected(i);
					break;
				}
			}

			int titleId = 0;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				titleId = getResources().getIdentifier("action_bar_title",
						"id", "android");
			} else {
				titleId = R.id.action_bar_title;
			}
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
			}
		}
	}
	
	

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(INITIALLY_CREATED_KEY, initiallyCreated);
	}



	@Override
	public void finish() {
		Intent intent = new Intent();
		intent.putExtra(ARTICLE_SELECTED_KEY, theViewPager.getCurrentItem());
		setResult(Activity.RESULT_OK, intent);
		super.finish();
	}

}
