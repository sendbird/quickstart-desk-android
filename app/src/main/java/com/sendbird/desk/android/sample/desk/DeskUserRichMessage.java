package com.sendbird.desk.android.sample.desk;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sendbird.android.channel.GroupChannel;
import com.sendbird.android.exception.SendbirdException;
import com.sendbird.android.message.BaseMessage;
import com.sendbird.android.message.UserMessage;
import com.sendbird.android.params.UserMessageUpdateParams;
import com.sendbird.android.shadow.com.google.gson.JsonElement;
import com.sendbird.android.shadow.com.google.gson.JsonObject;
import com.sendbird.android.shadow.com.google.gson.JsonParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeskUserRichMessage {

    private static final String USER_RICH_MESSAGE_CUSTOM_TYPE = "SENDBIRD_DESK_RICH_MESSAGE";
    private static final String EVENT_TYPE_INQUIRE_CLOSURE = "SENDBIRD_DESK_INQUIRE_TICKET_CLOSURE";
    private static final String INQUIRE_CLOSURE_STATE_WAITING = "WAITING";
    private static final String INQUIRE_CLOSURE_STATE_CONFIRMED = "CONFIRMED";
    private static final String INQUIRE_CLOSURE_STATE_DECLINED = "DECLINED";
    static final String EVENT_TYPE_URL_PREVIEW = "SENDBIRD_DESK_URL_PREVIEW";


    //+ public methods
    public static boolean isInquireCloserType(@Nullable BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            JsonElement el = (JsonElement) JsonParser.parseString(((UserMessage)message).getData());
            if (!el.isJsonNull() && el.getAsJsonObject().has("type")
                    && el.getAsJsonObject().get("type").getAsString().equals(EVENT_TYPE_INQUIRE_CLOSURE)) {
                result = true;
            }
        }
        return result;
    }

    public static boolean isInquireCloserTypeWaitingState(@Nullable BaseMessage message) {
        boolean result = false;
        String state = getInquireCloserTypeState(message);
        if (state != null && state.equals(INQUIRE_CLOSURE_STATE_WAITING)) {
            result = true;
        }
        return result;
    }

    public static boolean isInquireCloserTypeConfirmedState(@Nullable BaseMessage message) {
        boolean result = false;
        String state = getInquireCloserTypeState(message);
        if (state != null && state.equals(INQUIRE_CLOSURE_STATE_CONFIRMED)) {
            result = true;
        }
        return result;
    }

    public static boolean isInquireCloserTypeDeclinedState(@Nullable BaseMessage message) {
        boolean result = false;
        String state = getInquireCloserTypeState(message);
        if (state != null && state.equals(INQUIRE_CLOSURE_STATE_DECLINED)) {
            result = true;
        }
        return result;
    }

    public static boolean isUrlPreviewType(@Nullable BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            JsonElement el = (JsonElement) JsonParser.parseString(((UserMessage)message).getData());
            if (!el.isJsonNull() && el.getAsJsonObject().has("type")
                    && el.getAsJsonObject().get("type").getAsString().equals(EVENT_TYPE_URL_PREVIEW)) {
                result = true;
            }
        }
        return result;
    }

    @Nullable
    public static UrlPreviewInfo getUrlPreviewInfo(@Nullable BaseMessage message) {
        UrlPreviewInfo info = null;
        try {
            if (message != null && isUrlPreviewType(message)) {
                info = new UrlPreviewInfo(((UserMessage)message).getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }


    public static void updateUserMessageWithUrl(final @NonNull GroupChannel channel, final @NonNull BaseMessage message, final @Nullable String text, @Nullable String url, final @Nullable UpdateUserMessageWithUrlHandler handler) {
        final ExecutorService service = Executors.newSingleThreadExecutor();
        final Handler main = new Handler(Looper.getMainLooper());
        if (url == null) return;

        service.execute(() -> {
            UrlPreviewAsyncTask task = new UrlPreviewAsyncTask();
            UrlPreviewInfo info = task.doInBackground(url);
            if (info == null) return;
            main.post(() -> {
                try {
                    String jsonString = info.toJsonString();
                    UserMessageUpdateParams params = new UserMessageUpdateParams();
                    params.setMessage(text);
                    params.setData(jsonString);
                    params.setCustomType(USER_RICH_MESSAGE_CUSTOM_TYPE);
                    channel.updateUserMessage(message.getMessageId(), params, (userMessage, e) -> {
                        if (e != null) {
                            if (handler != null) {
                                handler.onResult(null, e);
                            }
                            return;
                        }

                        if (handler != null) {
                            handler.onResult(userMessage, null);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    if (handler != null) {
                        handler.onResult(null, new SendbirdException("Url parsing error."));
                    }
                }
            });
        });
    }
    //- public methods


    //+ public handlers
    public interface UpdateUserMessageWithUrlHandler {
        void onResult(@Nullable UserMessage userMessage, @Nullable SendbirdException e);
    }
    //- public handlers


    //+ private methods
    private static boolean is(BaseMessage message) {
        return (message instanceof UserMessage && ((UserMessage) message).getCustomType().equals(USER_RICH_MESSAGE_CUSTOM_TYPE));
    }

    private static String getInquireCloserTypeState(BaseMessage message) {
        String state = null;
        if (isInquireCloserType(message)) {
            final JsonObject data = (JsonObject) JsonParser.parseString(((UserMessage)message).getData()).getAsJsonObject();
            final JsonObject body = data.get("body").getAsJsonObject();
            state = body.get("state").getAsString();
        }
        return state;
    }
    //- private methods
}
