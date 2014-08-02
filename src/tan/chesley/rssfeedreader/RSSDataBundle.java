package tan.chesley.rssfeedreader;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

public class RSSDataBundle implements Parcelable{
	private String title, description, link, source, sourceTitle; // Required descriptors
	private String pubDate; // Optional descriptors
	private final String stringUUID;

	public RSSDataBundle() {
		stringUUID = UUID.randomUUID().toString();
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
		return 6; // the total number of descriptors that are packaged in the parcel
	}

	public static final Parcelable.Creator<RSSDataBundle> CREATOR = new Parcelable.Creator<RSSDataBundle>() {
		public RSSDataBundle createFromParcel(Parcel in) {
			RSSDataBundle newRdBundle = new RSSDataBundle();
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
	}
}
