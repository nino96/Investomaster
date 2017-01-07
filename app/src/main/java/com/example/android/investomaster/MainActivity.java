package com.example.android.investomaster;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.example.android.investomaster.utilities.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView mJsonResponse;
    public SimpleCursorAdapter mAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mJsonResponse = (TextView)findViewById(R.id.tv_json_response);


        //CURRENTLY mAdapter gets defined in displayListView() and gets referenced in repeatTask()
        //therefore call to displayListView() call before repeatTask()

        displayListView();
        repeatTask();
        //displayListView();

    }

    public class StockQueryTask extends AsyncTask<ArrayList<URL>,Void,ArrayList<String>>{
        @Override
        protected ArrayList<String> doInBackground(ArrayList<URL>... urls) {
            ArrayList<URL> searchqueries = urls[0];
            ArrayList<String> response = new ArrayList<String>();

            for (URL query : searchqueries) {
                try {

                    response.add(NetworkUtils.getResponseFromHttpUrl(query));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            return response;

        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            //LinearLayout linear = (LinearLayout)findViewById(R.id.linear_layout_main);
            //linear.removeAllViews();

            SQLiteOpenHelper stockDatabaseHelper = new StockDatabaseHelper(MainActivity.this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            for(String json:strings){
                try {
                    if(!json.toLowerCase().contains("response code 400")) {
                        JSONObject stock = new JSONObject(json);
                        String price = stock.getString("l");
                        String symbol = stock.getString("t");
                        String change_percent = stock.getString("cp");
                        String change = stock.getString("c");

                        price = price.replaceAll("^\"|\"$", "");
                        //Log.v("PRICE", price);
                        symbol = symbol.replaceAll("^\"|\"$", "");
                        //Log.v("SYMBOL_DEBUG", symbol);
                        change_percent = change_percent.replaceAll("^\"|\"$", "");
                        change = change.replaceAll("^\"|\"$", "");


                        ContentValues stockValues = new ContentValues();

                        stockValues.put("price", price);
                        stockValues.put("change", change);
                        if (change.contains("-")) {
                            stockValues.put("change_dir", 0);
                        } else {
                            stockValues.put("change_dir", 1);
                        }
                        stockValues.put("change_percent", change_percent);

                        db.update("nasdaq", stockValues, "symbol = ?", new String[]{symbol});

                    }

                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
            //update adapter to relect new changes
            Cursor cursor = db.query("nasdaq", new String[]{"_id","name","symbol","price"},null, null, null, null, null,String.valueOf(50));
            mAdapter.changeCursor(cursor);

            //now update the listviews connected
            mAdapter.notifyDataSetChanged();

            displayListView();
        }
    }

    private void displayListView(){
        try {
            SQLiteOpenHelper stockDatabaseHelper = new StockDatabaseHelper(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            //Cursor cursor = db.query("nasdaq", new String[]{"symbol"},null, null, null, null, null,);
            Cursor cursor = db.query("nasdaq", new String[]{"_id","name","symbol","price"},null, null, null, null, null,String.valueOf(50));

            String[] columns = new String[]{
                "name","symbol","price"
            };

            int[] to = new int[]{
                    R.id.tv_name,
                    R.id.tv_symbol,
                    R.id.tv_price
            };

            mAdapter = new SimpleCursorAdapter(this,R.layout.stock_info,cursor,columns,to,0);
            listView = (ListView)findViewById(R.id.listView);
            listView.setAdapter(mAdapter);

            //cursor.close();
            //db.close();
        }catch(SQLiteException e){
            e.printStackTrace();
        }
    }

    private void repeatTask(){
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getStockData();
            }
        },0, 120000);

    }

    public void getStockData(){

        ArrayList<URL> urls = new ArrayList<URL>();
        String query = null;
        try {
            SQLiteOpenHelper stockDatabaseHelper = new StockDatabaseHelper(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            Cursor cursor = db.query("nasdaq", new String[]{"symbol"},null, null, null, null, null,String.valueOf(50));

            while(cursor.moveToNext()){
                query = cursor.getString(0);

                query = query.replaceAll("^\"|\"$", "");
                urls.add(NetworkUtils.buildUrl(query));
                //Log.v("SYMBOL",query);
            }


            /*final URL queryurl = NetworkUtils.buildUrl(query);*/
            //run task only if internet connection available
            if(isNetworkAvailable()) {
                Log.v("NETWORK","Available");
                new StockQueryTask().execute(urls);
            }
            //cursor.close();
            //db.close();

        }catch(SQLiteException e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId){
            case R.id.action_refresh:
                getStockData();
                break;

            case R.id.action_settings:

        }

        return super.onOptionsItemSelected(item);
    }
}
