package tan.chesley.rssfeedreader;

import java.util.UUID;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

public class RSSDataBundle implements Parcelable{
	private String title, description, link, source, sourceTitle; // Required descriptors
	private String pubDate; // Optional descriptors
	private String[] data = new String[] {title, description, link, source, sourceTitle, pubDate};
	private final String stringUUID;

	public RSSDataBundle() { stringUUID = UUID.randomUUID().toString(); }
	
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
		for (String s : data) {
			if (s != null) {
				parcel.writeByte((byte) 1);
				parcel.writeString(s);
			}
			else {
				parcel.writeByte((byte) 0);
			}
		}
		return parcel;
	}
	
	public void restoreParcel(Parcel parcel) {
		for (int i = 0;i < data.length;i++) {
			if (parcel.readByte() == 1) {
				data[i] = parcel.readString();
			}
			else {
				data[i] = null;
			}
		}
		Assert.assertEquals(title, data[0]);
		Assert.assertEquals(description, data[1]);
		Assert.assertEquals(link, data[2]);
		Assert.assertEquals(source, data[3]);
		Assert.assertEquals(sourceTitle, data[4]);
		Assert.assertEquals(pubDate, data[5]);
	}
	
	public int getParceledLength() {
		return data.length; // total number of descriptors that are packaged in the parcel
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
		arg0 = getParcel();
	}
}
