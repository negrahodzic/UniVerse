package com.universe.android.repository;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.universe.android.R;
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

public class EventRepository extends FirebaseRepository {
    private static final String TAG = "EventRepository";
    private static final String DOCKER_IP = "172.22.140.90";
    private static final String API_PORT = "8080";
    private static final String API_BASE_URL = "http://" + DOCKER_IP + ":" + API_PORT + "/api";

    private static EventRepository instance;
    private final RequestQueue requestQueue;
    private final Context context;

    private EventRepository(Context context) {
        super();
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    public static synchronized EventRepository getInstance(Context context) {
        if (instance == null) {
            instance = new EventRepository(context);
        }
        return instance;
    }

    public interface EventCallback {
        void onSuccess(List<Event> events);
        void onError(String error);
    }

    public interface BookingCallback {
        void onSuccess(Ticket ticket);
        void onError(String error);
    }

    public void getUpcomingEvents(final EventCallback callback) {
        String url = API_BASE_URL + "/events";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
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
                        Log.e(TAG, "Error fetching events: " +
                                (error.getMessage() != null ? error.getMessage() : "Unknown error"), error);
                        callback.onError("Error connecting to event service");
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    public void getEventById(String eventId, final EventCallback callback) {
        String url = API_BASE_URL + "/events/" + eventId;

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
                        Log.e(TAG, "Error fetching event: " +
                                (error.getMessage() != null ? error.getMessage() : "Unknown error"), error);
                        callback.onError("Error fetching event details");
                    }
                }
        );

        requestQueue.add(request);
    }

    public void bookEvent(String eventId, int numberOfTickets, final BookingCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        String url = API_BASE_URL + "/events/" + eventId + "/book";

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("numberOfTickets", numberOfTickets);

            Map<String, String> headers = new HashMap<>();
            headers.put("X-API-KEY", "org_5102a8751b8e4258b1c0f36f5570e516");

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        try {
                            String bookingId = response.has("bookingId")
                                    ? response.getString("bookingId")
                                    : "BOOKING-" + UUID.randomUUID().toString().substring(0, 8);

                            String verificationCode = response.has("verificationCode")
                                    ? response.getString("verificationCode")
                                    : String.format("%06d", (int) (Math.random() * 1000000));

                            getEventById(eventId, new EventCallback() {
                                @Override
                                public void onSuccess(List<Event> events) {
                                    if (events.size() > 0) {
                                        Event event = events.get(0);

                                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                                        String purchaseDate = sdf.format(new Date());

                                        Ticket ticket = new Ticket(
                                                bookingId,
                                                event,
                                                purchaseDate,
                                                verificationCode,
                                                false
                                        );

                                        ticket.setNumberOfTickets(numberOfTickets);
                                        ticket.setPointsPrice(event.getPointsPrice());

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

            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000,
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating booking request: " + e.getMessage(), e);
            callback.onError("Error creating booking request");
        }
    }

    private void saveTicketToFirestore(Ticket ticket, int numberOfTickets, int pointsPrice, Runnable onComplete) {
        if (!isLoggedIn()) {
            Log.e(TAG, "No user logged in, cannot save ticket");
            return;
        }

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

        db.collection("users")
                .document(getCurrentUserId())
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

    public Task<List<Ticket>> getUserTickets() {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        return db.collection("users")
                .document(getCurrentUserId())
                .collection("tickets")
                .orderBy("purchaseTimestamp", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<Ticket> tickets = new ArrayList<>();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String ticketId = document.getId();
                            String eventId = document.getString("eventId");
                            String purchaseDate = document.getString("purchaseDate");
                            String verificationCode = document.getString("verificationCode");
                            boolean isUsed = document.getBoolean("used") != null ?
                                    document.getBoolean("used") : false;

                            // Get stored event details
                            String eventTitle = document.getString("eventTitle");
                            String eventDate = document.getString("eventDate");
                            String eventTime = document.getString("eventTime");
                            String eventLocation = document.getString("eventLocation");
                            String eventAddress = document.getString("eventAddress");

                            // Create a basic event with the stored details
                            Event basicEvent = new Event(
                                    eventId,
                                    eventTitle != null ? eventTitle : "Unknown Event",
                                    "Loading event details...",
                                    eventDate != null ? eventDate : "Unknown date",
                                    eventTime != null ? eventTime : "",
                                    eventLocation != null ? eventLocation : "Unknown location",
                                    eventAddress != null ? eventAddress : "Unknown address",
                                    0,
                                    R.drawable.ic_launcher_foreground
                            );

                            // Create ticket with basic event details
                            Ticket ticket = new Ticket(
                                    ticketId, basicEvent, purchaseDate, verificationCode, isUsed);

                            // Add additional ticket details if available
                            if (document.getLong("numberOfTickets") != null) {
                                ticket.setNumberOfTickets(document.getLong("numberOfTickets").intValue());
                            }

                            if (document.getLong("pointsPrice") != null) {
                                ticket.setPointsPrice(document.getLong("pointsPrice").intValue());
                            }

                            tickets.add(ticket);
                        }
                    }

                    return tickets;
                });
    }

    public Task<Void> markTicketAsUsed(String ticketId) {
        if (!isLoggedIn()) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        DocumentReference ticketRef = db.collection("users")
                .document(getCurrentUserId())
                .collection("tickets")
                .document(ticketId);

        return ticketRef.update("used", true);
    }

    private List<Event> parseEventList(JSONArray jsonArray) throws JSONException {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject eventJson = jsonArray.getJSONObject(i);
            events.add(parseEvent(eventJson));
        }

        return events;
    }

    private Event parseEvent(JSONObject eventJson) throws JSONException {
        String id = eventJson.getString("eventId");
        String title = eventJson.getString("eventName");

        String description = eventJson.has("description") ?
                eventJson.getString("description") :
                "No description available";

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
            date = dateTimeStr;
        }

        JSONObject venueJson = eventJson.has("venue") ? eventJson.getJSONObject("venue") : null;
        String location = venueJson != null ? venueJson.getString("name") : "TBD";
        String address = venueJson != null ? venueJson.getString("address") : "Address unavailable";

        double latitude = venueJson != null && venueJson.has("latitude") ?
                venueJson.getDouble("latitude") : 0.0;
        double longitude = venueJson != null && venueJson.has("longitude") ?
                venueJson.getDouble("longitude") : 0.0;

        int pointsPrice = eventJson.has("ticketPrice") ?
                (int) eventJson.getDouble("ticketPrice") :
                500;

        int availableTickets = eventJson.has("availableTickets") ?
                eventJson.getInt("availableTickets") :
                10;

        Event event = new Event(
                id, title, description, date, time, location, address,
                latitude, longitude, pointsPrice, availableTickets
        );

        if (eventJson.has("organizer")) {
            event.setOrganizer(eventJson.getString("organizer"));
        }

        return event;
    }
}