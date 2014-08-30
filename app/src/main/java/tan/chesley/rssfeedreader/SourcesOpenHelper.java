package tan.chesley.rssfeedreader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class SourcesOpenHelper extends SQLiteOpenHelper{
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;
    private static final int DATABASE_VERSION = 5;
    private static final String SOURCES_TABLE_NAME = "sources";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_ENABLED = "enabled";
    private static final String SOURCES_TABLE_CREATE =
        "CREATE TABLE IF NOT EXISTS " + SOURCES_TABLE_NAME + " (" +
        KEY_SOURCE + " TEXT, " +
        KEY_ENABLED + " INTEGER);";
    private static final String NEW_SOURCES_TABLE_CREATE =
        "CREATE TABLE IF NOT EXISTS " + SOURCES_TABLE_NAME + " (" +
        KEY_SOURCE + " TEXT, " +
        KEY_ENABLED + " INTEGER);";

    public SourcesOpenHelper(Context context) {
        super(context, SOURCES_TABLE_NAME, null, DATABASE_VERSION);
        /*
        Log.e("SourcesOpenHelper", "Regenerating defaults");
        clearAllSources();
        addSource("http://rss.cnn.com/rss/cnn_world.rss", 1);
        addSource("http://rss.cnn.com/rss/cnn_tech.rss", 1);
        addSource("http://news.feedzilla.com/en_us/headlines/top-news/world-news.rss", 1);
        addSource("http://news.feedzilla.com/en_us/headlines/science/top-stories.rss", 1);
        addSource("http://news.feedzilla.com/en_us/headlines/technology/top-stories.rss", 1);
        addSource("http://news.feedzilla.com/en_us/headlines/programming/top-stories.rss", 1);
        addSource("http://www.reddit.com/.rss", 1);
        */
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SOURCES_TABLE_CREATE);
    }

    @Override
    public void onUpgrade (SQLiteDatabase db, int i, int i2) {
        Log.e("SourcesOpenHelper", "Conducting SQLite database upgrade");
        // Credits to Pentium10 at StackOverflow
        db.execSQL(SOURCES_TABLE_CREATE);
        ArrayList<String> cols = getColumns(db, SOURCES_TABLE_NAME);
        db.execSQL("ALTER TABLE " + SOURCES_TABLE_NAME + " RENAME TO temp_" + SOURCES_TABLE_NAME + ";");
        db.execSQL(NEW_SOURCES_TABLE_CREATE);
        cols.retainAll(getColumns(db, SOURCES_TABLE_NAME));
        String preservedCols = TextUtils.join(",", cols);
        db.execSQL(String.format(
            "INSERT INTO %s (%s) SELECT %s FROM temp_%s;",
            SOURCES_TABLE_NAME, preservedCols, preservedCols, SOURCES_TABLE_NAME));
        db.execSQL("DROP TABLE temp_" + SOURCES_TABLE_NAME + ";");
    }

    public ArrayList<String> getColumns(SQLiteDatabase db, String dbName) {
        Cursor cursor = db.query(dbName, null, null, null, null, null, null);
        ArrayList<String> ret = new ArrayList<String>(Arrays.asList(cursor.getColumnNames()));
        cursor.close();
        return ret;
    }

    public void addSource(String source, int enabled) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO " + SOURCES_TABLE_NAME + " VALUES (\"" + source + "\", " + enabled + ");");
        db.close();
    }

    public void deleteSource(String source) {
        Log.e("SourcesOpenHelper", "Deleting source " + source);
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + SOURCES_TABLE_NAME + " WHERE rowid = (SELECT rowid FROM " + SOURCES_TABLE_NAME + " WHERE " + KEY_SOURCE + " = \"" + source + "\" LIMIT 1);");
        db.close();
    }

    public void setEnabled(String source, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        int bool = enabled ? 1 : 0;
        db.execSQL("UPDATE " + SOURCES_TABLE_NAME + " SET " + KEY_ENABLED + " = " + bool + " WHERE " + KEY_SOURCE + " = \"" + source + "\";");
        db.close();
    }

    public boolean isEnabled(String source) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_ENABLED + " FROM " + SOURCES_TABLE_NAME + " WHERE " + KEY_SOURCE + " = \"" + source + "\";", null);
        cursor.moveToFirst();
        boolean enabled = cursor.getInt(0) != 0;
        cursor.close();
        db.close();
        return enabled;
    }

    public void clearAllSources() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + SOURCES_TABLE_NAME + ";");
        db.close();
    }

    public ArrayList<String> getSourcesArrayList() {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<String> sources = new ArrayList<String>();
        Cursor cursor = db.rawQuery("SELECT " + KEY_SOURCE + " FROM " + SOURCES_TABLE_NAME + ";", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            sources.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        db.close();
        return sources;
    }

    public String[] getSources() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_SOURCE + " FROM " + SOURCES_TABLE_NAME + ";", null);
        String[] sources = new String[cursor.getCount()];
        int index = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            sources[index++] = cursor.getString(0);
            cursor.moveToNext();
        }
        cursor.close();
        db.close();
        return sources;
    }

    public String[] getEnabledSources() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_SOURCE + " FROM " + SOURCES_TABLE_NAME + " WHERE " + KEY_ENABLED + " = 1;", null);
        String[] sources = new String[cursor.getCount()];
        int index = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            sources[index++] = cursor.getString(0);
            cursor.moveToNext();
        }
        cursor.close();
        db.close();
        return sources;
    }
}
