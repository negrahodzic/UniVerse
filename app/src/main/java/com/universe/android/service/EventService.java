package com.universe.android.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.universe.android.model.Event;
import com.universe.android.model.Ticket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EventService {
    private static final String TAG = "EventService";

    // Mac IP address where Docker is running
    private static final String DOCKER_IP = "172.21.141.161";

    private static final String API_PORT = "8080";

    private static final String API_BASE_URL = "http://" + DOCKER_IP + ":" + API_PORT + "/api";

    private final RequestQueue requestQueue;
    private final Context context;

    public EventService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface EventCallback {
        void onSuccess(List<Event> events);

        void onError(String error);
    }

    public interface BookingCallback {
        void onSuccess(Ticket ticket);

        void onError(String error);
    }

    /**
     * Get all upcoming events from the API
     */
    public void getUpcomingEvents(final EventCallback callback) {
        String url = API_BASE_URL + "/events";
        Log.d(TAG, "Fetching events from: " + url);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            Log.d(TAG, "Received events response: " + response.toString().substring(0, Math.min(200, response.toString().length())) + "...");
                            List<Event> events = parseEventList(response);
                            callback.onSuccess(events);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing events: " + e.getMessage(), e);
                            callback.onError("Error parsing event data");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching events: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"), error);
                    }
                }
        );

        // Add longer timeout for development
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                15000,  // 15 seconds timeout
                1,       // 1 retry
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    /**
     * Book tickets for an event
     */
    public void bookEvent(String eventId, int numberOfTickets, final BookingCallback callback) {
        String url = API_BASE_URL + "/events/" + eventId + "/book";
        Log.d(TAG, "Booking event: " + url);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("numberOfTickets", numberOfTickets);

            // Add API key header
            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-KEY", "org_5102a8751b8e4258b1c0f36f5570e516");

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        try {
                            Log.d(TAG, "Booking response: " + response.toString());

                            // Parse response
                            String bookingId = response.has("bookingId")
                                    ? response.getString("bookingId")
                                    : "BOOKING-" + UUID.randomUUID().toString().substring(0, 8);

                            String verificationCode = response.has("verificationCode")
                                    ? response.getString("verificationCode")
                                    : String.format("%06d", (int) (Math.random() * 1000000));

                            // Get event details
                            getEventById(eventId, new EventCallback() {
                                @Override
                                public void onSuccess(List<Event> events) {
                                    if (events.size() > 0) {
                                        Event event = events.get(0);

                                        // Create ticket object
                                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                                        String purchaseDate = sdf.format(new Date());

                                        Ticket ticket = new Ticket(
                                                bookingId,
                                                event,
                                                purchaseDate,
                                                verificationCode,
                                                false
                                        );

                                        // Set ticket quantity and price
                                        ticket.setNumberOfTickets(numberOfTickets);
                                        ticket.setPointsPrice(event.getPointsPrice());

                                        // Save ticket to user's Firestore collection
                                        saveTicketToFirestore(ticket, numberOfTickets, event.getPointsPrice(), () -> {
                                            callback.onSuccess(ticket);
                                        });
                                    } else {
                                        callback.onError("Could not find event details");
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    callback.onError("Error retrieving event: " + error);
                                }
                            });
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing booking response: " + e.getMessage(), e);
                            callback.onError("Error processing booking");
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error booking event: " +
                                (error.getMessage() != null ? error.getMessage() : "Unknown error"), error);
                        callback.onError("Error connecting to booking service: " + error.getMessage());
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }
            };

            // Add longer timeout for development
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    15000,  // 15 seconds timeout
                    1,       // 1 retry
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating booking request: " + e.getMessage(), e);
            callback.onError("Error creating booking request");
        }
    }

    /**
     * Save ticket to user's Firestore collection
     */
    private void saveTicketToFirestore(Ticket ticket, int numberOfTickets, int pointsPrice, Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in, cannot save ticket");
            return;
        }

        // Create ticket data for Firestore
        Map<String, Object> ticketData = new HashMap<>();
        ticketData.put("eventId", ticket.getEvent().getId());
        ticketData.put("purchaseDate", ticket.getPurchaseDate());
        ticketData.put("verificationCode", ticket.getVerificationCode());
        ticketData.put("used", ticket.isUsed());
        ticketData.put("numberOfTickets", numberOfTickets);
        ticketData.put("pointsPrice", pointsPrice);
        ticketData.put("totalPointsSpent", numberOfTickets * pointsPrice);
        ticketData.put("purchaseTimestamp", new Date());

        // Add event details for quick access without requiring a join
        ticketData.put("eventTitle", ticket.getEvent().getTitle());
        ticketData.put("eventDate", ticket.getEvent().getDate());
        ticketData.put("eventTime", ticket.getEvent().getTime());
        ticketData.put("eventLocation", ticket.getEvent().getLocation());
        ticketData.put("eventAddress", ticket.getEvent().getAddress());

        // Save to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(currentUser.getUid())
                .collection("tickets")
                .document(ticket.getId())
                .set(ticketData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ticket saved to Firestore");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving ticket: " + e.getMessage());
                });
    }

    /**
     * Get details for a specific event
     */
    public void getEventById(String eventId, final EventCallback callback) {
        String url = API_BASE_URL + "/events/" + eventId;
        Log.d(TAG, "Fetching event details: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Event event = parseEvent(response);
                            List<Event> events = new ArrayList<>();
                            events.add(event);
                            callback.onSuccess(events);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing event: " + e.getMessage(), e);
                            callback.onError("Error parsing event details");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching event: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"), error);
                        callback.onError("Error fetching event details");
                    }
                }
        );

        requestQueue.add(request);
    }

    /**
     * Parse a list of events from JSON response
     */
    private List<Event> parseEventList(JSONArray jsonArray) throws JSONException {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject eventJson = jsonArray.getJSONObject(i);
            events.add(parseEvent(eventJson));
        }

        return events;
    }

    /**
     * Parse a single event from JSON
     */
    private Event parseEvent(JSONObject eventJson) throws JSONException {
        Log.d(TAG, "Parsing event: " + eventJson.toString().substring(0, Math.min(200, eventJson.toString().length())) + "...");

        String id = eventJson.getString("eventId");
        String title = eventJson.getString("eventName");

        // Get description if available
        String description = eventJson.has("description") ?
                eventJson.getString("description") :
                "No description available";

        // Parse date and time
        String dateTimeStr = eventJson.getString("eventDateTime");
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        SimpleDateFormat displayTimeFormat = new SimpleDateFormat("h:mm a", Locale.US);

        String date = "";
        String time = "";
        try {
            Date dateObj = apiFormat.parse(dateTimeStr);
            date = displayDateFormat.format(dateObj);
            time = displayTimeFormat.format(dateObj);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage(), e);
            date = dateTimeStr; // Use raw value as fallback
        }

        // Parse location information
        JSONObject venueJson = eventJson.has("venue") ? eventJson.getJSONObject("venue") : null;
        String location = venueJson != null ? venueJson.getString("name") : "TBD";
        String address = venueJson != null ? venueJson.getString("address") : "Address unavailable";

        // Get coordinates if available
        double latitude = venueJson != null && venueJson.has("latitude") ?
                venueJson.getDouble("latitude") : 0.0;
        double longitude = venueJson != null && venueJson.has("longitude") ?
                venueJson.getDouble("longitude") : 0.0;

        int pointsPrice = eventJson.has("ticketPrice") ?
                (int) eventJson.getDouble("ticketPrice") :
                500; // Default points price

        int availableTickets = eventJson.has("availableTickets") ?
                eventJson.getInt("availableTickets") :
                10; // Default available tickets

        Event event = new Event(
                id, title, description, date, time, location, address,
                latitude, longitude, pointsPrice, availableTickets
        );

        // Add organizer if available
        if (eventJson.has("organizer")) {
            event.setOrganizer(eventJson.getString("organizer"));
        }

        return event;
    }

}