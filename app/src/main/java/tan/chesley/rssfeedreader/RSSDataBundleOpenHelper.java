package tan.chesley.rssfeedreader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class RSSDataBundleOpenHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 1;
    private static final String RSS_DATA_TABLE_NAME = "rssdata";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_LINK = "link";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_SOURCE_TITLE = "sourceTitle";
    private static final String KEY_PUB_DATE = "pubDate";

    private static final String RSS_DATA_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
        RSS_DATA_TABLE_NAME + " (" +
        KEY_UUID + " TEXT, " +
        KEY_TITLE + " TEXT, " +
        KEY_DESCRIPTION + " TEXT, " +
        KEY_LINK + " TEXT, " +
        KEY_SOURCE + " TEXT, " +
        KEY_SOURCE_TITLE + " TEXT, " +
        KEY_PUB_DATE + " TEXT" +
        ");";

    private static final String NEW_RSS_DATA_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
        RSS_DATA_TABLE_NAME + " (" +
        KEY_UUID + " TEXT, " +
        KEY_TITLE + " TEXT, " +
        KEY_DESCRIPTION + " TEXT, " +
        KEY_LINK + " TEXT, " +
        KEY_SOURCE + " TEXT, " +
        KEY_SOURCE_TITLE + " TEXT, " +
        KEY_PUB_DATE + " TEXT" +
        ");";

    public RSSDataBundleOpenHelper(Context context) {
        super(context, RSS_DATA_TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate (SQLiteDatabase db) {
        db.execSQL(RSS_DATA_TABLE_CREATE);
    }

    @Override
    public void onUpgrade (SQLiteDatabase db, int i, int i2) {
        Log.e("RSSDataBundleOpenHelper", "Conducting SQLite database upgrade");
        // Credits to Pentium10 at StackOverflow
        db.execSQL(RSS_DATA_TABLE_CREATE);
        ArrayList<String> cols = getColumns(db, RSS_DATA_TABLE_NAME);
        db.execSQL("ALTER TABLE " + RSS_DATA_TABLE_NAME + " RENAME TO temp_" + RSS_DATA_TABLE_NAME + ";");
        db.execSQL(NEW_RSS_DATA_TABLE_CREATE);
        cols.retainAll(getColumns(db, RSS_DATA_TABLE_NAME));
        String preservedCols = TextUtils.join(",", cols);
        db.execSQL(String.format(
            "INSERT INTO %s (%s) SELECT %s FROM temp_%s;",
            RSS_DATA_TABLE_NAME, preservedCols, preservedCols, RSS_DATA_TABLE_NAME));
        db.execSQL("DROP TABLE temp_" + RSS_DATA_TABLE_NAME + ";");
    }

    public ArrayList<String> getColumns(SQLiteDatabase db, String dbName) {
        Cursor cursor = db.query(dbName, null, null, null, null, null, null);
        ArrayList<String> ret = new ArrayList<String>(Arrays.asList(cursor.getColumnNames()));
        cursor.close();
        return ret;
    }

    public void addBundle(RSSDataBundle rdBundle) {
        SQLiteDatabase db = getWritableDatabase();
        if (isUnique(db, rdBundle.getTitle())) {
            db.execSQL(String.format("INSERT INTO %s VALUES (\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\");",
                                     RSS_DATA_TABLE_NAME, rdBundle.getId(), rdBundle.getTitle(), rdBundle.getDescription(), rdBundle.getLink(), rdBundle.getSource(), rdBundle.getSourceTitle(), rdBundle.getPubDate()));
        }
        else {
            Log.e("RSSDataBundleOpenHelper", "Duplicate entry found. Skipping.");
        }
    }


    public void addBundles(ArrayList<RSSDataBundle> rdBundles) {
        SQLiteDatabase db = getWritableDatabase();
        for (RSSDataBundle rdBundle : rdBundles) {
            if (isUnique(db, rdBundle.getId())) {
                db.execSQL(String.format("INSERT INTO %s VALUES (\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\");",
                                         RSS_DATA_TABLE_NAME, rdBundle.getId(), rdBundle.getTitle(), rdBundle.getDescription(), rdBundle.getLink(), rdBundle.getSource(), rdBundle.getSourceTitle(), rdBundle.getPubDate()));
            }
            else {
                Log.e("RSSDataBundleOpenHelper", "Duplicate entry found. Skipping.");
            }
        }
    }

    public boolean isUnique(SQLiteDatabase db, String title) {
        Cursor cursor = db.rawQuery(String.format("SELECT %s FROM %s WHERE %s = \"%s\"", KEY_TITLE, RSS_DATA_TABLE_NAME, KEY_TITLE, title), null);
        boolean retBool = cursor.getCount() <= 1;
        cursor.close();
        return retBool;
    }

    public ArrayList<RSSDataBundle> getBundles() {
        ArrayList<RSSDataBundle> bundles = new ArrayList<RSSDataBundle>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + RSS_DATA_TABLE_NAME, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            RSSDataBundle rdBundle = new RSSDataBundle(cursor.getString(0));
            rdBundle.setTitle(cursor.getString(1));
            rdBundle.setDescription(cursor.getString(2));
            rdBundle.setLink(cursor.getString(3));
            rdBundle.setSource(cursor.getString(4));
            rdBundle.setSourceTitle(cursor.getString(5));
            rdBundle.setPubDate(cursor.getString(6));
            bundles.add(rdBundle);
            cursor.moveToNext();
        }
        cursor.close();
        return bundles;
    }

    public void clearAllData() {
        getWritableDatabase().execSQL("DELETE FROM " + RSS_DATA_TABLE_NAME + ";");
    }
}
