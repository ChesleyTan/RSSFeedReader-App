package tan.chesley.rssfeedreader;

import java.util.ArrayList;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tan.chesley.rssfeedreader.TaskFragment.GetRssFeedTask;
import android.util.Log;

public class RSSHandler extends DefaultHandler {
	final int stateUnknown = 0;
	final int stateTitle = 1;
	final int stateDescription = 2;
	final int stateLink = 3;
	final int stateSourceTitle = 4;
	final long timeout = 2000; // 2 second timeout for feed parsing
	final GetRssFeedTask parent;
	ArrayList<MyMap> data = new ArrayList<MyMap>();

	int state = stateUnknown;
	int articleCount = 0;
	long startParsingTime = 0;
	RSSDataBundle rdBundle = null;
	boolean reading = false;
	String sourceTitle = null;
	String sourceURL = null;

	public RSSHandler(GetRssFeedTask task) {
		parent = task;
	}
	
	public void reset() {
		state = stateUnknown;
		articleCount = 0;
		startParsingTime = 0;
		rdBundle = null;
		reading = false;
		sourceTitle = null;
		sourceURL = null;
	}

	public void initializeRdBundleIfNeeded() {
		if (rdBundle == null) {
			rdBundle = new RSSDataBundle();
		}
	}

	@Override
	public void startDocument() throws SAXException {
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		Log.e("Feed", "Started feed stream.");
		startParsingTime = System.currentTimeMillis();
	}

	@Override
	public void endDocument() throws SAXException {
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		Log.e("Feed", "Ended feed stream.");
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			org.xml.sax.Attributes attributes) throws SAXException {
		
		if (parent.isCancelled()) {
			throw new SAXException();
		}

		if (System.currentTimeMillis() - startParsingTime > timeout) {
			// Skip to next source
			reset();
			throw new SAXException();
		}
		
		if (!reading && localName.equalsIgnoreCase("title") && sourceTitle == null) {
			state = stateSourceTitle;
		}
			
		// Skip to first article
		if (!reading && localName.equalsIgnoreCase("item")) {
			reading = true;
			articleCount++;
		}

		// Parse tag and save data
		if (reading) {
			if (articleCount < HeadlinesFragment.RSS_ARTICLE_COUNT_MAX
					&& state == stateUnknown) {
				if (localName.equalsIgnoreCase("title")) {
					state = stateTitle;
				} else if (localName.equalsIgnoreCase("description")) {
					state = stateDescription;
				} else if (localName.equalsIgnoreCase("link")) {
					state = stateLink;
				} else {
					// Log.e("Unknown tag name", localName);
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
		
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		
		if (System.currentTimeMillis() - startParsingTime > timeout) {
			// Skip to next source
			reset();
			throw new SAXException();
		}
		if (localName.equalsIgnoreCase("item")) {
			if (rdBundle != null) {
				rdBundle.setSourceTitle(sourceTitle);
				rdBundle.setSource(sourceURL);
				MyMap datum = new MyMap();
				datum.put(rdBundle.getTitle(), rdBundle);
				data.add(datum);
			}
			rdBundle = null;
			// Stop reading until we get to the next item tag
			reading = false;
		}
		state = stateUnknown;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		if (System.currentTimeMillis() - startParsingTime > timeout) {
			// Skip to next source
			reset();
			throw new SAXException();
		}
		if (state == stateTitle) {
			// Log.e("New Headline", strCharacters);
			initializeRdBundleIfNeeded();
			if (rdBundle.getTitle() == null) {
				rdBundle.setTitle(makeString(ch, start, length));
			}
		} else if (state == stateDescription) {
			// Log.e("New Description", strCharacters);
			initializeRdBundleIfNeeded();
			if (rdBundle.getDescription() == null) {
				String s = makeString(ch, start, length);
				// Special case where description contains garbage data
				if (s.contains("<") || s.contains(">")) {
					// TODO Figure out how to get a string resource without invoking an activity
					//rdBundle.setDescription(new Activity().getResources().getString(R.string.noDescriptionAvailable));
					rdBundle.setDescription("No description available.");
				} else {
					// If good input, then just set description
					rdBundle.setDescription(s);
				}
			}
		} else if (state == stateLink) {
			// Log.e("New Link", strCharacters);
			initializeRdBundleIfNeeded();
			if (rdBundle.getLink() == null) {
				rdBundle.setLink(makeString(ch, start, length));
			}
		} else if (state == stateSourceTitle) {
			sourceTitle = makeString(ch, start, length);
			//Log.e("New Source Title", sourceTitle);
		}
	}

	public String makeString(char[] ch, int start, int length) {
		return new String(ch, start, length).trim().replaceAll("\\s+", " ");
	}
	
	public ArrayList<MyMap> getData() {
		return data;
	}
	
	public void notifyCurrentSource(String url) {
		sourceURL = url;
		Log.e("New Source", url);
	}
}