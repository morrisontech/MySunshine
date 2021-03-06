package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {


    private ArrayAdapter<String> mForecastAdapter;
    // makes array adapter global variable
    // so it can be called from all methods in ForecastFragment

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this line checks for menu options that need to be inflated for this fragment-
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_data, menu);

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                FetchWeatherTask weatherTask = new FetchWeatherTask();
                weatherTask.execute("98102");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        //list of random data to fill View class named rootView
        final ArrayList<String> forecastArray = new ArrayList<>();
        forecastArray.add("Today - Sunny - 88/63");
        forecastArray.add("Tomorrow - Foggy - 80/62");
        forecastArray.add("Wednesday - Cloudy - 80/62");
        forecastArray.add("Thursday - Sunny - 80/62");
        forecastArray.add("Friday - Windy - 80/62");
        forecastArray.add("Saturday - Cloudy - 80/62");
        forecastArray.add("Sunday - Rain - 70/62");

        //ArrayAdapter(Context context, int resource, int textViewResourceID, T[ ] objects
        //ArrayAdapter populates an array of objects into a specified view inside a container of a specified context
        mForecastAdapter = new ArrayAdapter<String>
                (getActivity(),
                        R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,
                        forecastArray);
        //ListView populates ArrayAdapter data into the actual GUI listview
        final ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                //onItemClick envokes a callback method when an item is clicked
                String clickedItem = mForecastAdapter.getItem(i);
//               Toast toastWeather = Toast.makeText(getActivity(), clickedItem, Toast.LENGTH_SHORT);
//               toastWeather.show();

                Intent openDetailActivity = new Intent(getActivity(), DetailActivity.class)
                        .putExtra("message", clickedItem);
                startActivity(openDetailActivity);

            }
        });

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        //create tag variable to use for logging
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.


            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            // "list" includes an array of daily forecast data
            final String OWM_LIST = "list";
            // max and min temperatures
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            //"weather" includes array of one object (which can be referenced at index 0)
            final String OWM_WEATHER = "weather";
            final String OWM_DESCRIPTION = "description";


            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            //resultStrs stores list of data results in a single string
            //to display list basic forecast list in list_item_forecast
            String[] resultStrs = new String[numDays];

            //downloaded JSON string passed into JSON object
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            //iterate through JSONObject "list" array (parent node
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherDescription = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherDescription.getString(OWM_DESCRIPTION);


                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                //JSONObject temperatureObject = dayForecast.getJSONObject(MAIN_NODE);
                JSONObject temperatureObject = dayForecast.getJSONObject("temp");
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;

            }

            return resultStrs;
        }

        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String unitType = "imperial";
            String format = "json";
            String apiKey = "0483d5d55d7fc757e8b57ebde8804cd4";
            int numDays = 7;
            //String numDaysString="7";


            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //http://api.openweathermap.org/data/2.5/forecast?q=98102&units=imperial&appid=0483d5d55d7fc757e8b57ebde8804cd4
                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units"; //use metric in other places
                final String API_KEY = "appid";
                final String NUM_DAYS = "cnt";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, unitType)
                        .appendQueryParameter(NUM_DAYS, Integer.toString(numDays))
                        .appendQueryParameter(API_KEY, apiKey)
                        .build();
                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Built URI: " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                Log.d(LOG_TAG, "doInBackground() returned: " + forecastJsonStr);

            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(String[] strings) {
            if (strings != null) {
                mForecastAdapter.clear();
                for (String dayForecastStr : strings) {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}
