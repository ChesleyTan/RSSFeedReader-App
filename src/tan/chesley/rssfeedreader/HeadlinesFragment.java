package tan.chesley.rssfeedreader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class HeadlinesFragment extends ListFragment {
	public static final String PARSED_FEED_DATA = "tan.chesley.rssfeedreader.parsedfeeddata";
	public static final String ARTICLE_HEADLINE = "tan.chesley.rssfeedreader.articleheadline";
	public static final int RSS_ARTICLE_COUNT_MAX = 50;
	public static final String[] FEEDS = new String[] {
			"http://rss.cnn.com/rss/cnn_world.rss",
			"http://rss.cnn.com/rss/cnn_tech.rss" };
	public static Map<String, RSSDataBundle> headlines = new MyMap();
	public static ArrayList<MyMap> data = new ArrayList<MyMap>();
	private static HeadlinesFragment singleton;
	public Button updateButton;

	/*
	 * static { headlines = new HashMap<String, String>();
	 * headlines.put("Headline 1", "Article 1"); headlines.put("Headline 2",
	 * "Article 2"); headlines.put("Headline 3", "Article 3");
	 * headlines.put("Headline 4", "Article 4"); Log.e("HeadlinesFragment",
	 * "HeadlinesFragment initialized."); }
	 */

	public HeadlinesFragment() {
		singleton = this;
	}

	public static HeadlinesFragment getInstance() {
		return singleton;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setRetainInstance(true);
		Log.e("Instance", "Instance: Calling onCreate method for fragment.");

		updateButton = (Button) getActivity().findViewById(R.id.updateButton);

		if (savedInstanceState != null) {
			ArrayList<MyMap> tmp = savedInstanceState
					.getParcelableArrayList(PARSED_FEED_DATA);
			if (tmp == null) {
				Log.e("Instance", "Instance: no saved data found.");
			} else {
				data = tmp;
				updateListAdapter();
				Log.e("Instance", "Restored Instance State.");
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		/*
		 * // Display progress bar while the list loads ProgressBar progressBar
		 * = new ProgressBar(getActivity()); progressBar.setLayoutParams(new
		 * LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
		 * Gravity.CENTER)); progressBar.setIndeterminate(true);
		 * getListView().setEmptyView(progressBar);
		 */

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(PARSED_FEED_DATA, data);
		Log.e("Instance", "Saved Instance State.");
	}

	public void syncFeeds() {
		new GetRssFeedTask().execute(FEEDS);
		Log.e("Instance", "Instance: syncing feeds.");
	}

	private void updateFeedView() {
		ArrayList<MyMap> newData = new ArrayList<MyMap>();
		for (String s : headlines.keySet()) {
			MyMap datum = new MyMap();
			datum.put(s, headlines.get(s));
			newData.add(datum);
		}
		data = newData;
		updateListAdapter();
	}

	private void updateListAdapter() {
		if (getListAdapter() == null) {
			HeadlinesAdapter adapter = new HeadlinesAdapter(data);
			setListAdapter(adapter);
		}
		((HeadlinesAdapter) getListAdapter()).notifyDataSetChanged();
	}
	
	private class HeadlinesAdapter extends ArrayAdapter<MyMap> {
		
		public HeadlinesAdapter(ArrayList<MyMap> myData) {
			super(getActivity(), R.layout.feed_list_item, myData);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.feed_list_item, null);
			}
			MyMap dataMap = getItem(position);
			TextView headlineTextView = (TextView) convertView.findViewById(android.R.id.text1);
			TextView articleTextView = (TextView) convertView.findViewById(android.R.id.text2);
			String headline = dataMap.keySet().iterator().next();
			headlineTextView.setText(headline);
			articleTextView.setText(dataMap.get(headline).getDescription());
			return convertView;
		}
		
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.e("Instance", "Instance: Inflating new fragment.");
		View rootView = inflater.inflate(R.layout.fragment_rssfeed, container,
				false);
		return rootView;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Map<String, RSSDataBundle> map = ((Map<String, RSSDataBundle>) ((HeadlinesAdapter) getListAdapter()).getItem(position));
		Log.e("Click", "Clicked " + map.toString());
		Intent intent = new Intent(getActivity(), ArticleView.class);
		intent.putExtra(ARTICLE_HEADLINE, map.keySet().iterator().next());
		startActivity(intent);
	}
	
	public ArrayList<MyMap> getRssData() {
		return data;
	}

	private class RSSHandler extends DefaultHandler {
		final int stateUnknown = 0;
		final int stateTitle = 1;
		final int stateDescription = 2;
		final long timeout = 2000; // 2 second timeout for feed parsing
		
		int state = stateUnknown;
		int articleCount = 0;
		long startParsingTime = 0;
		String title = "";
		boolean reading = false;
		
		public void reset() {
			state = stateUnknown;
			articleCount = 0;
			startParsingTime = 0;
			title = "";
			reading = false;
		}

		@Override
		public void startDocument() throws SAXException {
			Log.e("Feed", "Started feed stream.");
			startParsingTime = System.currentTimeMillis();
		}

		@Override
		public void endDocument() throws SAXException {
			Log.e("Feed", "Ended feed stream.");
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				org.xml.sax.Attributes attributes) throws SAXException {
			if (System.currentTimeMillis() - startParsingTime > timeout) {
				reset();
				throw new SAXException();
			}
			// Skip to first article
			if (!reading && localName.equalsIgnoreCase("item")) {
				reading = true;
			}

			// Parse tag and save data
			if (reading) {
				if (articleCount < RSS_ARTICLE_COUNT_MAX) {
					if (localName.equalsIgnoreCase("title")) {
						state = stateTitle;
						articleCount++;
					} else if (localName.equalsIgnoreCase("description")) {
						state = stateDescription;
					} else {
						// Log.e("Unknown tag name", localName);
						state = stateUnknown;
					}
				} else {
					reset();
					throw new SAXException();
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			state = stateUnknown;
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			String strCharacters = new String(ch, start, length);
			if (state == stateTitle) {
				//Log.e("New Headline", strCharacters);
				// Store title temporarily for later storage in a key-value pair
				title = strCharacters;
			} else if (state == stateDescription && !title.equals("")) {
				// Write title and description in a key-value pair to the
				// headlines HashMap
				//Log.e("New Description", strCharacters);
				RSSDataBundle rdBundle = new RSSDataBundle();
				rdBundle.setTitle(title).setDescription(strCharacters);
				headlines.put(title, rdBundle);
				title = "";
			}
		}
	}

	public class GetRssFeedTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... urls) {
			try {

				SAXParserFactory mySAXParserFactory = SAXParserFactory
						.newInstance();
				SAXParser mySAXParser = mySAXParserFactory.newSAXParser();
				XMLReader myXMLReader = mySAXParser.getXMLReader();
				RSSHandler myRSSHandler = new RSSHandler();
				myXMLReader.setContentHandler(myRSSHandler);
				InputSource myInputSource;
				URL url;
				for (String s : urls) {
					url = new URL(s);
					Log.e("Feed", "Syncing feed " + s);
					myInputSource = new InputSource(url.openStream());
					try {
						myXMLReader.parse(myInputSource);
					} catch (SAXException e) {
						continue;
					}
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
				Log.e("Parser", "Cannot connect to RSS feed!");
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				Log.e("Parser", "Cannot connect to RSS feed!");
			} catch (SAXException e) {
				e.printStackTrace();
				Log.e("Parser", "Cannot connect to RSS feed!");
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("Parser", "Cannot connect to RSS feed!");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			updateFeedView();
		}

	}

	private static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
		ArrayList<View> views = new ArrayList<View>();
		final int childCount = root.getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = root.getChildAt(i);
			if (child instanceof ViewGroup) {
				views.addAll(getViewsByTag((ViewGroup) child, tag));
			}

			final Object tagObj = child.getTag();
			if (tagObj != null && tagObj.equals(tag)) {
				views.add(child);
			}

		}
		return views;
	}
}
