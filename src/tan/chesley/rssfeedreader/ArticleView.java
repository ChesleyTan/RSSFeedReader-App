package tan.chesley.rssfeedreader;

import java.util.ArrayList;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils.TruncateAt;
import android.text.method.SingleLineTransformationMethod;
import android.widget.TextView;

public class ArticleView extends FragmentActivity {
	private ViewPager theViewPager;
	private ArrayList<MyMap> rssData;
	private ArticleViewPagerChangeListener viewPagerPageChangeListener;

	public class ArticleViewPagerChangeListener implements
			ViewPager.OnPageChangeListener {

		@Override
		public void onPageSelected(int arg0) {
			String headline = rssData.get(arg0).keySet().iterator().next();
			setTitle(headline);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
			// TODO Auto-generated method stub

		}
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		theViewPager = new ViewPager(this);
		theViewPager.setId(R.id.viewPager);
		setContentView(theViewPager);
		theViewPager
				.setOnPageChangeListener(viewPagerPageChangeListener = new ArticleViewPagerChangeListener());
		rssData = HeadlinesFragment.getInstance().getRssData();
		FragmentManager fragMan = getSupportFragmentManager();
		theViewPager.setAdapter(new FragmentStatePagerAdapter(fragMan) {

			@Override
			public Fragment getItem(int arg0) {
				MyMap data = rssData.get(arg0);
				String myHeadline = data.keySet().iterator().next();
				return ArticleViewFragment.newArticleViewFragment(myHeadline,
						data.get(myHeadline));
			}

			@Override
			public int getCount() {
				return rssData.size();
			}

		});

		String uuid = getIntent().getStringExtra(HeadlinesFragment.ARTICLE_ID);

		for (int i = 0; i < rssData.size(); i++) {
			MyMap map = rssData.get(i);
			if (map.get(map.keySet().iterator().next()).getId().equals(uuid)) {
				theViewPager.setCurrentItem(i);
				// Explicitly call the page change listener to set the action
				// bar title appropriately
				viewPagerPageChangeListener.onPageSelected(i);
				break;
			}
		}

		int titleId = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			titleId = getResources().getIdentifier("action_bar_title", "id",
					"android");
		} else {
			titleId = R.id.action_bar_title;
		}
		TextView title = (TextView) findViewById(titleId);
		if (title != null) {
			title.setEllipsize(TruncateAt.MARQUEE);
			title.setMarqueeRepeatLimit(-1);
			title.setFocusable(true);
			title.setFocusableInTouchMode(true);
			title.requestFocus();
			title.setTransformationMethod(SingleLineTransformationMethod
					.getInstance());
		}
	}


}
