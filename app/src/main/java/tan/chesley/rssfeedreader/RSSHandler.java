package tan.chesley.rssfeedreader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tan.chesley.rssfeedreader.TaskFragment.GetRssFeedTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.audiofx.BassBoost;
import android.preference.PreferenceManager;
import android.util.Log;

public class RSSHandler extends DefaultHandler {

    final int stateUnknown = 0;
    final int stateTitle = 1;
    final int stateDescription = 2;
    final int stateLink = 3;
    final int stateSourceTitle = 4;
    final int statePubDate = 5;
    final long timeout; // timeout for parsing an individual feed
    final GetRssFeedTask parent;
    final String noDescriptionAvailableString;
    final int MILLISECONDS_IN_A_DAY = 86400000;
    final int maxArticleCount;
    final boolean enforceArticleAgeLimit;
    final boolean useFullDescription;
    final int maxDescriptionParts;
    final int maxSanitizationIterations;
    final String[] timezones = TimeZone.getAvailableIDs();
    final RSSDataBundleOpenHelper dbHelper;

    int articleAgeLimit; // age limit for the article in milliseconds from epoch
    ArrayList<MyMap> data = new ArrayList<MyMap>();

    int state = stateUnknown;
    int articleCount = 0;
    long startParsingTime = 0;
    RSSDataBundle rdBundle = null;
    boolean reading = false;
    String sourceTitle = null;
    String sourceURL = null;
    String articleTitlePart = "";
    String articleDescriptionPart = "";
    int numDescriptionParts = 0;
    int numSanitizationIterations = 0;
    boolean gotDescription = false;
    String linkPart = "";
    boolean badInput = false;

    public RSSHandler (GetRssFeedTask task, int numSources, long syncTimeout, Context context) {
        parent = task;
        dbHelper = new RSSDataBundleOpenHelper(context);
        noDescriptionAvailableString = context.getResources().getString(
            R.string.noDescriptionAvailable);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        maxArticleCount = prefs.getInt(SettingsActivity.KEY_PREF_MAX_ARTICLE_NUMBER,
                                       context.getResources().getInteger(R.integer.max_article_number_default));
        enforceArticleAgeLimit = prefs.getBoolean("pref_articleAgeLimitCheckBox", false);
        useFullDescription = prefs.getBoolean("pref_useFullDescriptionCheckBox", false);
        if (useFullDescription) {
            maxDescriptionParts = 500;
            maxSanitizationIterations = 100;
        }
        else {
            maxDescriptionParts = 25;
            maxSanitizationIterations = 20;
        }
        if (enforceArticleAgeLimit) {
            articleAgeLimit = MILLISECONDS_IN_A_DAY * prefs.getInt(SettingsActivity.KEY_PREF_ARTICLE_AGE_LIMIT, context.getResources().getInteger(R.integer.article_age_limit_default));
        }
        timeout = syncTimeout / numSources;
    }

    public void reset () {
        state = stateUnknown;
        articleCount = 0;
        startParsingTime = 0;
        rdBundle = null;
        reading = false;
        sourceTitle = null;
        sourceURL = null;
        articleTitlePart = "";
        articleDescriptionPart = "";
        numDescriptionParts = 0;
        numSanitizationIterations = 0;
        gotDescription = false;
        linkPart = "";
        badInput = false;
    }

    public void initializeRdBundleIfNeeded () {
        if (rdBundle == null) {
            rdBundle = new RSSDataBundle(null);
        }
    }

    @Override
    public void startDocument () throws SAXException {
        if (parent.isCancelled()) {
            throw new SAXException();
        }
        Log.e("Feed", "Started feed stream.");
        startParsingTime = System.currentTimeMillis();
    }

    @Override
    public void endDocument () throws SAXException {
        if (parent.isCancelled()) {
            throw new SAXException();
        }
        Log.e("Feed", "Ended feed stream.");
        reset();
    }

    @Override
    public void startElement (String uri, String localName, String qName,
                              org.xml.sax.Attributes attributes) throws SAXException {
        // Check if sync task was cancelled
        if (parent.isCancelled()) {
            throw new SAXException();
        }
        // Check if syncing timeout has been reached
        if (System.currentTimeMillis() - startParsingTime > timeout) {
            // Skip to next source
            reset();
            Log.e("RSSHandler",
                  "Individual source feed syncing timeout reached.");
            throw new SAXException();
        }
        state = stateUnknown;
        // Parse tag and save data
        if (reading && !badInput) {
            if (articleCount < maxArticleCount) {
                if (qName.equalsIgnoreCase("title")) {
                    state = stateTitle;
                }
                else if (qName.equalsIgnoreCase("description")) {
                    state = stateDescription;
                }
                else if (qName.equalsIgnoreCase("link")) {
                    state = stateLink;
                }
                else if (qName.equalsIgnoreCase("pubDate")) {
                    state = statePubDate;
                }
                else {
                    // Log.e("Unknown tag name", qName);
                }
            }
            else { // Stop when we have reached the max number of articles to
                // read
                reset();
                endDocument();
                throw new SAXException();
            }
        }

        if (!reading) {
            // Check if we need to get the source title
            if (sourceTitle == null && qName.equalsIgnoreCase("title")) {
                state = stateSourceTitle;
            }
            // Skip to first article
            else if (!reading && qName.equalsIgnoreCase("item")) {
                articleCount++;
                reading = true;
            }
        }
    }

