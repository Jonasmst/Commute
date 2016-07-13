package com.androiddev.jonas.commute;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

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

/* Fragment for tabs */
public class TransportFragment extends Fragment {

    // Views
    private SwipeRefreshLayout refreshLayout;
    private LinearLayout proposalsList;

    // Variables
    String RUTER_API_URL = "http://api.trafikanten.no/reisrest/Travel/GetTravelsByPlaces/?";
    String RUTER_API_URL_TAIL = "&changeMargin=4&propolsals=5&isafter=true";
    String THORSOV_ID = "3010430";
    String FORSK_AREA_ID = "1000018525";
    String RADIUM_ID = "3012583";
    String TO_ACTIVE = FORSK_AREA_ID;
    String[] destinations = {FORSK_AREA_ID, RADIUM_ID};
    // Colors
    String TRAM_COLOR = "#41BECD";
    String TBANE_COLOR = "#F07800";
    String BUS_COLOR = "#E60000";

    public static TransportFragment getInstance(int position) {
        TransportFragment transportFragment = new TransportFragment();

        Bundle args = new Bundle();
        args.putInt("position", position);
        transportFragment.setArguments(args);

        return transportFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.transport_fragment, container, false);

        refreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.SwipeRefreshLayout);
        refreshLayout.setProgressViewOffset(true, 50, 150);
        proposalsList = (LinearLayout) layout.findViewById(R.id.ProposalsLayout);

        Bundle bundle = getArguments();
        if(bundle != null) {
            TO_ACTIVE = destinations[bundle.getInt("position")];
        }

        setListeners();
        checkTransport();

        return layout;
    }

    // Sets listener on the pull-to-refresh
    public void setListeners() {
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                checkTransport();
            }
        });
    }

    public void checkTransport() {

        // Set refresher to true. Used when refresh is not made actively by user.
        if(!refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(true);
        }

        proposalsList.removeAllViews();
        RequestClient client = new RequestClient();
        HashMap<String, String> dt = getTimeAndDate();
        String departure = dt.get("day") + dt.get("month") + dt.get("year") + dt.get("hour") + dt.get("min");
        String url = RUTER_API_URL + "time=" + departure + "&fromplace=" + THORSOV_ID + "&toplace=" + TO_ACTIVE + RUTER_API_URL_TAIL;

        Log.v("RUTER", "URL: " + url);

        client.execute(url);
    }

    public void updateTransportText(JSONArray ja) {

        try {
            JSONObject data = ja.getJSONObject(0);
            JSONArray proposals = data.getJSONObject("GetTravelsByPlacesResult").getJSONArray("TravelProposals");

            // Iterate over proposals
            int length = proposals.length();
            for(int i = 0; i < length; i++) {
                JSONObject prop = proposals.getJSONObject(i);

                // First, test with duration of proposal
                String arrival = prop.getString("ArrivalTime"); // E.g. /Date(1433358180000+0200)/
                String departure = prop.getString("DepartureTime");



                int arrival_start_index = arrival.indexOf("(");
                int arrival_end_index = arrival.indexOf("+");
                int departure_start_index = departure.indexOf("(");
                int departure_end_index = departure.indexOf("+");

                Long arrival_timestamp = Long.parseLong(arrival.substring(arrival_start_index + 1, arrival_end_index));
                Long departure_timestamp = Long.parseLong(departure.substring(departure_start_index + 1, departure_end_index));

                Calendar arrival_cal = Calendar.getInstance();
                arrival_cal.setTimeInMillis(arrival_timestamp);
                String arrivalTime = String.format("%02d:%02d", arrival_cal.get(Calendar.HOUR_OF_DAY), arrival_cal.get(Calendar.MINUTE));

                Calendar departure_cal = Calendar.getInstance();
                departure_cal.setTimeInMillis(departure_timestamp);
                String departureTime = String.format("%02d:%02d", departure_cal.get(Calendar.HOUR_OF_DAY), departure_cal.get(Calendar.MINUTE));

                // Calculate difference - I can simply use milliseconds when determining which one is faster.
                Long difference = arrival_timestamp - departure_timestamp;
                String duration_minutes = String.format("%02d min", TimeUnit.MILLISECONDS.toMinutes(difference) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference)));

                // Get stages
                JSONArray stages = prop.getJSONArray("TravelStages");
                int numStages = stages.length();

                // Keep track of which stage has been checked
                String firstStage = null;
                String secondStage = null;

                for(int j = 0; j < numStages; j++) {
                    JSONObject stage = stages.getJSONObject(j);

                    // Skip walking part
                    if(stage.getString("Destination").toLowerCase().equals("gangledd")) {
                        continue;
                    }

                    if(firstStage == null) {
                        firstStage = stage.getString("LineName");
                    } else if(secondStage == null) {
                        secondStage = stage.getString("LineName");
                    } else {
                        Log.v("RUTER", "There seems to more than two stages.");
                    }
                }

                // TODO: Icons based on lineName
                // Add new ProposalView
                ProposalView propView = new ProposalView(getActivity());
                propView.getTime().setText(departureTime + " - " + arrivalTime);
                propView.getDuration().setText(duration_minutes); // This may need refactoring
                propView.getFirstMedium().setBackgroundColor(Color.parseColor(getLineColorsByName(firstStage)));
                propView.getFirstLineNumber().setText(firstStage);
                propView.getFirstLineIcon().setImageResource(getLineIconsByLineName(firstStage));

                // Check if there is a second stage
                if(secondStage != null) {
                    propView.getWalkingMan().setVisibility(View.VISIBLE);
                    propView.getSecondMedium().setVisibility(View.VISIBLE);
                    propView.getSecondMedium().setBackgroundColor(Color.parseColor(getLineColorsByName(secondStage)));
                    propView.getSecondLineNumber().setText(secondStage);
                    propView.getSecondLineIcon().setImageResource(getLineIconsByLineName(secondStage));
                }

                // Add to scrollview
                proposalsList.addView(propView);

            }

            // Cancel loading spinner
            if(refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(false);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Get line color
    public String getLineColorsByName(String lineName) {
        int i = Integer.parseInt(lineName);

        if(i <= 6) {
            // Subways
            return TBANE_COLOR;
        } else if(i >= 11 && i <= 19) {
            // Trams
            return TRAM_COLOR;
        }

        return BUS_COLOR;
    }

    // Get line icons
    public int getLineIconsByLineName(String lineName) {

        int i = Integer.parseInt(lineName);

        if(i <= 6) {
            // Subways
            return R.drawable.tbane;
        } else if(i >= 11 && i <= 19) {
            // Trams
            return R.drawable.tram;
        } else if(i > 19) {
            // Buses
            return R.drawable.bus;
        }

        return R.drawable.walking;
    }

    // Get the time and the date
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

                if (responseCode == 200) {
                    String responseString = readStream(urlConnection.getInputStream());
                    response = new JSONArray();
                    JSONObject first = new JSONObject(responseString);
                    response.put(first);

                } else {
                    Log.v("COMMUTE", "Response code: " + responseCode);
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
                updateTransportText(jsonArray);
            } else {
                Log.d("COMMUTE", "Transport: JsonArray is empty");
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


}