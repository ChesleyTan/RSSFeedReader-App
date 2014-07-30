package tan.chesley.rssfeedreader;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class ArticleView extends FragmentActivity {
	private ViewPager theViewPager;
	private ArrayList<MyMap> rssData;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		theViewPager = new ViewPager(this);
		theViewPager.setId(R.id.viewPager);
		setContentView(theViewPager);
		theViewPager
		.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int arg0) {
				setTitle(rssData.get(arg0).keySet().iterator().next());

			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub

			}
		});
		rssData = HeadlinesFragment.getInstance().getRssData();
		FragmentManager fragMan = getSupportFragmentManager();
		theViewPager.setAdapter(new FragmentStatePagerAdapter(fragMan) {

			@Override
			public Fragment getItem(int arg0) {
				MyMap data = rssData.get(arg0);
				String myHeadline = data.keySet().iterator().next();
				return ArticleViewFragment.newArticleViewFragment(
						myHeadline, data.get(myHeadline));
			}

			@Override
			public int getCount() {
				return rssData.size();
			}

		});

		String headline = getIntent().getStringExtra(
				HeadlinesFragment.ARTICLE_HEADLINE);
		
		for (int i = 0; i < rssData.size(); i++) {
			if (rssData.get(i).keySet().iterator().next().equals(headline)) {
				theViewPager.setCurrentItem(i);
				break;
			}
		}
		Log.e("ArticleView", "ArticleView3: " + headline);
	}

}
