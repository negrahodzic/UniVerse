package com.universe.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

public class NetworkUtil {
    private static final String TAG = "NetworkUtil";

    /**
     * Check if the device has a network connection
     * @param context Application context
     * @return True if connected, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities == null) return false;

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.d(TAG, "Network available - WiFi");
                return true;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.d(TAG, "Network available - Cellular");
                return true;
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.d(TAG, "Network available - Ethernet");
                return true;
            }

            return false;
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Test if the Docker API server is reachable by checking the events endpoint directly
     * @param context Application context
     * @param callback Callback to handle the result
     */
    public static void isDockerApiAvailable(Context context, ApiAvailabilityCallback callback) {
        // First check if any network is available
        if (!isNetworkAvailable(context)) {
            callback.onResult(false);
            return;
        }

        // Create a request to check API availability - use events endpoint instead of health
        String url = "http://172.21.141.161/api/events";

        com.android.volley.RequestQueue queue = com.android.volley.toolbox.Volley.newRequestQueue(context);
        com.android.volley.Request<String> request = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.GET,
                url,
                response -> {
                    // API is available
                    Log.d(TAG, "Docker API is available");
                    callback.onResult(true);
                },
                error -> {
                    // API is not available
                    Log.e(TAG, "Docker API is not available: " + error.getMessage());
                    callback.onResult(false);
                }
        );

        // Short timeout since this is just a connectivity check
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                3000,  // 3 seconds timeout
                0,     // no retries
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }

    public interface ApiAvailabilityCallback {
        void onResult(boolean isAvailable);
    }
}