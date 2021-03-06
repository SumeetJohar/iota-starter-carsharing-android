/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the IBM License, a copy of which may be obtained at:
 * <p>
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AEGGZJ&popup=y&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
 * <p>
 * You may not use this file except in compliance with the license.
 */
package carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class API {
    public static String defaultAppURL = "https://iota-starter-server.mybluemix.net";
    public static String defaultAppGUID = "";
    public static String defaultCustomAuth = ""; // non-MCA server
    //    public static String bmRegion = BMSClient.REGION_US_SOUTH;
    public static String customRealm = "custauth";

    public static String connectedAppURL = defaultAppURL;
    public static String connectedAppGUID = defaultAppGUID;
    public static String connectedCustomAuth = defaultCustomAuth;

    // Setup all the API endpoint URLs
    public static String carsNearby = connectedAppURL + "/user/carsnearby";
    public static String reservation = connectedAppURL + "/user/reservation";
    public static String reservations = connectedAppURL + "/user/activeReservations";
    public static String carControl = connectedAppURL + "/user/carControl";
    public static String driverStats = connectedAppURL + "/user/driverInsights/statistics";
    public static String trips = connectedAppURL + "/user/driverInsights";
    public static String tripBehavior = connectedAppURL + "/user/driverInsights/behaviors";
    public static String latestTripBehavior = connectedAppURL + "/user/driverInsights/behaviors/latest";
    public static String tripRoutes = connectedAppURL + "/user/triproutes";
    public static String tripAnalysisStatus = connectedAppURL + "/user/driverInsights/tripanalysisstatus";
    public static String credentials = connectedAppURL + "/user/device/credentials";

    public static Context context;
    public static SharedPreferences sharedpreferences;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void runInAsyncUIThread(final Runnable task, final Activity activity) {
        runInAsyncUIThread(task, activity, 0);
    }

    public static void runInAsyncUIThread(final Runnable task, final Activity activity, final long delayMs) {
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                activity.runOnUiThread(task);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }


    public API(Context context) {
        this.context = context;
    }

    public static void setURIs(final String appURL) {
        // Setup all the API endpoint URLs
        carsNearby = appURL + "/user/carsnearby";
        reservation = appURL + "/user/reservation";
        reservations = appURL + "/user/activeReservations";
        carControl = appURL + "/user/carControl";
        driverStats = appURL + "/user/driverInsights/statistics";
        trips = appURL + "/user/driverInsights";
        tripBehavior = appURL + "/user/driverInsights/behaviors";
        latestTripBehavior = appURL + "/user/driverInsights/behaviors/latest";
        tripRoutes = appURL + "/user/triproutes";
        credentials = appURL + "/user/device/credentials";
    }

    public static void setDefaultServer() {
        connectedAppURL = defaultAppURL;
        connectedAppGUID = defaultAppGUID;
        connectedCustomAuth = defaultCustomAuth;

        setURIs(connectedAppURL);
    }

    public static void doInitialize() {
        // Use sharedpreferences to store data on the device (to not show repetitive warnings etc.)
        sharedpreferences = context.getSharedPreferences("carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI", Context.MODE_PRIVATE);

        final String appRoute = sharedpreferences.getString("appRoute", "no-appRoute");
        final String appGUID = sharedpreferences.getString("appGUID", "no-appGUID");
        final String customAuth = sharedpreferences.getString("customAuth", "no-customAuth");

        if (appRoute != "no-appRoute") {
            connectedAppURL = appRoute;
            connectedAppGUID = appGUID == "no-appGUID" ? "" : appGUID;
            connectedCustomAuth = customAuth == "no-customAuth" ? "false" : customAuth;

            setURIs(connectedAppURL);
        }

        if (connectedCustomAuth == "true") {
            Log.i("API", "Initialize and set up MCA");
//            BMSClient.sharedInstance.initializeWithBluemixAppRoute(connectedAppURL, bluemixAppGUID: connectedAppGUID, bluemixRegion: bmRegion)
//            BMSClient.sharedInstance.authorizationManager = MCAAuthorizationManager.sharedInstance
//            delegateCustomAuthHandler()
        } else {
            Log.i("API", "Non-MCA server");
        }
    }

    public static String getUUID() {
        // Use sharedpreferences to store data on the device (to not show repetitive warnings etc.)
        sharedpreferences = context.getSharedPreferences("carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI", Context.MODE_PRIVATE);

        // Check if a UUID has already been assigned to and stored on the device
        String uuidString = sharedpreferences.getString("iota-starter-uuid", "no-iota-starter-uuid");

        // If there's already a UUID assigned, then just return it
        if (uuidString != "no-iota-starter-uuid") {
            return uuidString;
        } else { // If not, then generate one and return
            uuidString = UUID.randomUUID().toString();

            sharedpreferences.edit().putString("iota-starter-uuid", uuidString).apply();

            return uuidString;
        }
    }

    // Check if the disclaimer has been shown to the user already
    public static boolean warningShown() {
        sharedpreferences = context.getSharedPreferences("carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI", Context.MODE_PRIVATE);

        final boolean warningShown = sharedpreferences.getBoolean("iota-starter-warning-message", false);

        if (warningShown) {
            return warningShown;
        } else {
            sharedpreferences.edit().putBoolean("iota-starter-warning-message", true).apply();

            return false;
        }
    }

    /**
     * @param boolean agreed - true if the user agreed, false if not
     * @return bool - disclaimerShownAndAgreed or false
     * @desc Checks if the disclaimer has been shown to the user already, and stores the value on the device if they agree
     */
    public static boolean disclaimerShown(boolean agreed) {
        sharedpreferences = context.getSharedPreferences("carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI", Context.MODE_PRIVATE);

        final boolean disclaimerShownAndAgreed = sharedpreferences.getBoolean("iota-starter-disclaimer", false);

        if (disclaimerShownAndAgreed) {
            return disclaimerShownAndAgreed;
        } else if (!disclaimerShownAndAgreed && agreed) {
            sharedpreferences.edit().putBoolean("iota-starter-disclaimer", true).apply();
        }

        return false;
    }

    // Custom class to make and handle http requests
    public static class doRequest extends AsyncTask<String, Void, JSONArray> {

        public interface TaskListener {
            // Call the postExecute function after the request is finished
            public void postExecute(JSONArray result) throws JSONException, MqttException;
        }

        private final TaskListener taskListener;

        public doRequest(TaskListener listener) {
            this.taskListener = listener;
        }

        /*
         * @desc Async function that will execute the request
         * @param string params -
         *      params[0] == url (String)
                params[1] == request type (String e.g. "GET")
                params[2] == parameters query (Uri converted to String)
                params[3] == body (JSONObject converted to String)
         * @return JSONArray result - response data
         */
        @Override
        protected JSONArray doInBackground(String... params) {
            int code = 0;

            try {
                final URL url = new URL(params[0]);   // params[0] == URL - String
                final String requestType = params[1]; // params[1] == Request Type - String e.g. "GET"
                final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                Log.i(requestType + " Request", params[0]);

                // Set request header property "iota-starter-uuid" to the UUID assigned to the device
                urlConnection.setRequestProperty("iota-starter-uuid", getUUID());
                Log.i("Using UUID", getUUID());

                urlConnection.setRequestMethod(requestType); // GET, POST, FETCH, etc.

                if (requestType == "POST" || requestType == "PUT") {
                    // Set the DoInput flag to true if you intend to use the URL connection for input, false if not.
                    // The default is true, but we're going to set it to True just in case anything overrides it
                    urlConnection.setDoInput(true);

                    // Enable sending request body
                    urlConnection.setDoOutput(true);

                    // If an HTTP Parameters Query has been set
                    if (params.length > 2 && params[2] != null) { // params[2] == HTTP Parameters Query - String
                        final String query = params[2];

                        // Write the query to the outputstream for our connection object
                        final OutputStream os = urlConnection.getOutputStream();
                        final BufferedWriter writer = new BufferedWriter(
                                new OutputStreamWriter(os, "UTF-8"));
                        writer.write(query);
                        writer.flush();
                        writer.close();
                        os.close();

                        urlConnection.connect();
                        Log.i("Using Parameters:", params[2]);
                    }

                    // If an HTTP body has been set
                    if (params.length > 3) { // params[3] == HTTP Body - String
                        final String httpBody = params[3];

                        urlConnection.setRequestProperty("Content-Type", "application/json");
                        urlConnection.setRequestProperty("Content-Length", httpBody.length() + "");

                        final OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8");
                        wr.write(httpBody);
                        wr.flush();
                        wr.close();

                        Log.i("Using Body:", httpBody);
                    }
                }

                try {
                    // Get the response code (200, 400, 500, etc.)
                    code = urlConnection.getResponseCode();

                    final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    final StringBuilder stringBuilder = new StringBuilder();

                    String line;
                    // Go through every line in the bufferedReader and append the line to the stringBuilder
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    bufferedReader.close();

                    try {
                        final JSONArray result = new JSONArray(stringBuilder.toString());

                        final JSONObject statusCode = new JSONObject();
                        statusCode.put("statusCode", code + "");

                        result.put(statusCode);

                        return result;
                    } catch (JSONException ex) {
                        try {
                            final JSONArray result = new JSONArray();

                            final JSONObject object = new JSONObject(stringBuilder.toString());
                            result.put(object);

                            final JSONObject statusCode = new JSONObject();
                            statusCode.put("statusCode", code);
                            Log.d("Responded With", code + "");

                            result.put(statusCode);

                            return result;
                        } catch (JSONException exc) {
                            final JSONArray result = new JSONArray();

                            final JSONObject object = new JSONObject();
                            object.put("result", stringBuilder.toString());

                            result.put(object);

                            return result;
                        }
                    }
                } finally { // After the task is finished, disconnect the connection
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);

                final JSONArray result = new JSONArray();

                final JSONObject statusCode = new JSONObject();

                try {
                    statusCode.put("statusCode", code);
                    Log.d("Responded With", code + "");
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }

                result.put(statusCode);

                return result;
            }
        }

        // Execute after the task is finished
        @Override
        protected void onPostExecute(JSONArray result) {
            super.onPostExecute(result);

            if (this.taskListener != null) {
                try {
                    this.taskListener.postExecute(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}