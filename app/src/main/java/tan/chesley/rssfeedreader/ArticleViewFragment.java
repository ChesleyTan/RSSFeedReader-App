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
    private RSSDataBundle rdBundle;
	private TextView articleTextView;
	private TextView titleTextView;
	private TextView sourceTextView;
    private TextView dateTextView;
	private Button openInBrowserButton;

	public static ArticleViewFragment newArticleViewFragment(RSSDataBundle rdBundle) {
		Bundle bundle = new Bundle();
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
			rdBundle = ((RSSDataBundle) getArguments()
					.getParcelable(RSSDATABUNDLE));
		}
		if (savedInstanceState != null) {
            rdBundle = (RSSDataBundle) savedInstanceState.getParcelable(RSSDATABUNDLE);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View theView = inflater
				.inflate(R.layout.article_view, container, false);
		articleTextView = (TextView) theView.findViewById(R.id.articleTextView);
		articleTextView.setText(rdBundle.getDescription());
		titleTextView = (TextView) theView.findViewById(R.id.titleTextView);
		titleTextView.setText(rdBundle.getTitle());
		sourceTextView = (TextView) theView.findViewById(R.id.sourceTextView);
		sourceTextView.setText(rdBundle.getSourceTitle());
        dateTextView = (TextView) theView.findViewById(R.id.dateTextView);
        dateTextView.setText(rdBundle.getUserPreferredDateFormat(getActivity()));
		openInBrowserButton = (Button) theView
				.findViewById(R.id.openInBrowserButton);
		openInBrowserButton
				.setOnClickListener(new ArticleViewOpenInBrowserButtonClickListener());

		return theView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putParcelable(RSSDATABUNDLE, rdBundle);
	}

	public class ArticleViewOpenInBrowserButtonClickListener implements
			View.OnClickListener {

		@Override
		public void onClick(View arg0) {
			String url = rdBundle.getLink();
			// Log.e("URL Open", "URL: " + url);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
		}

	}

}
