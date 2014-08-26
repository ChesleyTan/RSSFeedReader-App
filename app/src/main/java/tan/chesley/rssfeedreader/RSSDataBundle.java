package tan.chesley.rssfeedreader;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class RSSDataBundle implements Parcelable{
    public static final String NUMBERS = "0123456789";
	private String title, description, link, source, sourceTitle; // Required descriptors
	private String pubDate; // Optional descriptors
	private String stringUUID;
    private boolean read;
    private long age;

	public RSSDataBundle(String stringUUID) {
        if (stringUUID == null) {
            this.stringUUID = UUID.randomUUID().toString();
        }
        else {
            this.stringUUID = stringUUID;
        }
		title = description = link = source = sourceTitle = pubDate = "";
	}
	
	public String getId() {
		return stringUUID;
	}
	
	public String getTitle() {
		return title;
	}
	public RSSDataBundle setTitle(String title) {
		this.title = title;
		return this;
	}
	public String getDescription() {
		return description;
	}
	public RSSDataBundle setDescription(String description) {
		this.description = description;
		return this;
	}
	public String getLink() {
		return link;
	}
	public RSSDataBundle setLink(String link) {
		this.link = link;
		return this;
	}
	public String getSource() {
		return source;
	}
	public RSSDataBundle setSource(String source) {
		this.source = source;
		return this;
	}
	public String getSourceTitle() {
		return sourceTitle;
	}
	public RSSDataBundle setSourceTitle(String sourceTitle) {
		this.sourceTitle = sourceTitle;
		return this;
	}
	public String getPubDate() {
		return pubDate;
	}
	public RSSDataBundle setPubDate(String pubDate) {
		this.pubDate = pubDate;
		return this;
	}
    public long getAge() {
        if (age == 0) {
            age = getCalendar().getTimeInMillis();
        }
        return age;
    }
    public RSSDataBundle setAge(long age) {
        this.age = age;
        return this;
    }
    public boolean isRead() {
        return read;
    }
    public void setRead(boolean bool) {
        read = bool;
    }
    // NOTE: This method must be called when the data in the RSSDataBundle is changed after
    // initialization to update the data inside the database
    public void notifyDatabaseDataChanged(Context context) {
        new RSSDataBundleOpenHelper(context).updateData(this);
    }
	public Calendar getCalendar() {
		Calendar calendar = Calendar.getInstance();
		String[] pubDateFields = pubDate.split(" ");
		int year = Integer.parseInt(pubDateFields[2]);
		String monthStr = pubDateFields[1];
		int month = monthStr.contains("Jan") ? Calendar.JANUARY :
					monthStr.contains("Feb") ? Calendar.FEBRUARY :
					monthStr.contains("Mar") ? Calendar.MARCH :
					monthStr.contains("Apr") ? Calendar.APRIL :
					monthStr.contains("May") ? Calendar.MAY :
					monthStr.contains("Jun") ? Calendar.JUNE :
					monthStr.contains("Jul") ? Calendar.JULY :
					monthStr.contains("Aug") ? Calendar.AUGUST :
					monthStr.contains("Sep") ? Calendar.SEPTEMBER :
					monthStr.contains("Oct") ? Calendar.OCTOBER :
					monthStr.contains("Nov") ? Calendar.NOVEMBER :
					Calendar.DECEMBER;
		int day = safeParseInt(pubDateFields[0]);
		String[] timeFields = pubDateFields[3].split(":");
		int hour = safeParseInt(timeFields[0]);
		int minute = safeParseInt(timeFields[1]);
		int second = safeParseInt(timeFields[2]);
		calendar.set(year, month, day, hour, minute, second);
		TimeZone here = TimeZone.getDefault();
		int offset = here.getRawOffset();
		if (here.inDaylightTime(new Date())) {
			offset += here.getDSTSavings();
		}
		int hourOffset = pubDateFields[4].substring(0, 1).equals("+") ? -1 * safeParseInt(pubDateFields[4].substring(1, 3)) : safeParseInt(pubDateFields[4].substring(1, 3));
		int minuteOffset = pubDateFields[4].substring(0, 1).equals("+") ? -1 * safeParseInt(pubDateFields[4].substring(3)) : safeParseInt(pubDateFields[4].substring(3));
		minuteOffset += offset / 1000 / 60;
		calendar.add(Calendar.HOUR, hourOffset);
		calendar.add(Calendar.MINUTE, minuteOffset);
		return calendar;
	}
	public String getAbsoluteDate() {
		Calendar calendar = getCalendar();
		int hr = calendar.get(Calendar.HOUR_OF_DAY);
		String hour = Integer.toString(hr);
		if (hr < 10) {
			hour = "0" + hour;
		}
		int min = calendar.get(Calendar.MINUTE);
		String minute = Integer.toString(min);
		if (min < 10) {
			minute = "0" + minute;
		}
		int sec = calendar.get(Calendar.SECOND);
		String second = Integer.toString(sec);
		if (sec < 10) {
			second = "0" + second;
		}
		int mth = calendar.get(Calendar.MONTH);
		String month = mth == 1 ? "Jan" :
					   mth == 2 ? "Feb" :
					   mth == 3 ? "Mar" :
					   mth == 4 ? "Apr" :
					   mth == 5 ? "May" :
					   mth == 6 ? "Jun" :
					   mth == 7 ? "Jul" :
					   mth == 8 ? "Aug" :
					   mth == 9 ? "Sep" :
					   mth == 10 ? "Oct" :
					   mth == 11 ? "Nov" :
					   "Dec";
		String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
		String year = Integer.toString(calendar.get(Calendar.YEAR));
		return  hour + ":" + minute + ":" + second + " " +
				month + " " + day + " " + year;
	}

    public String getRelativeDate() {
        return DateUtils.getRelativeTimeSpanString(getCalendar().getTimeInMillis()).toString();
    }

    public String getUserPreferredDateFormat(Context context) {
        int formatType = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_feedDateFormat_type", "1"));
        if (formatType == 1) {
            return getRelativeDate();
        }
        else {
            return getAbsoluteDate();
        }
    }

    public int safeParseInt(String s) {
        int result = 0;
        for (int i = 0;i < s.length();i++) {
            int indexAndValue = NUMBERS.indexOf(s.substring(i, i+1));
            if (indexAndValue > -1) {
                result = result * 10 + indexAndValue;
            }
        }
        return result;
    }

	@SuppressLint("Recycle")
	public Parcel getParcel() {
		Parcel parcel = Parcel.obtain();
		writeToParcel(parcel, 0);
		return parcel;
	}
	
	public void restoreParcel(Parcel parcel) {
		title = parcel.readString();
		description = parcel.readString();
		link = parcel.readString();
		source = parcel.readString();
		sourceTitle = parcel.readString();
		pubDate = parcel.readString();
        stringUUID = parcel.readString();
        age = parcel.readLong();
        read = parcel.readInt() == 1;
		/*
		Log.e("RSSDataBundle", "Read Title: " + title);
		Log.e("RSSDataBundle", "Read Desc: " + description);
		Log.e("RSSDataBundle", "Read Link: " + link);
		Log.e("RSSDataBundle", "Read Source: " + source);
		Log.e("RSSDataBundle", "Read Source Title: " + sourceTitle);
		Log.e("RSSDataBundle", "Read pubData: " + pubDate);
		*/
	}
	
	public int getParceledLength() {
		return 9; // the total number of descriptors that are packaged in the parcel
	}

	public static final Parcelable.Creator<RSSDataBundle> CREATOR = new Parcelable.Creator<RSSDataBundle>() {
		public RSSDataBundle createFromParcel(Parcel in) {
			RSSDataBundle newRdBundle = new RSSDataBundle("");
			newRdBundle.restoreParcel(in);
			return newRdBundle;
		}

		public RSSDataBundle[] newArray(int size) {
			return new RSSDataBundle[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		/*
		Log.e("RSSDataBundle", "Write Title: " + title);
		Log.e("RSSDataBundle", "Write Desc: " + description);
		Log.e("RSSDataBundle", "Write Link: " + link);
		Log.e("RSSDataBundle", "Write Source: " + source);
		Log.e("RSSDataBundle", "Write Source Title: " + sourceTitle);
		Log.e("RSSDataBundle", "Write pubData: " + pubDate);
		*/
		arg0.writeString(title);
		arg0.writeString(description);
		arg0.writeString(link);
		arg0.writeString(source);
		arg0.writeString(sourceTitle);
		arg0.writeString(pubDate);
        arg0.writeString(stringUUID);
        arg0.writeLong(age);
        arg0.writeInt(read ? 1 : 0);
	}

    public static void markAsRead(Context context, RSSDataBundle rdBundle) {
       if (!rdBundle.isRead()) {
            rdBundle.setRead(true);
            rdBundle.notifyDatabaseDataChanged(context);
       }
    }
}
