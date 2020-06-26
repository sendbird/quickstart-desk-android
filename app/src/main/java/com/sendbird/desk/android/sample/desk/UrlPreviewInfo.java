package com.sendbird.desk.android.sample.desk;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

public class UrlPreviewInfo {

    private final String mUrl;
    private final String mSiteName;
    private final String mTitle;
    private final String mDescription;
    private final String mImageUrl;


    UrlPreviewInfo(String url, String siteName, String title, String description, String imageUrl) {
        mUrl = url;
        mSiteName = siteName;
        mTitle = title;
        mDescription = description;
        mImageUrl = imageUrl;
    }

    UrlPreviewInfo(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject bodyObject = jsonObject.getJSONObject("body");

        mUrl = bodyObject.has("url") ? bodyObject.getString("url") : "";
        mSiteName = bodyObject.has("site_name") ? bodyObject.getString("site_name") : "";
        mTitle = bodyObject.has("title") ? bodyObject.getString("title") : "";
        mDescription = bodyObject.has("description") ? bodyObject.getString("description") : "";
        mImageUrl = bodyObject.has("image") ? bodyObject.getString("image") : "";
    }

    String toJsonString() throws JSONException {
        JSONObject bodyObject = new JSONObject();
        bodyObject.put("url", mUrl);
        bodyObject.put("site_name", mSiteName);
        bodyObject.put("title", mTitle);
        bodyObject.put("description", mDescription);
        bodyObject.put("image", mImageUrl);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", DeskUserRichMessage.EVENT_TYPE_URL_PREVIEW);
        jsonObject.put("body", bodyObject);

        return jsonObject.toString();
    }


    //+ public methods
    public String getUrl() {
        return mUrl;
    }

    public String getSiteName() {
        return mSiteName;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getDomainName() {
        String domainName = "";
        try {
            String urlString = mUrl;
            if (urlString != null && urlString.length() > 0) {
                urlString = urlString.toLowerCase();
                if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                    urlString = "http://" + urlString;
                }
            }

            URL url = new URL(urlString);
            domainName = url.getHost();
            domainName = domainName.startsWith("www.") ? domainName.substring(4) : domainName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return domainName;
    }
    //- public methods
}