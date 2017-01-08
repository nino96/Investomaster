package com.example.android.investomaster;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.TextView;

import com.example.android.investomaster.utilities.NetworkUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import au.com.bytecode.opencsv.CSVReader;

public class StockDetail extends AppCompatActivity{

    private long rowId;
    private String stock_name;
    private String symbol;
    private String change;
    private String price;
    private int rise_fall;
    private String changePercent;

    //END DATE WHICH IS ALWAYS THE CURRENT DATE
    private String endDate;

    private InputStream in;

    //ImageView for charts not provided by google
//    private WebView mChartImage;

    //LineChart for graph
    private LineChart mChart;

    class CustomObject {
        String value1;
        String value2;

        CustomObject(String v1, String v2) {
            value1 = v1;
            value2 = v2;
        }

        String getValue1(){
            return value1;
        }
        String getValue2(){
            return value2;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TO REQUEST NOT TO USE TITLE
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_stock_detail);


        //webview if google doesn't have historical data
//        mChartImage = (WebView)findViewById(R.id.chart_image);

        //initialize line chart variable
        mChart = (LineChart)findViewById(R.id.lineChart);
        mChart.setNoDataText("No network or invalid date chosen");




        rowId = (Long) getIntent().getExtras().get("ID");

        try {
            SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
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
            db.close();


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

        //SET WEBVIEW URL (STILL INVISIBLE)!!!!
//        mChartImage.loadUrl("http://chart.finance.yahoo.com/c/1y/"+symbol);


        //CONSTRUCT CURRENT DATE,WHICH WILL BE END DATE FOR GRAPH

        String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        int mYear,mMonth,mDay;
        String month;

        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        month = MONTHS[mMonth];

        //END DATE IS ALWAYS THE CURRENT DATE!
        endDate = month+" "+mDay+", "+mYear;


        //default start date
        if(isNetworkAvailable()) {

            getHistoricalData("Jan 01, 2016");
        }
        else{
            mChart.clear();
            mChart.invalidate();
            mChart.setVisibility(View.VISIBLE);
        }
    }

    public class HistoricalDataQueryTask extends AsyncTask<URL,Void,ArrayList<CustomObject>>{

        @Override
        protected ArrayList<CustomObject> doInBackground(URL... urls) {


            URL url = urls[0];
            //String response = null;
            InputStream in = null;
            ArrayList<CustomObject> dataObjects = null;
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);
                CSVReader csvReader = new CSVReader(reader);
                String[] nextLine;

                dataObjects = new ArrayList<CustomObject>();

                //skip first row since it has column names and not actual values
                nextLine = csvReader.readNext();
                while (nextLine!=null && (nextLine = csvReader.readNext()) != null) {

                    //nextline[0] is date and nextLine[1] is opening price,ignoring max,min,and close price and market cap
                    CustomObject c = new CustomObject(nextLine[0],nextLine[1]);

                    dataObjects.add(c);
                }
                //Log.v("CSVReader", yAxis.get(2));

            }catch (IOException e){
                e.printStackTrace();
            }

            return dataObjects;

        }

