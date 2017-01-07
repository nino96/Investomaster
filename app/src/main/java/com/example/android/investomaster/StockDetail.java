package com.example.android.investomaster;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.android.investomaster.utilities.NetworkUtils;

import java.io.IOException;
import java.net.URL;

public class StockDetail extends AppCompatActivity {

    private long rowId;
    private String stock_name;
    private String symbol;
    private String change;
    private String price;
    private int rise_fall;
    private String changePercent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);


        rowId = (Long) getIntent().getExtras().get("ID");

        try {
            SQLiteOpenHelper stockDatabaseHelper = new StockDatabaseHelper(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            Cursor cursor = db.query("nasdaq",new String[]{"name","symbol","price","change","change_dir","change_percent"},"_id = ?",new String[]{Long.toString(rowId)},null,null,null);

            cursor.moveToFirst();
            stock_name = cursor.getString(0);
            symbol = cursor.getString(1);
            price = cursor.getString(2);
            change = cursor.getString(3);
            rise_fall = cursor.getInt(4);
            changePercent = cursor.getString(5);

            //cursor.close();
            //db.close();


        }catch (SQLiteException e){
            e.printStackTrace();
        }finally {

        }

        TextView tv = (TextView)findViewById(R.id.tv_detailed_name);
        tv.setText("Listed Name : "+stock_name);

        tv = (TextView)findViewById(R.id.tv_detailed_symbol);
        tv.setText("Symbol : "+symbol);

        tv = (TextView)findViewById(R.id.tv_detailed_price);
        tv.setText("Last Traded Price : "+price+"$");

        tv = (TextView)findViewById(R.id.tv_detailed_change);
        tv.setText("Change : "+change+" ("+changePercent+"%)");
        if(rise_fall==0){
            tv.setTextColor(Color.RED);
        }
        else{
            tv.setTextColor(Color.GREEN);
        }

        getHistoricalData();
    }


    private void getHistoricalData(){
        String startDate = "Jan 01, 2012";
        String endDate = "Aug 08, 2012";

        URL url = NetworkUtils.buildHistoricalUrl(symbol,startDate,endDate);

        //not using NetworkUtils getResponseFromHttpUrl method since requirement different

        try{
            String response = NetworkUtils.getResponseFromHttpUrl(url);
            TextView tv = (TextView)findViewById(R.id.tv_historical_response);
            tv.setText(response);
        }catch (IOException e){
            e.printStackTrace();
        }


        /*try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
        }catch(IOException e)
        {
            e.printStackTrace();
        }*/

    }
}
