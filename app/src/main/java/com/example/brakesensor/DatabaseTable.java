package com.example.brakesensor;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DatabaseTable {
    private static final String TAG = DatabaseTable.class.getSimpleName();

    //The columns we'll include in the dictionary table
    public static final String COL_TIME = "TIME";
    public static final String COL_LF = "LF";
    public static final String COL_RF = "RF";
    public static final String COL_LR = "LR";
    public static final String COL_RR = "RR";
    public static final String COL_LC = "LC";
    public static final String COL_RC = "RC";

    private static final String DATABASE_NAME = "DICTIONARY";
    private static final String FTS_VIRTUAL_TABLE = "FTS";
    private static final int DATABASE_VERSION = 1;

    private final DatabaseOpenHelper databaseOpenHelper;

    public DatabaseTable(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context);
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper {
        private final Context helperContext;
        private SQLiteDatabase mDatabase;

        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        COL_TIME + ", " +
                        COL_LF + ", " + COL_RF + ", " + COL_LR + ", " + COL_RR + ", " + COL_LC + ", " + COL_RC + ")";

        DatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            helperContext = context;

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }

        private void loadDictionary() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadWords();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        private void loadWords() throws IOException {
            final Resources resources = helperContext.getResources();
            InputStream inputStream = resources.openRawResource(R.raw.saved_sensor_data);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] strings = TextUtils.split(line, "-");
                    if (strings.length < 2) continue;
                    long id = addWord(strings[0].trim(), strings[1].trim(), strings[2].trim(), strings[3].trim(), strings[4].trim(), strings[5].trim(), strings[6].trim());
                    if (id < 0) {
                        Log.e(TAG, "unable to add word: " + strings[0].trim());
                    }
                }
            } finally {
                reader.close();
            }
        }

        public long addWord(String time, String lf, String rf, String lr, String rr, String lc, String rc) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(COL_TIME, time);
            initialValues.put(COL_LC, lc);
            initialValues.put(COL_LF, lf);
            initialValues.put(COL_LR, lr);
            initialValues.put(COL_RC, rc);
            initialValues.put(COL_RF, rf);
            initialValues.put(COL_RR, rr);

            return database.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

    }
}
