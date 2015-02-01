package tan.chesley.rssfeedreader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ArticleViewFragment extends Fragment {

	private static final String RSSDATABUNDLE = "tan.chesley.rssfeedreader.rssdatabundle";
    private RSSDataBundle rdBundle;
	private TextView articleTextView;
	private TextView titleTextView;
	private TextView sourceTextView;
    private TextView dateTextView;

	public static ArticleViewFragment newArticleViewFragment(RSSDataBundle rdBundle) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(RSSDATABUNDLE, rdBundle);
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
				.inflate(R.layout.article_view_fragment, container, false);
		articleTextView = (TextView) theView.findViewById(R.id.articleTextView);
        // TODO show images
		//articleTextView.setText(Html.fromHtml(rdBundle.getDescription(getActivity()), new URLImageParser(articleTextView, getActivity()), null));
        //articleTextView.setText(Html.fromHtml(rdBundle.getDescription(getActivity())));
        articleTextView.setText(rdBundle.getSpannedDescription(getActivity()));
        boolean centerDescriptionText = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_centerArticleViewDescriptionText", false);
        if (centerDescriptionText) {
            articleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        // TODO make text selectable while retaining clickability of the links
        // Make links clickable
        articleTextView.setMovementMethod(LinkMovementMethod.getInstance());
        /* TODO how to modify the charsequence and still preserve the spannable quality
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charSequence.length();i++) {
            if (charSequence.charAt(i) == '\n') {
                Log.e("newline", "found");
            }
            else {
                sb.append(charSequence.charAt(i));
            }
        }
        */

		titleTextView = (TextView) theView.findViewById(R.id.titleTextView);
		titleTextView.setText(rdBundle.getTitle());
		sourceTextView = (TextView) theView.findViewById(R.id.sourceTextView);
		sourceTextView.setText(rdBundle.getSourceTitle());
        dateTextView = (TextView) theView.findViewById(R.id.dateTextView);
        dateTextView.setText(rdBundle.getUserPreferredDateFormat(getActivity()));

		return theView;
	}

    @Override
    public void onResume () {
        super.onResume();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run () {
                CharSequence charSequence = rdBundle.getSpannedDescription(getActivity());
                SpannableStringBuilder sp = new SpannableStringBuilder(charSequence);
                URLSpan[] spans = sp.getSpans(0, charSequence.length(), URLSpan.class);
                for (URLSpan urlSpan : spans) {
                    MyURLSpan mySpan = new MyURLSpan(urlSpan.getURL());
                    sp.setSpan(mySpan, sp.getSpanStart(urlSpan),
                               sp.getSpanEnd(urlSpan), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    sp.removeSpan(urlSpan);
                }
                articleTextView.setText(sp);
            }
        });

    }

    private class MyURLSpan extends ClickableSpan {
        private String url;

        public MyURLSpan(String url) {
            super();
            this.url = url;
        }

        @Override
        public void onClick(View v) {
            //Toast.makeText(getActivity(), getResources().getString(R.string.takingYouTo) + url, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            Toaster.showAlternateToast(getActivity(), getResources().getString(R.string.takingYouTo) , url, getResources().getDrawable(R.drawable.ic_action_web_site), Toast.LENGTH_LONG);
            startActivity(intent);
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putParcelable(RSSDATABUNDLE, rdBundle);
	}

    /*
    public Html.ImageGetter imageGetter() {
        Html.ImageGetter imageGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable (String s) {
                Log.e("Fetching", " " + s);
                if (imageFetchAsyncTask.getDrawable() == null) {
                    if (!imageFetchAsyncTask.isRunning()) {
                        imageFetchAsyncTask.setRunning(true);
                        imageFetchAsyncTask.execute(s);
                    }
                    Drawable d = getResources().getDrawable(R.drawable.ic_action_import_export);
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    return d;
                }
                else {
                    //return imageFetchAsyncTask.getDrawable();
                    Drawable d = getResources().getDrawable(R.drawable.ic_action_refresh);
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    return d;
                }
            }
        };
        return imageGetter;
    }

    public class ImageFetchAsyncTask extends AsyncTask<String, Void, Void> {
        private TextView mCallback;
        private Drawable drawable;
        private boolean running;

        public ImageFetchAsyncTask(TextView textView) {
            mCallback = textView;
        }
        @Override
        protected Void doInBackground (String... strings) {
            running = true;
            try {
                Log.e("Fetching from: ", strings[0]);
                drawable = Drawable.createFromStream(new URL(strings[0]).openStream(), strings[0]);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                Log.e("ImageGetter", "Got drawable");
            } catch(IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Drawable getDrawable() {
            return drawable;
        }

        @Override
        protected void onPostExecute (Void aVoid) {
            if (drawable != null) {
                Log.e("ImageGetter", "success");
            }
            else {
                Log.e("ImageGetter", "failed");
                drawable = getResources().getDrawable(R.drawable.ic_action_refresh);
            }
            running = false;
            super.onPostExecute(aVoid);
            mCallback.invalidate();
            for (Drawable d : mCallback.getCompoundDrawables()) {
                try {
                    mCallback.invalidateDrawable(d);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            ((View)mCallback.getParent()).invalidate();
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean bool) {
            running = bool;
        }
    }
    */

}
