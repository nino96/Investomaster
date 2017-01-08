package com.example.android.investomaster;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class Favorites extends AppCompatActivity {

    public SimpleCursorAdapter mAdapter;
    private ListView listView;
    private TextView mErrorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        listView = (ListView)findViewById(R.id.listView_fav);
        mErrorMessage = (TextView)findViewById(R.id.tv_no_favorites);

        displayListView();

    }



    //onRestart() to update the favorites list if changing some favorites and then pressing back button to get original activity
    //without changes, so update listview here
    @Override
    protected void onRestart() {
        super.onRestart();

        //DON'T INVOKE IF MADAPTER NOT INITIALIZED YET
        if(mAdapter!=null) {
            try {
                SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
                SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

                //Cursor cursor = db.query("nasdaq", new String[]{"symbol"},null, null, null, null, null,);
                Cursor cursor = db.query("nasdaq", new String[]{"_id", "name", "symbol", "price"}, "favorite = ?", new String[]{Integer.toString(1)}, null, null, null);

                mAdapter.changeCursor(cursor);
                mAdapter.notifyDataSetChanged();
                displayListView();

            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayListView(){
        try {
            SQLiteOpenHelper stockDatabaseHelper = StockDatabaseHelper.getInstance(this);
            SQLiteDatabase db = stockDatabaseHelper.getReadableDatabase();

            //Cursor cursor = db.query("nasdaq", new String[]{"symbol"},null, null, null, null, null,);
            Cursor cursor = db.query("nasdaq", new String[]{"_id","name","symbol","price"},"favorite = ?", new String[]{Integer.toString(1)}, null, null, null);

            if(!cursor.moveToFirst()){
                mErrorMessage.setVisibility(View.VISIBLE);
                return;
            }
            else{
                mErrorMessage.setVisibility(View.GONE);
            }

            String[] columns = new String[]{
                    "name","symbol","price"
            };

            int[] to = new int[]{
                    R.id.tv_name,
                    R.id.tv_symbol,
                    R.id.tv_price
            };

            mAdapter = new SimpleCursorAdapter(this,R.layout.stock_info,cursor,columns,to,0);
            //listView = (ListView)findViewById(R.id.listView);
            listView.setAdapter(mAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    //get cursor positioned to corresponding row in result set
                    Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                    //create new activity,passing in the data of the selected item
                    Intent detailed_info = new Intent(Favorites.this,StockDetail.class);
                    detailed_info.putExtra("ID",id);

                    startActivity(detailed_info);


                }
            });


            //IF CLOSED THEN GIVING ILLEGAL STATE EXCEPTION(Attempt to reopen already closed object : sqlitequery)
            //so cursor.close() releases all resources and should not be closed until we are done with all the work with it

            //cursor.close();
            //db.close();
        }catch(SQLiteException e){
            e.printStackTrace();
        }
    }
}
