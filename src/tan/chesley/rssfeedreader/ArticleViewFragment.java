package tan.chesley.rssfeedreader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ArticleViewFragment extends Fragment {
	private static final String RSSDATABUNDLE = "tan.chesley.rssfeedreader.rssdatabundle";
	private String myHeadline = "";
	private String myArticle = "";
	private TextView articleTextView;
	
	public static ArticleViewFragment newArticleViewFragment(String headline, RSSDataBundle rdBundle) {
		Bundle bundle = new Bundle();
		bundle.putString(HeadlinesFragment.ARTICLE_HEADLINE, headline);
		bundle.putParcelable(RSSDATABUNDLE, rdBundle);
		ArticleViewFragment fragment = new ArticleViewFragment();
		fragment.setArguments(bundle);
		Log.e("ArticleView", "ArticleView4: " + rdBundle.getDescription());
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myHeadline = getArguments().getString(HeadlinesFragment.ARTICLE_HEADLINE);
		myArticle = ((RSSDataBundle) getArguments().getParcelable(RSSDATABUNDLE)).getDescription();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View theView = inflater.inflate(R.layout.article_view, container, false);
		articleTextView = (TextView) theView.findViewById(R.id.articleTextView);
		articleTextView.setText(myArticle);
		Log.e("ArticleView", "ArticleView5: " + myArticle);
		
		return theView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	
}
