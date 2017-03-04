package com.example.android.investomaster;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Niyam on 06/01/2017.
 */

public class StockDatabaseHelper extends SQLiteOpenHelper {

    private static StockDatabaseHelper sInstance;

    private static final String DB_NAME="Stocks";
    private static final int DB_VERSION=1;

    String mCSVfile;
    AssetManager manager;

    public static synchronized StockDatabaseHelper getInstance(Context context) {


        if (sInstance == null) {
            sInstance = new StockDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private StockDatabaseHelper(Context context){
        super(context,DB_NAME,null,DB_VERSION);

        manager = context.getAssets();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE nasdaq (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "symbol TEXT, "
                + "name TEXT, "
                + "price TEXT, "
                + "change_dir INTEGER, "
                + "change TEXT, "
                + "change_percent TEXT, "
                + "notif INTEGER DEFAULT 0, "
                + "favorite INTEGER DEFAULT 0, "
                + "lower INTEGER DEFAULT -1, "
                + "upper INTEGER DEFAULT -1);");
        mCSVfile = "NASDAQ.csv";

        InputStream inStream = null;
        try {
            inStream = manager.open(mCSVfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader buffer = new BufferedReader(new InputStreamReader(inStream));

        String line = "";
        db.beginTransaction();
        try {
            //skip first line,since first line are column names
            line = buffer.readLine();
            while ((line = buffer.readLine()) != null) {
                String[] colums = line.split(",");

                Log.v("COLUMNS",colums[0]);
                ContentValues cv = new ContentValues();
                cv.put("symbol", colums[0].trim().replaceAll("^\"|\"$", ""));
                cv.put("name", colums[1].trim().replaceAll("^\"|\"$", ""));
                db.insert("nasdaq", null, cv);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        db.setTransactionSuccessful();
        db.endTransaction();



    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldversion, int newversion) {

    }
}
