package com.sendbird.desk.android.sample.desk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Hashtable;

public class UrlPreviewAsyncTask {

    private static final int TIMEOUT_MILLIS = 10 * 1000;

    @Nullable
    public UrlPreviewInfo doInBackground(@NonNull String url) {
        Hashtable<String, String> result = new Hashtable<>();
        Document doc;
        try {
            doc = Jsoup.connect(url).followRedirects(true).timeout(TIMEOUT_MILLIS).get();

            Elements titleTags = doc.select("title");
            if (titleTags != null && titleTags.size() > 0) {
                result.put("title", titleTags.get(0).text());
            }

            Elements descTags = doc.select("meta[name=description]");
            if (descTags != null && descTags.size() > 0) {
                result.put("description", descTags.get(0).attr("content"));
            }

            Elements ogTags = doc.select("meta[property^=og:]");
            for (int i = 0; i < ogTags.size(); i++) {
                Element tag = ogTags.get(i);

                String text = tag.attr("property");
                if ("og:image".equals(text) || "og:image:url".equals(text)) {
                    if (!result.containsKey("image")) {
                        result.put("image", tag.attr("content"));
                    }
                } else if ("og:description".equals(text)) {
                    if (!result.containsKey("description")) {
                        result.put("description", tag.attr("content"));
                    }
                } else if ("og:title".equals(text)) {
                    if (!result.containsKey("title")) {
                        result.put("title", tag.attr("content"));
                    }
                } else if ("og:site_name".equals(text)) {
                    if (!result.containsKey("site_name")) {
                        result.put("site_name", tag.attr("content"));
                    }
                } else if ("og:url".equals(text)) {
                    if (!result.containsKey("url")) {
                        result.put("url", tag.attr("content"));
                    }
                }
            }

            Elements twitterTags = doc.select("meta[property^=twitter:]");
            for (int i = 0; i < twitterTags.size(); i++) {
                Element tag = twitterTags.get(i);

                String text = tag.attr("property");
                if ("twitter:image".equals(text)) {
                    if (!result.containsKey("image")) {
                        result.put("image", tag.attr("content"));
                    }
                } else if ("twitter:description".equals(text)) {
                    if (!result.containsKey("description")) {
                        result.put("description", tag.attr("content"));
                    }
                } else if ("twitter:title".equals(text)) {
                    if (!result.containsKey("title")) {
                        result.put("title", tag.attr("content"));
                    }
                } else if ("twitter:site".equals(text)) {
                    if (!result.containsKey("site_name")) {
                        result.put("site_name", tag.attr("content"));
                    }
                } else if ("twitter:url".equals(text)) {
                    if (!result.containsKey("url")) {
                        result.put("url", tag.attr("content"));
                    }
                }
            }

            if (!result.containsKey("url")) {
                result.put("url", url);
            }

            final String urlString = result.get("url");
            if (urlString != null && urlString.startsWith("//")) {
                result.put("url", "http:" + result.get("url"));
            }

            if (!result.containsKey("site_name") && result.get("title") != null) {
                result.put("site_name", result.get("title"));
            }

            final String imageString = result.get("image");
            if (imageString != null && imageString.startsWith("//")) {
                result.put("image", "http:" + result.get("image"));
            }

            if (result.get("url") != null && result.get("title") != null && result.get("image") != null) {
                return new UrlPreviewInfo(
                        result.get("url"),
                        result.get("site_name"),
                        result.get("title"),
                        result.get("description"),
                        result.get("image")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
