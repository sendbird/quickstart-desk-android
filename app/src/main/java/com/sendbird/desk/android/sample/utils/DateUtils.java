package com.sendbird.desk.android.sample.utils;

import android.content.Context;

import com.sendbird.desk.android.sample.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * A class with static util methods.
 */
public class DateUtils {

    private DateUtils() {
    }

    /**
     * Gets timestamp in millis and converts it to HH:mm (e.g. 16:44).
     */
    public static String formatTime(long timeInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(timeInMillis);
    }

    /**
     * If the given time is of a different date, display the date.
     * If it is of the same date, display the time.
     *
     * @param timeInMillis The time to convert, in milliseconds.
     * @return The time or date.
     */
    public static String formatDateTime(Context context, long timeInMillis) {
        if (isToday(timeInMillis)) {
            return formatTime(timeInMillis);
        } else {
            return formatDate(context, timeInMillis);
        }
    }

    /**
     * Formats timestamp to 'date month' format (e.g. 'February 3').
     */
    public static String formatDate(Context context, long timeInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(context.getString(R.string.desk_date_format), Locale.getDefault());
        return dateFormat.format(timeInMillis);
    }

    /**
     * Returns whether the given date is today, based on the user's current locale.
     */
    public static boolean isToday(long timeInMillis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String date = dateFormat.format(timeInMillis);
        return date.equals(dateFormat.format(System.currentTimeMillis()));
    }

    /**
     * Checks if two dates are of the same day.
     *
     * @param millisFirst  The time in milliseconds of the first date.
     * @param millisSecond The time in milliseconds of the second date.
     * @return Whether {@param millisFirst} and {@param millisSecond} are off the same day.
     */
    public static boolean hasSameDate(long millisFirst, long millisSecond) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return dateFormat.format(millisFirst).equals(dateFormat.format(millisSecond));
    }
}
