package com.sendbird.desk.android.sample.desk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sendbird.android.ConnectionState;
import com.sendbird.android.SendbirdChat;
import com.sendbird.android.handler.ConnectHandler;
import com.sendbird.android.handler.ConnectionHandler;
import com.sendbird.android.handler.DisconnectHandler;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.sample.utils.PrefUtils;

public class DeskConnectionManager {

    public static void login(final @NonNull String userId, final @Nullable ConnectHandler handler) {
        SendbirdChat.connect(userId, (user, e) -> {
            if (e != null) {
                if (handler != null) {
                    handler.onConnected(user, e);
                }
                return;
            }

            SendBirdDesk.authenticate(userId, null, e1 -> {
                if (handler != null) {
                    handler.onConnected(user, e1);
                }
            });
        });
    }

    public static void logout(final @Nullable DisconnectHandler handler) {
        String token = PrefUtils.getPushToken();
        if (token == null) {
           disconnect(handler);
        } else {
            SendbirdChat.unregisterPushToken(token, e -> disconnect(handler));
        }
    }

    private static void disconnect(final @Nullable DisconnectHandler handler) {
        SendbirdChat.disconnect(() -> {
            if (handler != null) {
                handler.onDisconnected();
            }
        });
    }

    public static void addConnectionManagementHandler(@NonNull String handlerId, final @Nullable ConnectionManagementHandler handler) {
        SendbirdChat.addConnectionHandler(handlerId, new ConnectionHandler() {
            @Override
            public void onDisconnected(@NonNull String s) {

            }

            @Override
            public void onConnected(@NonNull String s) {

            }

            @Override
            public void onReconnectStarted() {
            }

            @Override
            public void onReconnectSucceeded() {
                if (handler != null) {
                    handler.onConnected(true);
                }
            }

            @Override
            public void onReconnectFailed() {
            }
        });

        if (SendbirdChat.getConnectionState() == ConnectionState.OPEN) {
            if (handler != null) {
                handler.onConnected(false);
            }
        } else if (SendbirdChat.getConnectionState() == ConnectionState.CLOSED) { // push notification or system kill
            final String userId = PrefUtils.getUserId();
            SendbirdChat.connect(userId, (user, e) -> {
                if (e != null) {
                    return;
                }

                SendBirdDesk.authenticate(userId, null, e1 -> {
                    if (e1 != null) {
                        return;
                    }

                    if (handler != null) {
                        handler.onConnected(false);
                    }
                });
            });
        }
    }

    public static void removeConnectionManagementHandler(@NonNull String handlerId) {
        SendbirdChat.removeConnectionHandler(handlerId);
    }

    public interface ConnectionManagementHandler {
        /**
         * A callback for when connected or reconnected to refresh.
         *
         * @param reconnect Set false if connected, true if reconnected.
         */
        void onConnected(boolean reconnect);
    }
}

