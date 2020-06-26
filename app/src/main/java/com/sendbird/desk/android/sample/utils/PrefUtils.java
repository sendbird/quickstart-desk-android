package com.sendbird.desk.android.sample.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtils {

    private static final String PREF_KEY = "PREF_KEY";
    private static final String PUSH_NOTIFICATION_PREF = "PUSH_NOTIFICATION_PREF";
    private static final String PUSH_DEVICE_TOKEN_PREF = "PUSH_DEVICE_TOKEN_PREF";
    private static final String USER_ID_PREF = "USER_ID_PREF";

    private static Context mContext;

    private PrefUtils() {
    }

    public static void init(Context context) {
        mContext = context;
    }

    public static boolean isPushNotificationEnabled() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        boolean pushPref = pref.getBoolean(PUSH_NOTIFICATION_PREF, true);
        String token = pref.getString(PUSH_DEVICE_TOKEN_PREF, "");
        return (pushPref && token.length() > 0);
    }

    public static void setPushNotification(boolean enabled) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE).edit();
        editor.putBoolean(PUSH_NOTIFICATION_PREF, enabled);
        editor.apply();
    }

    public static boolean getPushNotification() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return pref.getBoolean(PUSH_NOTIFICATION_PREF, true);
    }

    public static void setPushToken(String token) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(PUSH_DEVICE_TOKEN_PREF, token);
        editor.apply();
    }

    public static String getPushToken() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return pref.getString(PUSH_DEVICE_TOKEN_PREF, null);
    }

    public static void setUserId(String userId) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(USER_ID_PREF, userId);
        editor.apply();
    }

    public static String getUserId() {
        SharedPreferences pref = mContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return pref.getString(USER_ID_PREF, null);
    }
}
