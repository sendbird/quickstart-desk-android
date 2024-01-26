package com.sendbird.desk.android.sample.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public class Event {

    private static EventListener mEventListener = null;

    static void setEventListener(EventListener eventListener) {
        if (mEventListener != null) {
            mEventListener = eventListener;
        }
    }

    public static void onEvent(@NonNull String action, @Nullable Map<String, String> data) {
        if (mEventListener != null) {
            mEventListener.onEvent(action, data);
        }
    }

    public interface EventListener {
        void onEvent(@NonNull String action, @Nullable Map<String, String> data);

        String CHAT_ENTER = "CHAT_ENTER";   // [title:String, status:String, ticket_id:String]
        String CHAT_SEND_USER_MESSAGE = "CHAT_SEND_USER_MESSAGE";   // [message:String]
        String CHAT_ATTACH_FILE = "CHAT_ATTACH_FILE";   // [file_name:String, file_size:String, mime_type:String]
        String CHAT_DOWNLOAD_AGENT_FILE = "CHAT_DOWNLOAD_AGENT_FILE";   // [file_name:String, url:String]
        String CHAT_CONFIRM_END_OF_CHAT = "CHAT_CONFIRM_END_OF_CHAT";   // [choice:"yes" or "no"]
        String CHAT_EXIT = "CHAT_EXIT";

        String WEB_VIEWER_ENTER = "WEB_VIEWER_ENTER";   // [url:String]
        String WEB_VIEWER_RELOAD = "WEB_VIEWER_RELOAD";
        String WEB_VIEWER_EXIT = "WEB_VIEWER_EXIT";

        String PHOTO_VIEWER_ENTER = "PHOTO_VIEWER_ENTER";   // [file_name:String, file_size:String, mime_type:String]
        String PHOTO_VIEWER_DOWNLOAD_FILE = "PHOTO_VIEWER_DOWNLOAD_FILE";   // [file_name:String, url:String]
        String PHOTO_VIEWER_EXIT = "PHOTO_VIEWER_EXIT";

        String VIDEO_PLAYER_ENTER = "VIDEO_PLAYER_ENTER";   // [file_name:String, file_size:String, mime_type:String]
        String VIDEO_PLAYER_DOWNLOAD_FILE = "VIDEO_PLAYER_DOWNLOAD_FILE";   // [file_name:String, url:String]
        String VIDEO_PLAYER_EXIT = "VIDEO_PLAYER_EXIT";

        String INBOX_ENTER = "INBOX_ENTER";
        String INBOX_OPEN_TAB_SELECTED = "INBOX_OPEN_TAB_SELECTED";
        String INBOX_CLOSE_TAB_SELECTED = "INBOX_CLOSE_TAB_SELECTED";
        String INBOX_OPEN_TICKET_SELECTED = "INBOX_OPEN_TICKET_SELECTED";   // [title:String, status:String, ticket_id:String]
        String INBOX_CLOSE_TICKET_SELECTED = "INBOX_CLOSE_TICKET_SELECTED";   // [title:String, status:String, ticket_id:String]
        String INBOX_MOVE_TO_SETTINGS = "INBOX_MOVE_TO_SETTINGS";
        String INBOX_EXIT = "INBOX_EXIT";

        String SETTINGS_ENTER = "SETTINGS_ENTER";
        String SETTINGS_PUSH_ON = "SETTINGS_PUSH_ON";
        String SETTINGS_PUSH_OFF = "SETTINGS_PUSH_OFF";
        String SETTINGS_EXIT = "SETTINGS_EXIT";
    }
}
