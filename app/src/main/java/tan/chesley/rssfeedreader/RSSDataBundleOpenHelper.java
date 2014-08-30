package tan.chesley.rssfeedreader;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class RSSDataBundleOpenHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 5;
    private static final String RSS_DATA_TABLE_NAME = "rssdata";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PREVIEW_DESCRIPTION = "previewDescription";
    private static final String KEY_LINK = "link";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_SOURCE_TITLE = "sourceTitle";
    private static final String KEY_PUB_DATE = "pubDate";
    private static final String KEY_AGE = "age";
    private static final String KEY_READ = "read";
    private static final String KEY_DESCRIPTION_SANITIZED = "descriptionSanitized";

    private static final String RSS_DATA_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
        RSS_DATA_TABLE_NAME + " (" +
        KEY_UUID + " TEXT, " +
        KEY_TITLE + " TEXT, " +
        KEY_DESCRIPTION + " TEXT, " +
        KEY_PREVIEW_DESCRIPTION + " TEXT, " +
        KEY_LINK + " TEXT, " +
        KEY_SOURCE + " TEXT, " +
        KEY_SOURCE_TITLE + " TEXT, " +
        KEY_PUB_DATE + " TEXT, " +
        KEY_AGE + " INTEGER, " +
        KEY_READ + " INTEGER, " +
        KEY_DESCRIPTION_SANITIZED + " INTEGER" +
        ");";

    private static final String NEW_RSS_DATA_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
        RSS_DATA_TABLE_NAME + " (" +
        KEY_UUID + " TEXT, " +
        KEY_TITLE + " TEXT, " +
        KEY_DESCRIPTION + " TEXT, " +
        KEY_PREVIEW_DESCRIPTION + " TEXT, " +
        KEY_LINK + " TEXT, " +
        KEY_SOURCE + " TEXT, " +
        KEY_SOURCE_TITLE + " TEXT, " +
        KEY_PUB_DATE + " TEXT, " +
        KEY_AGE + " INTEGER, " +
        KEY_READ + " INTEGER, " +
        KEY_DESCRIPTION_SANITIZED + " INTEGER" +
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
            db.execSQL(String.format("INSERT INTO %s VALUES (?, ?, ?, ?, ?, ?, ?, ?, %s, %s, %s);",
                                     RSS_DATA_TABLE_NAME,
                                     rdBundle.getAge(),
                                     rdBundle.isRead() ? 1 : 0,
                                     rdBundle.isDescriptionSanitized() ? 1 : 0),
                       new String[] {
                           rdBundle.getId(),
                           rdBundle.getTitle(),
                           rdBundle.getRawDescription(),
                           rdBundle.getRawPreviewDescription(),
                           rdBundle.getLink(),
                           rdBundle.getSource(),
                           rdBundle.getSourceTitle(),
                           rdBundle.getPubDate()
                       });
        }
        else {
            Log.e("RSSDataBundleOpenHelper", "Duplicate entry found. Skipping.");
        }
        db.close();
    }


    public void addBundles(ArrayList<RSSDataBundle> rdBundles) {
        SQLiteDatabase db = getWritableDatabase();
        for (RSSDataBundle rdBundle : rdBundles) {
            if (isUnique(db, rdBundle.getTitle())) {
                db.execSQL(String.format("INSERT INTO %s VALUES (?, ?, ?, ?, ?, ?, ?, ?, %s, %s, %s);",
                                         RSS_DATA_TABLE_NAME,
                                         rdBundle.getAge(),
                                         rdBundle.isRead() ? 1 : 0,
                                         rdBundle.isDescriptionSanitized() ? 1 : 0),
                           new String[] {
                               rdBundle.getId(),
                               rdBundle.getTitle(),
                               rdBundle.getRawDescription(),
                               rdBundle.getRawPreviewDescription(),
                               rdBundle.getLink(),
                               rdBundle.getSource(),
                               rdBundle.getSourceTitle(),
                               rdBundle.getPubDate()
                           });
            }
            else {
                Log.e("RSSDataBundleOpenHelper", "Duplicate entry found. Skipping.");
            }
        }
        db.close();
    }

    public boolean isUnique(SQLiteDatabase db, String title) {
        boolean retBool = true;
        title = title.replaceAll("\\s", "");
        Cursor cursor = db.rawQuery(String.format("SELECT %s FROM %s", KEY_TITLE, RSS_DATA_TABLE_NAME), null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (title.equals(cursor.getString(0).replaceAll("\\s", ""))) {
                retBool = false;
                break;
            }
            cursor.moveToNext();
        }
        cursor.close();
        return retBool;
    }

    public ArrayList<RSSDataBundle> getBundles() {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<RSSDataBundle> bundles = new ArrayList<RSSDataBundle>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + RSS_DATA_TABLE_NAME, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            RSSDataBundle rdBundle = new RSSDataBundle(cursor.getString(0));
            rdBundle.setTitle(cursor.getString(1));
            rdBundle.setDescription(cursor.getString(2));
            rdBundle.setPreviewDescription(cursor.getString(3));
            rdBundle.setLink(cursor.getString(4));
            rdBundle.setSource(cursor.getString(5));
            rdBundle.setSourceTitle(cursor.getString(6));
            rdBundle.setPubDate(cursor.getString(7));
            rdBundle.setAge(cursor.getLong(8));
            rdBundle.setRead(cursor.getInt(9) != 0);
            rdBundle.setDescriptionSanitized(cursor.getInt(10) != 0);
            bundles.add(rdBundle);
            cursor.moveToNext();
        }
        cursor.close();
        db.close();
        return bundles;
    }

    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + RSS_DATA_TABLE_NAME + ";");
        db.close();
    }

    public void updateData(RSSDataBundle rdBundle) {
        SQLiteDatabase db = getWritableDatabase();
        Log.e("Updating database data for article: ", rdBundle.getTitle());
        db.execSQL(String.format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?;",
                                                    RSS_DATA_TABLE_NAME,
                                                    KEY_TITLE,
                                                    KEY_DESCRIPTION,
                                                    KEY_PREVIEW_DESCRIPTION,
                                                    KEY_LINK,
                                                    KEY_SOURCE,
                                                    KEY_SOURCE_TITLE,
                                                    KEY_PUB_DATE,
                                                    KEY_AGE,
                                                    KEY_READ,
                                                    KEY_DESCRIPTION_SANITIZED,
                                                    KEY_UUID), new String[] {
                                                                            rdBundle.getTitle(),
                                                                            rdBundle.getRawDescription(),
                                                                            rdBundle.getRawPreviewDescription(),
                                                                            rdBundle.getLink(),
                                                                            rdBundle.getSource(),
                                                                            rdBundle.getSourceTitle(),
                                                                            rdBundle.getPubDate(),
                                                                            Long.toString(rdBundle.getAge()),
                                                                            rdBundle.isRead() ? "1" : "0",
                                                                            rdBundle.isDescriptionSanitized() ? "1" : "0",
                                                                            rdBundle.getId()
                                                                            });
        db.close();
    }

    public void updateRead(RSSDataBundle rdBundle) {
        SQLiteDatabase db = getWritableDatabase();
        Log.e("Updating read for article: ", rdBundle.getTitle());
        db.execSQL(String.format("UPDATE %s SET %s=? WHERE %s=?;",
                                 RSS_DATA_TABLE_NAME,
                                 KEY_READ,
                                 KEY_UUID), new String[] {
                                                          rdBundle.isRead() ? "1" : "0",
                                                          rdBundle.getId()
                                            });
        db.close();
    }

    public void updateDescriptionSanitized(RSSDataBundle rdBundle) {
        SQLiteDatabase db = getWritableDatabase();
        Log.e("Updating description sanitized for article: ", rdBundle.getTitle());
        db.execSQL(String.format("UPDATE %s SET %s=?, %s=?, %s=? WHERE %s=?;",
                                 RSS_DATA_TABLE_NAME,
                                 KEY_DESCRIPTION,
                                 KEY_PREVIEW_DESCRIPTION,
                                 KEY_DESCRIPTION_SANITIZED,
                                 KEY_UUID), new String[] {
            rdBundle.getRawDescription(),
            rdBundle.getRawPreviewDescription(),
            rdBundle.isDescriptionSanitized() ? "1" : "0",
            rdBundle.getId()
        });
        db.close();
    }

    public void constrainDatabaseSize(int maxSize) {
        long currentSize = DatabaseUtils.queryNumEntries(getReadableDatabase(), RSS_DATA_TABLE_NAME);
        Log.e("Database size before: ", Long.toString(currentSize));
        if (currentSize > maxSize) {
            String ALTER_TBL = "DELETE FROM " + RSS_DATA_TABLE_NAME +
                " WHERE rowid IN (SELECT rowid FROM " + RSS_DATA_TABLE_NAME + " ORDER BY rowid LIMIT " + (currentSize - maxSize) + ");";
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(ALTER_TBL);
            db.close();
        }
        Log.e("Database size after: ", Long.toString(DatabaseUtils.queryNumEntries(getReadableDatabase(), RSS_DATA_TABLE_NAME)));
    }
}
