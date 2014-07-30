package tan.chesley.rssfeedreader;

import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("serial")
public class MyMap extends HashMap<String, RSSDataBundle> implements Parcelable {

	public MyMap() {
		super();
	}
	
	public MyMap(int n) {
		super(n);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeInt(size());
		for (String key : keySet()) {
			out.writeString(key);
			out.appendFrom(get(key).getParcel(), 0, get(key).getParceledLength());
		}
	}

	public final Parcelable.Creator<MyMap> CREATOR = new Parcelable.Creator<MyMap>() {
		public MyMap createFromParcel(Parcel in) {
			return new MyMap(in);
		}

		public MyMap[] newArray(int size) {
			return new MyMap[size];
		}
	};

	private MyMap(Parcel in) {
		super();
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			String key = in.readString();
			RSSDataBundle value = new RSSDataBundle();
			value.restoreParcel(in);
			put(key, value);
		}
	}
}