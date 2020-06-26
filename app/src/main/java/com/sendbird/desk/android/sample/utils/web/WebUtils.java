package com.sendbird.desk.android.sample.utils.web;

import android.util.Patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WebUtils {

    private WebUtils() {
    }

    public static List<String> extractUrls(String input) {
        List<String> result = new ArrayList<>();

        Pattern pattern = Patterns.WEB_URL;
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (pattern.matcher(word).find()) {
                if (!word.toLowerCase().contains("http://") && !word.toLowerCase().contains("https://")) {
                    word = "http://" + word;
                }
                result.add(word);
            }
        }

        return result;
    }
}