        @Override
        protected void onPostExecute(ArrayList<CustomObject> dataObjects) {

            /*TextView tv = (TextView)findViewById(R.id.tv_historical_response);
            tv.setText(s);*/
            //Log.v("HISTORICAL",strings.get(2));
            ArrayList<Entry> yVals = new ArrayList<Entry>();        //y-axis values as (float,count) combination
            ArrayList<String> xVals = new ArrayList<String>();      //x-axis values as string

            int count = 0;

            //if response empty(either because google did not respond or user chose very small date window) then
            // return from method and make mChart INVISIBLE
            if(dataObjects.size()<1){
//                mChartImage.setVisibility(View.VISIBLE);
                //mChart.setVisibility(View.GONE);
                mChart.clear();

                //mChart.setNoDataText("No network or invalid date chosen");

                return;
            }

            //REVERSE SINCE DATES START FROM MOST RECENT IN CSV FILE,WHEREAS WE WANT IT TO START FROM OLDEST!!!!!
            Collections.reverse(dataObjects);


            for(CustomObject o:dataObjects){

                xVals.add(o.getValue1());
                Log.v("ENTRY",o.getValue2());

                try{
                    Float.parseFloat(o.getValue2());

                }catch(NumberFormatException e){
                    break;
                }
                yVals.add(new Entry(Float.parseFloat(o.getValue2()),count++));
            }


            LineDataSet set1;

            // create a dataset and give it a type
            set1 = new LineDataSet(yVals, "DataSet 1");
            set1.setFillAlpha(110);
            // set1.setFillColor(Color.RED);

            // set the line to be drawn like this "- - - - - -"
            // set1.enableDashedLine(10f, 5f, 0f);
            // set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(0.1f);
            set1.setCircleRadius(3f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setDrawFilled(true);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xVals, dataSets);

            // set data
            mChart.setData(data);
            mChart.setDescription("Stock History");
            mChart.setNoDataText("No network or invalid date chosen");
            mChart.setScaleEnabled(true);
            mChart.setDragEnabled(true);

            //make webview invisible and chart visible if google has data
//            mChartImage.setVisibility(View.GONE);
            mChart.setVisibility(View.VISIBLE);

            mChart.invalidate();


        }
    }


    private void getHistoricalData(String startDate){
        //String startDate = "Jan 01, 2012";


        URL url = NetworkUtils.buildHistoricalUrl(symbol,startDate,endDate);

        //not using NetworkUtils getResponseFromHttpUrl method since requirement different

        new HistoricalDataQueryTask().execute(url);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stock_detail,menu);

        try {
            SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            MenuItem item = menu.findItem(R.id.action_check);
            Cursor cursor = db.query("nasdaq", new String[]{"favorite"}, "_id = ?", new String[]{Long.toString(rowId)}, null, null, null);
            cursor.moveToFirst();
            boolean isChecked;

            if(cursor.getInt(0)==0){
                isChecked = false;
            }
            else
            {
                isChecked = true;
            }

            item.setChecked(isChecked);

            cursor.close();
            db.close();

        }catch(SQLiteException e){
            e.printStackTrace();
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        int mYear,mMonth,mDay;
        String month;

        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        //since String needed in query url
        month = MONTHS[mMonth];


        if(itemId == R.id.action_start){
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year,
                                              int monthOfYear, int dayOfMonth) {

                            //CONSTRUCT START DATE AND THEN CALL HISTORICAL DATA FUNCTION
                            String startDate = MONTHS[monthOfYear]+" "+dayOfMonth+", "+year;
                            if(isNetworkAvailable()) {
                                getHistoricalData(startDate);
                            }
                            else{
                                mChart.clear();
                                mChart.invalidate();
                                mChart.setVisibility(View.VISIBLE);
                            }

                        }
                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }
        if(itemId == R.id.action_check){

            try {
                SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
                SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

                Cursor cursor = db.query("nasdaq",new String[]{"favorite"},"_id = ?",new String[]{Long.toString(rowId)},null,null,null);

                cursor.moveToFirst();
                //if not favorited then favorite else unfavorite
                if(cursor.getInt(0)==0){
                    ContentValues favorite = new ContentValues();
                    favorite.put("favorite",1);

                    item.setChecked(true);
                    db.update("nasdaq",favorite,"_id = ?",new String[]{Long.toString(rowId)});
                }
                else{
                    ContentValues favorite = new ContentValues();
                    favorite.put("favorite",0);
                    item.setChecked(false);
                    db.update("nasdaq",favorite,"_id = ?",new String[]{Long.toString(rowId)});
                }

                cursor.close();
                db.close();

            }catch(SQLiteException e){
                e.printStackTrace();
            }
        }

        return super.onOptionsItemSelected(item);
    }



    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
