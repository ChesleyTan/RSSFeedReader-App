package tan.chesley.rssfeedreader;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

public class RSSDataBundle implements Parcelable{
	private String title, description, link; // Required descriptors
	private String mediaURL, pubDate; // Optional descriptors
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
	public String getMediaURL() {
		return mediaURL;
	}
	public RSSDataBundle setMediaURL(String mediaURL) {
		this.mediaURL = mediaURL;
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
		parcel.writeString(title);
		parcel.writeString(description);
		parcel.writeString(link);
		parcel.writeString(mediaURL);
		parcel.writeString(pubDate);
		return parcel;
	}
	
	public void restoreParcel(Parcel parcel) {
		title = parcel.readString();
		description = parcel.readString();
		link = parcel.readString();
		mediaURL = parcel.readString();
		pubDate = parcel.readString();
	}
	
	public int getParceledLength() {
		return 5; // total number of descriptors that are packaged in the parcel
	}

	public final Parcelable.Creator<RSSDataBundle> CREATOR = new Parcelable.Creator<RSSDataBundle>() {
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