    @Override
    public void endElement (String uri, String localName, String qName)
        throws SAXException {
        // Check if sync task has been cancelled
        if (parent.isCancelled()) {
            throw new SAXException();
        }
        // Check if syncing timeout has been reached
        if (System.currentTimeMillis() - startParsingTime > timeout) {
            // Skip to next source
            reset();
            Log.e("RSSHandler",
                  "Individual source feed syncing timeout reached.");
            throw new SAXException();
        }
        if (state == stateTitle) {
            if (!dbHelper.isUnique(dbHelper.getReadableDatabase(), articleTitlePart)) {
                badInput = true;
                Log.e("RSSHandler", "Article already databased. Skipping.");
            }
        }
        if (qName.equalsIgnoreCase("item")) {
            if (rdBundle != null && !badInput) {
                rdBundle.setTitle(articleTitlePart);
                // Log.e("New Headline", rdBundle.getTitle());
                if (!gotDescription) {
                    clearTagsAndSetDescription(articleDescriptionPart);
                }
                Log.e("Number of description parts", Integer.toString(numDescriptionParts));
                // Log.e("New Description", rdBundle.getDescription());
                rdBundle.setLink(linkPart);
                // Log.e("New Link", rdBundle.getLink());
                rdBundle.setSourceTitle(sourceTitle);
                rdBundle.setSource(sourceURL);
                MyMap datum = new MyMap();
                datum.put(rdBundle.getTitle(), rdBundle);
                data.add(datum);
            }
            // Reset rdBundle to store next item's data
            rdBundle = null;
            // Reset variable to store article title
            articleTitlePart = "";
            // Reset variables to store article description
            articleDescriptionPart = "";
            numDescriptionParts = 0;
            numSanitizationIterations = 0;
            gotDescription = false;
            // Reset variable to store link
            linkPart = "";
            // Reset the bad input flag
            badInput = false;
            // Stop reading until we get to the next item tag
            reading = false;
        }
    }

