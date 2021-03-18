package com.orbs.info.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferenceManager {

    private static final String KEY = "SharedPreference";
    private static final String DEFAULT_PERIOD = "default_period";

    private static SharedPreferences getSharedPreference(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        return sharedPref;
    }

    public static Integer getDefaultPeriod(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        if (sharedPreferences == null) {
            Log.e(Util.LOG_TAG, "Shared Preference not set.");
            return Util.defaultTxSpeed;
        } else {
            return sharedPreferences.getInt(DEFAULT_PERIOD, 3);
        }
    }

    public static void setDefaultPeriod(Context context, int days) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        if (sharedPreferences == null) {
            Log.e(Util.LOG_TAG, "Shared Preference not set.");
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(DEFAULT_PERIOD, days);
            editor.commit();
        }
    }
}
