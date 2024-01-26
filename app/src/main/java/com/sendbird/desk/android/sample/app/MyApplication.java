package com.sendbird.desk.android.sample.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sendbird.android.SendbirdChat;
import com.sendbird.android.exception.SendbirdException;
import com.sendbird.android.handler.InitResultHandler;
import com.sendbird.android.params.InitParams;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.sample.desk.DeskManager;
import com.sendbird.desk.android.sample.utils.PrefUtils;

import java.util.Map;

public class MyApplication extends Application {
    public enum InitState {
        MIGRATING,
        FAILED,
        SUCCEED,
        NONE
    }

    private static final String APP_ID = "52292344-3DA9-47ED-8BB1-587BB0D36F4D";    // product
    private static MutableLiveData<InitState> initState = new MutableLiveData<>(InitState.NONE);

    @NonNull
    public static LiveData<InitState> getInitState() {
        return initState;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        InitParams params = new InitParams(APP_ID, this, false);
        SendbirdChat.init(params, new InitResultHandler() {
            @Override
            public void onMigrationStarted() {
                initState.setValue(InitState.MIGRATING);
            }
            @Override
            public void onInitFailed(@NonNull SendbirdException e) {
                initState.setValue(InitState.FAILED);
            }
            @Override
            public void onInitSucceed() {
                initState.setValue(InitState.SUCCEED);
            }
        });
        SendBirdDesk.init();

        DeskManager.init(getApplicationContext());
        PrefUtils.init(getApplicationContext());

        Event.setEventListener((action, data) -> {
            String log = action;
            if (data != null) {
                String dataString = mapToString(data);
                if (dataString != null && dataString.length() > 0) {
                    log += " (" + dataString + ")";
                }
            }
            android.util.Log.d("[onEvent]", log);
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
                stringBuilder.append(value);
            }
            result = stringBuilder.toString();
        }
        return result;
    }
}