    @Override
    public void characters (char[] ch, int start, int length)
        throws SAXException {
        // Check if sync task has been cancelled
        if (parent.isCancelled()) {
            throw new SAXException();
        }
        // Check if syncing timeout has been reached
        if (System.currentTimeMillis() - startParsingTime > timeout) {
            // Skip to next source
            reset();
            Log.e("RSSHandler",
                  "Individual source feed syncing timeout reached.");
            throw new SAXException();
        }
        if (state == stateTitle) {
            articleTitlePart += makeString(ch, start, length, true);
        }
        else if (state == stateDescription && !gotDescription) {
            if (numDescriptionParts > maxDescriptionParts) {
                initializeRdBundleIfNeeded();
                Log.e("RSSHandler", "Max number of description parts reached. Using default description.");
                rdBundle.setDescription(noDescriptionAvailableString);
                gotDescription = true;
            }
            else {
                numDescriptionParts++;
                articleDescriptionPart += makeString(ch, start, length, false);
            }
        }
        else if (state == stateLink) {
            linkPart += makeString(ch, start, length, true);
        }
        else if (state == stateSourceTitle) {
            sourceTitle = makeString(ch, start, length, true);
            // Log.e("New Source Title", sourceTitle);
        }
        else if (state == statePubDate) {
            initializeRdBundleIfNeeded();
            String dateString = makeString(ch, start, length, true);
            String[] dateStringFields = dateString.split(" ");
            // TODO more robustly recognize bad input
            // TODO account for case when the year is only two digits
            if (dateStringFields.length < 6) {
                badInput = true;
                //Log.e("New pubDate", "Bad input found. Skipping this item.");
                return;
            }
            // dateStringFields[0] => Day of week
            // dateStringFields[1] => Number of day
            // dateStringFields[2] => Month
            // dateStringFields[3] => Year
            // dateStringFields[4] => hr:min:sec
            // dateStringFields[5] => UTC offset or timezone
            String pubDate = dateStringFields[1] + " " + dateStringFields[2]
                + " " + dateStringFields[3] + " " + dateStringFields[4];
            // Recognize when timezone given is not in the form of an offset
            // from UTC
            if (!dateStringFields[5].contains("0")
                && !dateStringFields[5].contains("5")) {
                boolean timeZoneFound = false;
                double offset = 0.0;
                for (String s : timezones) {
                    if (s.contains(dateStringFields[5])) {
                        TimeZone tz = TimeZone.getTimeZone(s);
                        //Log.e("RSSHandler", "Found match for time zone "
                        //		+ dateStringFields[5] + " : "
                        //		+ tz.getRawOffset() / 1000
                        //		/ 60 + " minutes.");
                        timeZoneFound = true;
                        offset = tz.getRawOffset();
                    }
                }
                if (!timeZoneFound) {
                    //Log.e("RSSHandler", "No match found for time zone "
                    //		+ dateStringFields[5] + ". Using local time zone instead.");
                    TimeZone here = TimeZone.getDefault();
                    offset = here.getRawOffset();
                    if (here.inDaylightTime(new Date())) {
                        offset += here.getDSTSavings();
                    }
                }
                offset = offset / 1000.0 / 60.0 / 60.0;
                int hourOffset = (int) offset;
                int minutesOffset = (int) (offset % 1.0 * 60);
                String hourOffsetStr = hourOffset < 10 && hourOffset >= 0 ? "0"
                    + hourOffset
                    : hourOffset > -10 && hourOffset < 0 ? "-0"
                    + Math.abs(hourOffset) : Integer
                    .toString(hourOffset);
                String minutesOffsetStr = minutesOffset == 0 ? "00" : Integer
                    .toString(minutesOffset);
                String sign = offset > 0 ? "+" : ""; // Negative offsets already
                // have a sign
                dateStringFields[5] = sign + hourOffsetStr + minutesOffsetStr;
                //Log.e("New pubDate", "Non-offset time zone detected. Using "
                //		+ sign + hourOffsetStr + minutesOffsetStr + " instead.");
            }
            pubDate += " " + dateStringFields[5];
            rdBundle.setPubDate(pubDate);
            if (enforceArticleAgeLimit) {
                long now = Calendar.getInstance().getTimeInMillis();
                long articlePubDate = rdBundle.getCalendar().getTimeInMillis();
                if (now - articlePubDate > articleAgeLimit) {
                    badInput = true;
                    Log.e("New pubDate", "Article exceeds age limit. Skipping.");
                    return;
                }
                if (articlePubDate > now) {
                    badInput = true;
                    Log.e("New pubDate", "Invalid date given: Date is in the future!");
                    return;
                }
            }
            // Log.e("New pubDate", rdBundle.getPubDate());
        }
    }

    public String makeString (char[] ch, int start, int length, boolean trim)
        throws SAXException {
        if (parent.isCancelled()) {
            throw new SAXException();
        }
        if (trim) {
            return new String(ch, start, length).trim().replaceAll("\\s+", " ");
        }
        else {
            return new String(ch, start, length).replaceAll("\\s+", " ");
        }
    }

    public void clearTagsAndSetDescription (String s) {
        int startIndex, endIndex;
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("<")) > -1 && (endIndex = s.indexOf(">")) > -1 && startIndex < endIndex) {
            s = s.substring(0, startIndex) + s.substring(endIndex + 1);
            numSanitizationIterations++;
        }
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("&quot;")) > -1) {
            s = s.substring(0, startIndex) + "\"" + s.substring(startIndex + 1);
            numSanitizationIterations++;
        }
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("&amp;")) > -1) {
            s = s.substring(0, startIndex) + "&" + s.substring(startIndex + 1);
            numSanitizationIterations++;
        }
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("&apos;")) > -1) {
            s = s.substring(0, startIndex) + "'" + s.substring(startIndex + 1);
            numSanitizationIterations++;
        }
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("&lt;")) > -1) {
            s = s.substring(0, startIndex) + "<" + s.substring(startIndex + 1);
            numSanitizationIterations++;
        }
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("&gt;")) > -1) {
            s = s.substring(0, startIndex) + ">" + s.substring(startIndex + 1);
            numSanitizationIterations++;
        }
        while (numSanitizationIterations < maxSanitizationIterations && (startIndex = s.indexOf("&")) > -1 && (endIndex = s.indexOf(";")) > -1 && startIndex < endIndex) {
            s = s.substring(0, startIndex) + s.substring(endIndex + 1);
            numSanitizationIterations++;
        }
        if (numSanitizationIterations < maxSanitizationIterations) {
            s = s.trim().replaceAll("\\s+", " ");
            rdBundle.setDescription(s);
        }
        else {
            Log.e("RSSHandler", "Max number of sanitization iterations reached. Using default description.");
            rdBundle.setDescription(noDescriptionAvailableString);
        }
        gotDescription = true;
    }

    public ArrayList<MyMap> getData () {
        return data;
    }

    public void notifyCurrentSource (String url) {
        sourceURL = url;
        // Log.e("New Source", url);
    }
}