package com.sendbird.desk.android.sample.desk;

import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;
import com.sendbird.android.shadow.com.google.gson.JsonElement;
import com.sendbird.android.shadow.com.google.gson.JsonObject;
import com.sendbird.android.shadow.com.google.gson.JsonParser;

public class DeskUserRichMessage {

    private static final String USER_RICH_MESSAGE_CUSTOM_TYPE = "SENDBIRD_DESK_RICH_MESSAGE";
    private static final String EVENT_TYPE_INQUIRE_CLOSURE = "SENDBIRD_DESK_INQUIRE_TICKET_CLOSURE";
    private static final String INQUIRE_CLOSURE_STATE_WAITING = "WAITING";
    private static final String INQUIRE_CLOSURE_STATE_CONFIRMED = "CONFIRMED";
    private static final String INQUIRE_CLOSURE_STATE_DECLINED = "DECLINED";
    static final String EVENT_TYPE_URL_PREVIEW = "SENDBIRD_DESK_URL_PREVIEW";


    //+ public methods
    public static boolean isInquireCloserType(BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            JsonElement el = new JsonParser().parse(((UserMessage)message).getData());
            if (!el.isJsonNull() && el.getAsJsonObject().has("type")
                    && el.getAsJsonObject().get("type").getAsString().equals(EVENT_TYPE_INQUIRE_CLOSURE)) {
                result = true;
            }
        }
        return result;
    }

    public static boolean isInquireCloserTypeWaitingState(BaseMessage message) {
        boolean result = false;
        String state = getInquireCloserTypeState(message);
        if (state != null && state.equals(INQUIRE_CLOSURE_STATE_WAITING)) {
            result = true;
        }
        return result;
    }

    public static boolean isInquireCloserTypeConfirmedState(BaseMessage message) {
        boolean result = false;
        String state = getInquireCloserTypeState(message);
        if (state != null && state.equals(INQUIRE_CLOSURE_STATE_CONFIRMED)) {
            result = true;
        }
        return result;
    }

    public static boolean isInquireCloserTypeDeclinedState(BaseMessage message) {
        boolean result = false;
        String state = getInquireCloserTypeState(message);
        if (state != null && state.equals(INQUIRE_CLOSURE_STATE_DECLINED)) {
            result = true;
        }
        return result;
    }

    public static boolean isUrlPreviewType(BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            JsonElement el = new JsonParser().parse(((UserMessage)message).getData());
            if (!el.isJsonNull() && el.getAsJsonObject().has("type")
                    && el.getAsJsonObject().get("type").getAsString().equals(EVENT_TYPE_URL_PREVIEW)) {
                result = true;
            }
        }
        return result;
    }

    public static UrlPreviewInfo getUrlPreviewInfo(BaseMessage message) {
        UrlPreviewInfo info = null;
        try {
            if (isUrlPreviewType(message)) {
                info = new UrlPreviewInfo(((UserMessage)message).getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    public static void updateUserMessageWithUrl(final GroupChannel channel, final BaseMessage message, final String text, String url, final UpdateUserMessageWithUrlHandler handler) {
        new UrlPreviewAsyncTask() {
            @Override
            protected void onPostExecute(UrlPreviewInfo info) {
                try {
                    String jsonString = info.toJsonString();
                    channel.updateUserMessage(message.getMessageId(), text, jsonString,
                            USER_RICH_MESSAGE_CUSTOM_TYPE, new BaseChannel.UpdateUserMessageHandler() {
                                @Override
                                public void onUpdated(UserMessage userMessage, SendBirdException e) {
                                    if (e != null) {
                                        if (handler != null) {
                                            handler.onResult(null, e);
                                        }
                                        return;
                                    }

                                    if (handler != null) {
                                        handler.onResult(userMessage, null);
                                    }
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                    if (handler != null) {
                        handler.onResult(null, new SendBirdException("Url parsing error."));
                    }
                }
            }
        }.execute(url);
    }
    //- public methods


    //+ public handlers
    public interface UpdateUserMessageWithUrlHandler {
        void onResult(UserMessage userMessage, SendBirdException e);
    }
    //- public handlers


    //+ private methods
    private static boolean is(BaseMessage message) {
        return (message != null && message instanceof UserMessage && ((UserMessage)message).getCustomType().equals(USER_RICH_MESSAGE_CUSTOM_TYPE));
    }

    private static String getInquireCloserTypeState(BaseMessage message) {
        String state = null;
        if (isInquireCloserType(message)) {
            final JsonObject data = new JsonParser().parse(((UserMessage)message).getData()).getAsJsonObject();
            final JsonObject body = data.get("body").getAsJsonObject();
            state = body.get("state").getAsString();
        }
        return state;
    }
    //- private methods
}
