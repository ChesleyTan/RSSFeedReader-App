package tan.chesley.rssfeedreader;

import junit.framework.Assert;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ArticleViewFragment extends Fragment {

	private static final String RSSDATABUNDLE = "tan.chesley.rssfeedreader.rssdatabundle";
	private static final String ARTICLE_HEADLINE = "tan.chesley.rssfeedreader.articleheadline";
	private static final String HEADLINE = "tan.chesley.rssfeedreader.headline";
	private static final String ARTICLE = "tan.chesley.rssfeedreader.article";
	private static final String LINK = "tan.chesley.rssfeedreader.link";
	private static final String SOURCE = "tan.chesley.rssfeedreader.source";
	private String myHeadline = "";
	private String myArticle = "";
	private String myLink = "";
	private String mySource = "";
	private TextView articleTextView;
	private TextView titleTextView;
	private TextView sourceTextView;
	private Button openInBrowserButton;

	public static ArticleViewFragment newArticleViewFragment(String headline,
			RSSDataBundle rdBundle) {
		Bundle bundle = new Bundle();
		bundle.putString(ARTICLE_HEADLINE, headline);
		bundle.putParcelable(RSSDATABUNDLE, rdBundle);
		Assert.assertNotNull(rdBundle);
		ArticleViewFragment fragment = new ArticleViewFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			myHeadline = getArguments().getString(ARTICLE_HEADLINE);
			RSSDataBundle rdBundle = ((RSSDataBundle) getArguments()
					.getParcelable(RSSDATABUNDLE));
			myArticle = rdBundle.getDescription();
			myLink = rdBundle.getLink();
			mySource = rdBundle.getSourceTitle();
		}
		if (savedInstanceState != null) {
			myHeadline = savedInstanceState.getString(HEADLINE);
			myArticle = savedInstanceState.getString(ARTICLE);
			myLink = savedInstanceState.getString(LINK);
			mySource = savedInstanceState.getString(SOURCE);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View theView = inflater
				.inflate(R.layout.article_view, container, false);
		articleTextView = (TextView) theView.findViewById(R.id.articleTextView);
		articleTextView.setText(myArticle);
		titleTextView = (TextView) theView.findViewById(R.id.titleTextView);
		titleTextView.setText(myHeadline);
		sourceTextView = (TextView) theView.findViewById(R.id.sourceTextView);
		sourceTextView.setText(mySource);
		openInBrowserButton = (Button) theView
				.findViewById(R.id.openInBrowserButton);
		openInBrowserButton
				.setOnClickListener(new ArticleViewOpenInBrowserButtonClickListener());

		return theView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(HEADLINE, myHeadline);
		outState.putString(ARTICLE, myArticle);
		outState.putString(LINK, myLink);
		outState.putString(SOURCE, mySource);
	}

	public class ArticleViewOpenInBrowserButtonClickListener implements
			View.OnClickListener {

		@Override
		public void onClick(View arg0) {
			String url = myLink;
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				url = "http://" + url;
				Log.e("URL", "URL modified to " + url);
			}
			// Log.e("URL Open", "URL: " + url);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}

	}

}
