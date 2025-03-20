package com.universe.android.util;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Portrait-oriented QR code scanner activity
 * This is needed because the default ZXing scanner is locked to landscape mode
 */
public class CaptureActivityPortrait extends CaptureActivity {
    // This class just extends CaptureActivity, which allows the scanner to work in portrait orientation
}