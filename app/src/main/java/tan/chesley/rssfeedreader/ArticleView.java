package tan.chesley.rssfeedreader;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FixedFragmentStatePagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils.TruncateAt;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ArticleView extends FragmentActivity {

	public static final String ARTICLE_SELECTED_KEY = "tan.chesley.rssfeedreader.articleselected";
	public static final String RSS_DATA_KEY = "tan.chesley.rssfeedreader.rssdata";
	private ViewPager viewPager;
	private ArrayList<RSSDataBundle> rssData;
	private ArticleViewPagerChangeListener viewPagerPageChangeListener;
	private static FragmentStatePagerAdapter viewPagerAdapter;
	private TextView title;
    private LinearLayout action_openInBrowser;
    private LinearLayout action_previous_unread;
    private LinearLayout action_next_unread;

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
        BrightnessControl.toggleBrightness(getApplicationContext(), this);
		if (savedInstanceState == null) {
			rssData = HeadlinesFragment.getInstance().getRssData();
		}
		else {
			rssData = savedInstanceState.getParcelableArrayList(RSS_DATA_KEY);
		}
		setContentView(R.layout.article_view);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

		viewPager
				.setOnPageChangeListener(viewPagerPageChangeListener = new ArticleViewPagerChangeListener());
		FragmentManager fragMan = getSupportFragmentManager();
		viewPager
				.setAdapter(viewPagerAdapter = new FixedFragmentStatePagerAdapter(
                    fragMan) {

                    @Override
                    public Fragment getItem (int arg0) {
                        return ArticleViewFragment.newArticleViewFragment(rssData.get(arg0));
                    }

                    @Override
                    public int getCount () {
                        return rssData.size();
                    }

                });
        viewPager.setPageTransformer(true, new DepthPageTransformer());
		String uuid = getIntent().getStringExtra(HeadlinesFragment.ARTICLE_ID);

		for (int i = 0; i < rssData.size(); i++) {
			RSSDataBundle rdBundle = rssData.get(i);
			if (rdBundle.getId().equals(uuid)) {
				viewPager.setCurrentItem(i);
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

        action_openInBrowser = (LinearLayout) findViewById(R.id.action_open_in_browser);
        action_openInBrowser
            .setOnClickListener(new ArticleViewOpenInBrowserActionClickListener());
        action_next_unread = (LinearLayout) findViewById(R.id.action_next_unread);
        action_next_unread
            .setOnClickListener(new ArticleViewNextUnreadActionClickListener());
        action_previous_unread = (LinearLayout) findViewById(R.id.action_previous_unread);
        action_previous_unread
            .setOnClickListener(new ArticleViewPreviousUnreadActionClickListener());
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
		intent.putExtra(ARTICLE_SELECTED_KEY, viewPager.getCurrentItem());
		setResult(Activity.RESULT_OK, intent);
		Log.e("ArticleView", "Activity Finished.");
		super.finish();
	}

    public static void notifyPagerAdapterDataSetChanged() {
        if (viewPagerAdapter != null) {
            viewPagerAdapter.notifyDataSetChanged();
        }
    }

    public class ArticleViewOpenInBrowserActionClickListener implements
                                                             View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            RSSDataBundle rdBundle = rssData.get(viewPager.getCurrentItem());
            String url = rdBundle.getLink();
            // Log.e("URL Open", "URL: " + url);
            Toaster.showAlternateToast(ArticleView.this, getResources().getString(R.string.takingYouTo) , url, getResources().getDrawable(R.drawable.ic_action_web_site), Toast.LENGTH_LONG);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    public class ArticleViewNextUnreadActionClickListener implements
                                                          View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            for (int i = viewPager.getCurrentItem();i < rssData.size();i++) {
                if (!rssData.get(i).isRead()) {
                    viewPager.setCurrentItem(i, true);
                    return;
                }
            }
            Toaster.showToast(ArticleView.this, getResources().getString(R.string.noUnreadArticlesFound), Toast.LENGTH_SHORT);
        }
    }

    public class ArticleViewPreviousUnreadActionClickListener implements
                                                              View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            for (int i = viewPager.getCurrentItem();i >= 0;i--) {
                if (!rssData.get(i).isRead()) {
                    viewPager.setCurrentItem(i, true);
                    return;
                }
            }
            Toaster.showToast(ArticleView.this, getResources().getString(R.string.noUnreadArticlesFound), Toast.LENGTH_SHORT);
        }
    }
}
