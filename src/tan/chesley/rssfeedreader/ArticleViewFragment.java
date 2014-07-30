package tan.chesley.rssfeedreader;

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
	private String myHeadline = "";
	private String myArticle = "";
	private String myLink = "";
	private TextView articleTextView;
	private Button openInBrowserButton;

	public static ArticleViewFragment newArticleViewFragment(String headline,
			RSSDataBundle rdBundle) {
		Bundle bundle = new Bundle();
		bundle.putString(ARTICLE_HEADLINE, headline);
		bundle.putParcelable(RSSDATABUNDLE, rdBundle);
		assert (rdBundle != null);
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
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View theView = inflater
				.inflate(R.layout.article_view, container, false);
		articleTextView = (TextView) theView.findViewById(R.id.articleTextView);
		articleTextView.setText(myArticle);
		openInBrowserButton = (Button) theView
				.findViewById(R.id.openInBrowserButton);
		openInBrowserButton
				.setOnClickListener(new ArticleViewOpenInBrowserButtonClickListener());

		return theView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	public class ArticleViewOpenInBrowserButtonClickListener implements
			View.OnClickListener {

		@Override
		public void onClick(View arg0) {
			String url = myLink;
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				url = "http://" + url;
			}
			Log.e("URL Open", "URL: " + url);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}

	}

}
