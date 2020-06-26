package com.sendbird.desk.android.sample.app;

import android.app.Application;

import com.sendbird.android.SendBird;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.sample.desk.DeskManager;
import com.sendbird.desk.android.sample.utils.PrefUtils;

import java.util.Map;

public class MyApplication extends Application {

    private static final String APP_ID = "52292344-3DA9-47ED-8BB1-587BB0D36F4D";    // product

    @Override
    public void onCreate() {
        super.onCreate();

        SendBird.init(APP_ID, this);
        SendBirdDesk.init();

        DeskManager.init(getApplicationContext());
        PrefUtils.init(getApplicationContext());

        Event.setEventListener(new Event.EventListener() {
            @Override
            public void onEvent(String action, Map<String, String> data) {
                String log = action;
                if (data != null) {
                    String dataString = mapToString(data);
                    if (dataString != null && dataString.length() > 0) {
                        log += " (" + dataString + ")";
                    }
                }
                android.util.Log.d("[onEvent]", log);
            }
        });
    }

    private static String mapToString(Map<String, String> map) {
        String result = "";
        if (map != null) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String key : map.keySet()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(", ");
                }
                String value = String.valueOf(map.get(key));
                stringBuilder.append((key != null ? key : ""));
                stringBuilder.append("=");
                stringBuilder.append(value != null ? value : "");
            }
            result = stringBuilder.toString();
        }
        return result;
    }
}