package com.sendbird.desk.android.sample.desk;

import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.shadow.com.google.gson.JsonObject;
import com.sendbird.android.shadow.com.google.gson.JsonParser;

public class DeskAdminMessage {

    private static final String ADMIN_MESSAGE_CUSTOM_TYPE = "SENDBIRD_DESK_ADMIN_MESSAGE_CUSTOM_TYPE";
    private static final String EVENT_TYPE_ASSIGN = "TICKET_ASSIGN";
    private static final String EVENT_TYPE_TRANSFER = "TICKET_TRANSFER";
    private static final String EVENT_TYPE_CLOSE = "TICKET_CLOSE";


    //+ public methods
    public static boolean is(BaseMessage message) {
        return (message != null && message instanceof AdminMessage && ((AdminMessage) message).getCustomType().equals(ADMIN_MESSAGE_CUSTOM_TYPE));
    }

    public static boolean isAssignType(BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            String data = ((AdminMessage)message).getData();
            if (data != null && data.length() > 0) {
                JsonObject dataObj = new JsonParser().parse(data).getAsJsonObject();
                String type = dataObj.get("type").getAsString();
                if (type.equals(EVENT_TYPE_ASSIGN)) {
                    result = true;
                }
            }
        }
        return result;
    }

    public static boolean isTransferType(BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            String data = ((AdminMessage)message).getData();
            if (data != null && data.length() > 0) {
                JsonObject dataObj = new JsonParser().parse(data).getAsJsonObject();
                String type = dataObj.get("type").getAsString();
                if (type.equals(EVENT_TYPE_TRANSFER)) {
                    result = true;
                }
            }
        }
        return result;
    }

    public static boolean isCloseType(BaseMessage message) {
        boolean result = false;
        if (is(message)) {
            String data = ((AdminMessage)message).getData();
            if (data != null && data.length() > 0) {
                JsonObject dataObj = new JsonParser().parse(data).getAsJsonObject();
                String type = dataObj.get("type").getAsString();
                if (type.equals(EVENT_TYPE_CLOSE)) {
                    result = true;
                }
            }
        }
        return result;
    }
    //- public methods
}
