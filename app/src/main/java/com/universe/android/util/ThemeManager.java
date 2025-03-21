package com.universe.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;

import com.universe.android.R;

public class ThemeManager {
    private static final String THEME_PREF = "theme_prefs";
    private static final String CURRENT_ORG = "current_org";

    /**
     * Apply organisation theme to an activity
     */
    public static void applyOrganisationTheme(Activity activity, String orgId) {
        // Save current org ID
        SharedPreferences prefs = activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        prefs.edit().putString(CURRENT_ORG, orgId).apply();

        // Apply theme based on orgId
        if ("ntu".equals(orgId)) {
            activity.setTheme(R.style.Theme_UniVerse_NTU);
        } else {
            activity.setTheme(R.style.Theme_UniVerse);
        }
    }

    /**
     * Get current organisation ID
     */
    public static String getCurrentOrg(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        return prefs.getString(CURRENT_ORG, "");
    }

    /**
     * Add organisation logo to toolbar
     */
    public static void addLogoToToolbar(Activity activity, Toolbar toolbar, String orgId) {
        if (toolbar == null || orgId.isEmpty()) return;

        // Add organisation logo
        ImageView logoView = new ImageView(activity);
        int logoSize = (int)(32 * activity.getResources().getDisplayMetrics().density);
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(
                logoSize, logoSize, Gravity.END | Gravity.CENTER_VERTICAL);
        params.rightMargin = (int)(16 * activity.getResources().getDisplayMetrics().density);
        logoView.setLayoutParams(params);

        // Set logo based on organisation
        int logoResource = R.drawable.ic_launcher_foreground; // Default
        if ("ntu".equals(orgId)) {
            logoResource = R.drawable.ic_launcher_foreground; // Replace with actual NTU logo
        }

        logoView.setImageResource(logoResource);
        toolbar.addView(logoView);
    }
}