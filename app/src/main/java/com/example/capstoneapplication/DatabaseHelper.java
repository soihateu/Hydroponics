package com.example.capstoneapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PresetDB";
    private static final int DATABASE_VERSION = 1;

    private static final String DB_TABLE = "Database_Table";

    // DB Columns
    private static final String COL1 = "ID";
    private static final String COL2 = "NAME";
    private static final String COL3 = "LIGHT";
    private static final String COL4 = "HUMIDITY";
    private static final String COL5 = "TEMPERATURE";

    private static final String CREATE_TABLE = "CREATE TABLE " + DB_TABLE + "(" +
            COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COL2 + " TEXT," +
            COL3 + " FLOAT," +
            COL4 + " FLOAT," +
            COL5 + " FLOAT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
    }

    // Add entry to database
    public void insertData(String name, float light, float humidity, float temperature) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL2, name);
        values.put(COL3, light);
        values.put(COL4, humidity);
        values.put(COL5, temperature);
        db.insert(DB_TABLE, null, values);
    }

    public Cursor viewData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + DB_TABLE;
        Cursor cursor = db.rawQuery(query, null);

        return cursor;
    }
}
