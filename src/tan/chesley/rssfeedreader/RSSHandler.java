package tan.chesley.rssfeedreader;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tan.chesley.rssfeedreader.TaskFragment.GetRssFeedTask;
import android.content.Context;
import android.util.Log;

public class RSSHandler extends DefaultHandler {
	final int stateUnknown = 0;
	final int stateTitle = 1;
	final int stateDescription = 2;
	final int stateLink = 3;
	final int stateSourceTitle = 4;
	final int statePubDate = 5;
	final long timeout = 2000; // 2 second timeout for feed parsing
	final GetRssFeedTask parent;
	final String noDescriptionAvailableString;
	ArrayList<MyMap> data = new ArrayList<MyMap>();

	int state = stateUnknown;
	int articleCount = 0;
	long startParsingTime = 0;
	RSSDataBundle rdBundle = null;
	boolean reading = false;
	String sourceTitle = null;
	String sourceURL = null;
	boolean badInput = false;

	public RSSHandler(GetRssFeedTask task, Context context) {
		parent = task;
		noDescriptionAvailableString = context.getResources().getString(R.string.noDescriptionAvailable);
	}
	
	public void reset() {
		state = stateUnknown;
		articleCount = 0;
		startParsingTime = 0;
		rdBundle = null;
		reading = false;
		sourceTitle = null;
		sourceURL = null;
		badInput = false;
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
		// Check if sync task was cancelled
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		// Check if syncing timeout has been reached
		if (System.currentTimeMillis() - startParsingTime > timeout) {
			// Skip to next source
			reset();
			Log.e("RSSHandler", "Individual source feed syncing timeout reached.");
			throw new SAXException();
		}
		// Check if we need to get the source title
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
				} else if (localName.equalsIgnoreCase("pubDate")) {
					state = statePubDate;
				} else {
					// Log.e("Unknown tag name", localName);
				}
			} else { // Stop when we have reached the max number of articles to read
				reset();
				endDocument();
				throw new SAXException();
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// Check if sync task has been cancelled
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		// Check if syncing timeout has been reached
		if (System.currentTimeMillis() - startParsingTime > timeout) {
			// Skip to next source
			reset();
			Log.e("RSSHandler", "Individual source feed syncing timeout reached.");
			throw new SAXException();
		}
		if (localName.equalsIgnoreCase("item")) {
			if (rdBundle != null && !badInput) {
				rdBundle.setSourceTitle(sourceTitle);
				rdBundle.setSource(sourceURL);
				MyMap datum = new MyMap();
				datum.put(rdBundle.getTitle(), rdBundle);
				data.add(datum);
			}
			// Reset rdBundle to store next item's data
			rdBundle = null;
			// Reset the bad input flag
			badInput = false;
			// Stop reading until we get to the next item tag
			reading = false;
		}
		state = stateUnknown;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// Check if sync task has been cancelled
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		// Check if syncing timeout has been reached
		if (System.currentTimeMillis() - startParsingTime > timeout) {
			// Skip to next source
			reset();
			Log.e("RSSHandler", "Individual source feed syncing timeout reached.");
			throw new SAXException();
		}
		if (state == stateTitle) {
			initializeRdBundleIfNeeded();
			if (rdBundle.getTitle().equals("")) {
				rdBundle.setTitle(makeString(ch, start, length));
				//Log.e("New Headline", rdBundle.getTitle());
			}
		} else if (state == stateDescription) {
			initializeRdBundleIfNeeded();
			if (rdBundle.getDescription().equals("")) {
				String s = makeString(ch, start, length);
				// Special case where description contains garbage data
				if (s.contains("<") || s.contains(">")) {
					rdBundle.setDescription(noDescriptionAvailableString);
				} else {
					// If good input, then just set description
					rdBundle.setDescription(s);
				}
				//Log.e("New Description", rdBundle.getDescription());
			}
		} else if (state == stateLink) {
			initializeRdBundleIfNeeded();
			if (rdBundle.getLink().equals("")) {
				rdBundle.setLink(makeString(ch, start, length));
			}
			// Log.e("New Link", rdBundle.getLink());
		} else if (state == stateSourceTitle) {
			sourceTitle = makeString(ch, start, length);
			// Log.e("New Source Title", sourceTitle);
		} else if (state == statePubDate) {
			String dateString = makeString(ch, start, length);
			String[] dateStringFields = dateString.split(" ");
			// TODO more robustly recognize bad input
			if (dateStringFields.length < 6) {
				badInput = true;
				Log.e("New pubDate", "Bad input found. Skipping this item.");
				return;
			}
			String pubDate = dateStringFields[1] + " " + dateStringFields[2] + " " + dateStringFields[3] + " " + dateStringFields[4];
			// Recognize when timezone given is not in the form of an offset from UTC
			if (!dateStringFields[5].contains("0") && !dateStringFields[5].contains("5")) {
				TimeZone here = TimeZone.getDefault();
				double offset = here.getRawOffset();
				if (here.inDaylightTime(new Date())) {
					offset += here.getDSTSavings();
				}
				offset = offset / 1000.0 / 60.0 / 60.0;
				int hourOffset = (int) offset;
				int minutesOffset = (int) (offset % 1.0 * 60);
				String hourOffsetStr = hourOffset < 10 && hourOffset >= 0 ? "0" + hourOffset : 
						hourOffset > -10 && hourOffset < 0 ? "-0" + Math.abs(hourOffset) : Integer.toString(hourOffset);
				String minutesOffsetStr = minutesOffset == 0 ? "00" : Integer.toString(minutesOffset);
				String sign = offset > 0 ? "+" : ""; // Negative offsets already have a sign 
				String timeZone = sign + hourOffsetStr + minutesOffsetStr;
				Log.e("New pubDate", "Non-offset time zone detected. Using " + sign + hourOffsetStr + minutesOffsetStr + " instead.");
				pubDate += " " + timeZone;
			} else {
				pubDate += " " + dateStringFields[5];
			}
			rdBundle.setPubDate(pubDate);
			//Log.e("New pubDate", rdBundle.getPubDate());
		}
	}

	public String makeString(char[] ch, int start, int length) throws SAXException {
		if (parent.isCancelled()) {
			throw new SAXException();
		}
		return new String(ch, start, length).trim().replaceAll("\\s+", " ");
	}
	
	public ArrayList<MyMap> getData() {
		return data;
	}
	
	public void notifyCurrentSource(String url) {
		sourceURL = url;
		//Log.e("New Source", url);
	}
}