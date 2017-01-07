package com.example.android.investomaster.utilities;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Niyam on 06/01/2017.
 */

public class NetworkUtils {

    private static String QUOTE_BASE_URL1 = "https://www.google.com/finance/info";
    private static String QUOTE_BASE_URL = "http://dev.markitondemand.com/MODApis/Api/v2/Quote/json";
    private static String PARAM_SYMBOL="symbol";
    private static String PARAM_SYMBOL1="q";

    private static String QUERY_SYMBOL="GOOGL";


    public static URL buildUrl(String query){
        Uri builtUri = Uri.parse(QUOTE_BASE_URL1).buildUpon()
                .appendQueryParameter(PARAM_SYMBOL1,query)
                .build();

        URL url = null;
        try{
            url = new URL(builtUri.toString());

        }catch(MalformedURLException e){
            e.printStackTrace();
        }
        return url;
    }

    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

}
