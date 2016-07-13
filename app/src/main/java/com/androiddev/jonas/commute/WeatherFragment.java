package com.androiddev.jonas.commute;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class WeatherFragment extends Fragment {

    // Views
    RelativeLayout weatherOne, weatherTwo, weatherThree;
    LinearLayout weatherPeriods, weatherRow;
    ProgressBar weatherSpinner;

    // Variables
    String WEATHER_API_URL = "http://www.yr.no/place/Norway/Oslo/Oslo/Oslo/varsel.xml";
    String WEATHER_API_ICONS_URL = "http://api.yr.no/weatherapi/weathericon/1.1/?symbol=";

    // Weather data cache
    LruCache<String, Bitmap> weatherIconCache;
    LruCache<String, HashMap<String, JSONObject>> weatherDataCache;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.weather_fragment, container, false);

        weatherOne = (RelativeLayout) layout.findViewById(R.id.weather_one);
        weatherTwo = (RelativeLayout) layout.findViewById(R.id.weather_two);
        weatherThree = (RelativeLayout) layout.findViewById(R.id.weather_three);
        weatherRow = (LinearLayout) layout.findViewById(R.id.WeatherRow);
        weatherPeriods = (LinearLayout) layout.findViewById(R.id.WeatherPeriods);
        weatherSpinner = (ProgressBar) layout.findViewById(R.id.weatherProgressBar);

        setupCaches();
        checkWeather();

        return layout;
    }

    // Setup caches for icons and data
    public void setupCaches() {
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 30;
        weatherIconCache = new LruCache<>(cacheSize);
        weatherDataCache = new LruCache<>(cacheSize);
    }

    // Request weather data from YR
    public void checkWeather() {

        // Check if wheather-cache exists
        HashMap<String, String> dateTime = getTimeAndDate();
        String today = dateTime.get("year") + "-" + dateTime.get("month") + "-" + dateTime.get("day");
        String first_period = today + "T06:00:00";
        String second_period = today + "T12:00:00";
        String third_period = today + "T18:00:00";
        if(weatherDataCache.get(first_period) != null && weatherDataCache.get(second_period) != null && weatherDataCache.get(third_period) != null) {
            Toast.makeText(getActivity(), "Data already in cache", Toast.LENGTH_SHORT).show();
            weatherSpinner.setVisibility(View.GONE);
        } else {
            weatherSpinner.setVisibility(View.VISIBLE);
            RequestClient client = new RequestClient();
            client.execute(WEATHER_API_URL);
        }
    }

    // Update the views with new weather data
    public void updateWeatherText(JSONArray ja) {

        try {
            JSONObject data = ja.getJSONObject(0);
            // HERE: Parse through weather data
            JSONArray times = data.getJSONObject("weatherdata").getJSONObject("forecast").getJSONObject("tabular").getJSONArray("time");

            // Fetch the date
            HashMap<String, String> dateTime = getTimeAndDate();
            String today = dateTime.get("year") + "-" + dateTime.get("month") + "-" + dateTime.get("day");
            String first_period = today + "T06:00:00";
            String second_period = today + "T12:00:00";
            String third_period = today + "T18:00:00";

            // Run through weather items
            int length = times.length();
            for(int i = 0; i < length; i++) {
                JSONObject j = times.getJSONObject(i);
                String from = j.getString("from");
                String period = j.getString("period");

                if(from.contains(today)) {
                    // Check first period
                    if(period.equals("1")){
                        populateWeatherLayout(weatherOne, j, first_period);
                    }

                    // Check second period
                    if(period.equals("2")) {
                        populateWeatherLayout(weatherTwo, j, second_period);
                    }

                    // Check third period
                    if(period.equals("3")) {
                        populateWeatherLayout(weatherThree, j, third_period);
                    }
                }
            }

            // Check cache
            updateWeatherFromCache(first_period, second_period, third_period);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Show views
        weatherSpinner.setVisibility(View.GONE);
        weatherPeriods.setVisibility(View.VISIBLE);
        weatherRow.setVisibility(View.VISIBLE);

    }

    // Popualtes views if new data is found
    public void populateWeatherLayout(RelativeLayout layout, JSONObject json, String cache_identifier) {
        try {
            JSONObject precipitation = json.getJSONObject("precipitation");
            JSONObject temperature = json.getJSONObject("temperature");
            JSONObject symbol = json.getJSONObject("symbol");

            // Update cache
            if(weatherDataCache.get(cache_identifier) == null) {
                HashMap<String, JSONObject> cache_data = new HashMap<String, JSONObject>();
                cache_data.put("temperature", temperature);
                cache_data.put("precipitation", precipitation);
                cache_data.put("symbol", symbol);
                weatherDataCache.put(cache_identifier, cache_data);
            }

            // Load image
            int sym = symbol.getInt("number");
            ImageView icon = (ImageView) layout.getChildAt(0);
            new ImageLoader(icon).execute(WEATHER_API_ICONS_URL + sym + ";content_type=image/png", cache_identifier);

            // Set temperature
            int temp = temperature.getInt("value");
            TextView viewTemp = (TextView) layout.getChildAt(1);
            viewTemp.setText(""+temp + " \u2103");

            // Set precipitation
            int prec = precipitation.getInt("value");
            TextView rain = (TextView) layout.getChildAt(2);
            rain.setText("(" + prec + " mm)");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Populates views from stored cache
    public void updateWeatherFromCache(String cache_one, String cache_two, String cache_three) {
        // In case some didn't get updated, check cache and update from there
        HashMap<String, JSONObject> first_data = weatherDataCache.get(cache_one);
        HashMap<String, JSONObject> second_data = weatherDataCache.get(cache_two);
        HashMap<String, JSONObject> third_data = weatherDataCache.get(cache_three);
        Bitmap first_icon = weatherIconCache.get(cache_one);
        Bitmap second_icon = weatherIconCache.get(cache_two);
        Bitmap third_icon = weatherIconCache.get(cache_three);

        // Check first period
        if(first_data != null) {
            int temp = 0;
            try {
                temp = first_data.get("temperature").getInt("value");
                TextView viewTemp = (TextView) weatherOne.getChildAt(1);
                viewTemp.setText(""+temp + " \u2103");

                int precip = first_data.get("precipitation").getInt("value");
                TextView rain = (TextView) weatherOne.getChildAt(2);
                rain.setText("("+precip + " mm)");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(first_icon != null) {
            ImageView icon = (ImageView) weatherOne.getChildAt(0);
            icon.setImageBitmap(first_icon);
        }

        // Check second period
        if(second_data != null) {
            int temp = 0;
            try {
                temp = second_data.get("temperature").getInt("value");
                TextView viewTemp = (TextView) weatherTwo.getChildAt(1);
                viewTemp.setText(""+temp + " \u2103");

                int precip = second_data.get("precipitation").getInt("value");
                TextView rain = (TextView) weatherTwo.getChildAt(2);
                rain.setText("("+precip + " mm)");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(second_icon != null) {
            ImageView icon = (ImageView) weatherTwo.getChildAt(0);
            icon.setImageBitmap(second_icon);
        }

        // Check third period
        if(first_data != null) {
            int temp = 0;
            try {
                temp = third_data.get("temperature").getInt("value");
                TextView viewTemp = (TextView) weatherThree.getChildAt(1);
                viewTemp.setText(""+temp + " \u2103");

                int precip = third_data.get("precipitation").getInt("value");
                TextView rain = (TextView) weatherThree.getChildAt(2);
                rain.setText("("+precip + " mm)");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(third_icon != null) {
            ImageView icon = (ImageView) weatherThree.getChildAt(0);
            icon.setImageBitmap(third_icon);
        }
    }

    // Returns the time and date
    public HashMap<String, String> getTimeAndDate() {
        Calendar c = Calendar.getInstance();
        String hour = String.format("%02d", c.get(Calendar.HOUR_OF_DAY));
        String min = String.format("%02d", c.get(Calendar.MINUTE));
        String sec = String.format("%02d", c.get(Calendar.SECOND));
        String day = String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
        String month = String.format("%02d", c.get(Calendar.MONTH)+1);
        String year = String.format("%02d", c.get(Calendar.YEAR));

        HashMap<String, String> timeAndDate = new HashMap<String, String>();
        timeAndDate.put("hour", hour);
        timeAndDate.put("min", min);
        timeAndDate.put("sec", sec);
        timeAndDate.put("day", day);
        timeAndDate.put("month", month);
        timeAndDate.put("year", year);

        return timeAndDate;
    }

    /* The request manager */
    private class RequestClient extends AsyncTask<String, String, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            JSONArray response = new JSONArray();

            try {
                url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                int responseCode = urlConnection.getResponseCode();
                String content_type = urlConnection.getHeaderField("Content-Type").split(";")[0].trim();

                if (responseCode == 200) {
                    String responseString = readStream(urlConnection.getInputStream());

                    if(content_type.toLowerCase().contains("xml")) {
                        // HERE: Convert to JSONArray
                        JSONObject jay = XML.toJSONObject(responseString);
                        response = new JSONArray();
                        response.put(jay);
                    }

                } else {
                    Log.d("COMMUTE", "Weather Response code: " + responseCode);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            if(jsonArray.length() > 0) {
                updateWeatherText(jsonArray);
            } else {
                Log.d("COMMUTE", "Weather data jsonarray is empty");
            }
        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();

            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }

    /* Downloader for images */
    private class ImageLoader extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        String cache_identifier;

        public ImageLoader(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            cache_identifier = params[1];
            Bitmap mIcon = null;
            try {
                InputStream in = new URL(url).openStream();
                mIcon = BitmapFactory.decodeStream(in);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mIcon;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            bmImage.setImageBitmap(bitmap);
            weatherIconCache.put(cache_identifier, bitmap);
        }
    }
}