package com.bichoapp.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "config.db";
    private static final int DB_VERSION = 1;

    // Mesma estrutura do sistema atual: config(url, impressora)
    private static final String TABLE = "config";
    private static final String COL_URL = "url";
    private static final String COL_PRINTER = "impressora";

    private static final String DEFAULT_URL = "https://bichoapp.com.br";
    private static final String DEFAULT_PRINTER = "";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE +
                " (" + COL_URL + " VARCHAR, " + COL_PRINTER + " VARCHAR)");
        // Insere valores padrão
        ContentValues cv = new ContentValues();
        cv.put(COL_URL, DEFAULT_URL);
        cv.put(COL_PRINTER, DEFAULT_PRINTER);
        db.insert(TABLE, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Preserva dados existentes em upgrades
    }

    public String getUrl() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, new String[]{COL_URL}, null, null, null, null, null);
        if (c.moveToFirst()) {
            String url = c.getString(0);
            c.close();
            return (url != null && !url.isEmpty()) ? url : DEFAULT_URL;
        }
        c.close();
        return DEFAULT_URL;
    }

    public String getPrinter() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, new String[]{COL_PRINTER}, null, null, null, null, null);
        if (c.moveToFirst()) {
            String printer = c.getString(0);
            c.close();
            return (printer != null) ? printer : DEFAULT_PRINTER;
        }
        c.close();
        return DEFAULT_PRINTER;
    }

    public void setUrl(String url) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_URL, url);
        int rows = db.update(TABLE, cv, null, null);
        if (rows == 0) {
            cv.put(COL_PRINTER, DEFAULT_PRINTER);
            db.insert(TABLE, null, cv);
        }
    }

    public void setPrinter(String printer) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PRINTER, printer);
        int rows = db.update(TABLE, cv, null, null);
        if (rows == 0) {
            cv.put(COL_URL, DEFAULT_URL);
            db.insert(TABLE, null, cv);
        }
    }
}
