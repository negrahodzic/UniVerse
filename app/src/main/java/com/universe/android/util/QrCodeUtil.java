package com.universe.android.util;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class QrCodeUtil {

    private static final String TAG = "QrCodeUtil";


    public static Bitmap generateQrCode(String content, int width, int height) {
        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage());
            return null;
        }
    }

    public static Bitmap generateQrCodeFromJson(JSONObject jsonObject, int width, int height) {
        return generateQrCode(jsonObject.toString(), width, height);
    }

    public static Bitmap generateTicketQrCode(String ticketId, String verificationCode,
                                              String eventId, int numberOfTickets,
                                              int width, int height) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", ticketId);
            jsonObject.put("code", verificationCode);
            jsonObject.put("event", eventId);
            jsonObject.put("tickets", numberOfTickets);

            return generateQrCodeFromJson(jsonObject, width, height);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for QR code: " + e.getMessage());
            return null;
        }
    }

    public static Bitmap generateFriendQrCode(String userId, String username, int width, int height) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "friend");
            jsonObject.put("userId", userId);
            jsonObject.put("username", username);

            return generateQrCodeFromJson(jsonObject, width, height);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for friend QR code: " + e.getMessage());
            return null;
        }
    }

    public static ActivityResultLauncher<ScanOptions> setupScanner(
            AppCompatActivity activity, QrScanCallback callback) {

        return activity.registerForActivityResult(new ScanContract(),
                result -> {
                    if (result.getContents() == null) {
                        // Scan was cancelled
                        callback.onScanCancelled();
                    } else {
                        // Scan completed successfully
                        callback.onScanSuccess(result.getContents());
                    }
                });
    }

    public static ActivityResultLauncher<ScanOptions> setupScanner(
            Fragment fragment, QrScanCallback callback) {

        return fragment.registerForActivityResult(new ScanContract(),
                result -> {
                    if (result.getContents() == null) {
                        // Scan was cancelled
                        callback.onScanCancelled();
                    } else {
                        // Scan completed successfully
                        callback.onScanSuccess(result.getContents());
                    }
                });
    }

    public static void startScanner(ActivityResultLauncher<ScanOptions> launcher, String promptText) {
        ScanOptions options = new ScanOptions()
                .setPrompt(promptText)
                .setBeepEnabled(true)
                .setOrientationLocked(false)
                .setCaptureActivity(CaptureActivityPortrait.class);

        launcher.launch(options);
    }

    public static JSONObject parseQrCodeJson(String scanResult) {
        try {
            return new JSONObject(scanResult);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing QR code content: " + e.getMessage());
            return null;
        }
    }

    public interface QrScanCallback {
        void onScanSuccess(String result);
        void onScanCancelled();
    }
}