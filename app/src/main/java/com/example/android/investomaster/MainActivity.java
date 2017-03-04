package com.example.android.investomaster;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.investomaster.utilities.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView mJsonResponse;
    public SimpleCursorAdapter mAdapter;
    private ListView listView;
    private Timer t;

    private ProgressBar mLoadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mLoadingIndicator = (ProgressBar)findViewById(R.id.pb_loading_indicator);


        displayListView();
        repeatTask();


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

            mLoadingIndicator.setVisibility(View.INVISIBLE);



            SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(MainActivity.this);
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
                        symbol = symbol.replaceAll("^\"|\"$", "");
                        //Log.v("SYMBOL_DEBUG", symbol);
                        change_percent = change_percent.replaceAll("^\"|\"$", "");
                        change = change.replaceAll("^\"|\"$", "");


                        //---------------SEND NOTIFICATION LOGIC---------------
                        float temp = Float.parseFloat(price);


                        Cursor cursor = db.query("nasdaq", new String[]{"_id","lower","upper","notif","name"},"symbol =  ?", new String[]{symbol}, null, null, null);
                        cursor.moveToFirst();


                        long id = cursor.getLong(0);
                        int lower = cursor.getInt(1);
                        int upper = cursor.getInt(2);
                        int notif = cursor.getInt(3);
                        String stock_name = cursor.getString(4);


                        if((lower!=-1 || upper!=-1)&& notif==0 ){

                            NotificationCompat.Builder mBuilder = null;


                            if(lower!=-1 && temp<lower){
                                mBuilder =
                                         new NotificationCompat.Builder(getApplicationContext())
                                                .setSmallIcon(R.drawable.ic_settings_white_24dp)
                                                .setContentTitle("Stock "+symbol)
                                                .setContentText("Current price : "+price+" < Watchpoint "+Integer.toString(lower));


                            }
                            else if(upper!=-1 && temp>upper){
                                mBuilder =
                                        (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                                                .setSmallIcon(R.drawable.ic_settings_white_24dp)
                                                .setContentTitle("Stock "+symbol)
                                                .setContentText("Current price : "+price+" > Watchpoint "+Integer.toString(upper));
                            }


                            NotificationManager mNotificationManager =

                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


                            int m = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);


                            Intent detailed_info = new Intent(MainActivity.this,StockDetail.class);
                            detailed_info.putExtra("ID",id);
                            PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, (int)(Math.random()*100), detailed_info, 0);
                            mBuilder.setContentIntent(pendingIntent);

                            mNotificationManager.notify((int)(Math.random()*100),mBuilder.build());

                            //NOW SET notif to 1
                            ContentValues notifs = new ContentValues();
                            notifs.put("notif",1);
                            db.update("nasdaq", notifs, "symbol = ?", new String[]{symbol});

                        }
                        else{

                            //if price is between lower and upper then reset notif to 0,to allow notifications after first occurance
                            if(temp>lower && temp<upper){
                                ContentValues notifs = new ContentValues();
                                notifs.put("notif",0);
                                db.update("nasdaq", notifs, "symbol = ?", new String[]{symbol});
                            }
                        }

                        //----------------  SEND NOTIFICATION LOGIC OVER-----------------


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
            SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();


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

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                    Cursor cursor = (Cursor) listView.getItemAtPosition(position);


                    Intent detailed_info = new Intent(MainActivity.this,StockDetail.class);
                    detailed_info.putExtra("ID",id);

                    startActivity(detailed_info);


                }
            });


            //cursor.close();
            //db.close();
        }catch(SQLiteException e){
            e.printStackTrace();
        }
    }

    private void repeatTask(){

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int time = pref.getInt("AUTO_TIME",2);


        //t!=null if this is not the first call,if not first call then timeout value has changed,so cancel previous task
        if(t!=null) {
            t.cancel();
        }
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getStockData();
            }
        },0, 60000 * time);

    }

    public void getStockData(){

        ArrayList<URL> urls = new ArrayList<URL>();
        String query = null;
        try {
            SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            Cursor cursor = db.query("nasdaq", new String[]{"symbol"},null, null, null, null, null,String.valueOf(50));

            while(cursor.moveToNext()){
                query = cursor.getString(0);

                query = query.replaceAll("^\"|\"$", "");
                urls.add(NetworkUtils.buildUrl(query));

            }


            if(isNetworkAvailable()) {

                new StockQueryTask().execute(urls);
            }
            else
            {
                //make invisible if network not present
                mLoadingIndicator.setVisibility(View.INVISIBLE);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"No Network!",Toast.LENGTH_LONG).show();
                    }
                });

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
                mLoadingIndicator.setVisibility(View.VISIBLE);
                getStockData();
                break;

            case R.id.action_settings:
                getAutoRefreshTimeout();
                break;

            case R.id.action_favorite:
                Intent favorites = new Intent(this,Favorites.class);
                startActivity(favorites);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getAutoRefreshTimeout(){
        RelativeLayout linearLayout = new RelativeLayout(MainActivity.this);
        final NumberPicker aNumberPicker = new NumberPicker(MainActivity.this);
        aNumberPicker.setMaxValue(30);
        aNumberPicker.setMinValue(1);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
        RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        linearLayout.setLayoutParams(params);
        linearLayout.addView(aNumberPicker,numPicerParams);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int current = prefs.getInt("AUTO_TIME",2);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle("Select auto refresh interval in minutes(current value : "+ Integer.toString(current)+")");
        alertDialogBuilder.setView(linearLayout);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {

                                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putInt("AUTO_TIME",aNumberPicker.getValue());
                                editor.commit();
                                repeatTask();

                                Log.e("","New Quantity Value : "+ aNumberPicker.getValue());

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
