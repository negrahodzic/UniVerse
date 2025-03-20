package com.universe.android.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.util.Log;

public class NfcUtil {
    private static final String TAG = "NfcUtil";

    public static NfcAdapter initializeNfcAdapter(Activity activity) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            Log.d(TAG, "NFC is not available on this device");
        }
        return nfcAdapter;
    }

    public static PendingIntent createNfcPendingIntent(Activity activity) {
        return PendingIntent.getActivity(
                activity, 0,
                new Intent(activity, activity.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    public static void enableForegroundDispatch(Activity activity, NfcAdapter adapter, PendingIntent pendingIntent) {
        if (adapter != null) {
            IntentFilter tagFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] filters = new IntentFilter[] { tagFilter };
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, null);
        }
    }

    public static void disableForegroundDispatch(NfcAdapter adapter, Activity activity) {
        if (adapter != null) {
            adapter.disableForegroundDispatch(activity);
        }
    }

    public static boolean processNfcIntent(Intent intent, NfcTagCallback callback) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            // Get the tag and notify callback
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null && callback != null) {
                String serialNumber = bytesToHex(tag.getId());
                callback.onNfcTagDiscovered(tag, serialNumber);
                return true;
            }
        }
        return false;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean isNfcAvailableAndEnabled(NfcAdapter adapter) {
        return adapter != null && adapter.isEnabled();
    }

    public interface NfcTagCallback {
        void onNfcTagDiscovered(Tag tag, String serialNumber);
    }
}