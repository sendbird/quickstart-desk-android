package com.sendbird.desk.android.sample.desk;

import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.sample.utils.PrefUtils;

public class DeskConnectionManager {

    public static void login(final String userId, final SendBird.ConnectHandler handler) {
        SendBird.connect(userId, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(final User user, SendBirdException e) {
                if (e != null) {
                    if (handler != null) {
                        handler.onConnected(user, e);
                    }
                    return;
                }

                SendBirdDesk.authenticate(userId, null, new SendBirdDesk.AuthenticateHandler() {
                    @Override
                    public void onResult(SendBirdException e) {
                        if (handler != null) {
                            handler.onConnected(user, e);
                        }
                    }
                });
            }
        });
    }

    public static void logout(final SendBird.DisconnectHandler handler) {
        SendBird.disconnect(new SendBird.DisconnectHandler() {
            @Override
            public void onDisconnected() {
                if (handler != null) {
                    handler.onDisconnected();
                }
            }
        });
    }

    public static void addConnectionManagementHandler(String handlerId, final ConnectionManagementHandler handler) {
        SendBird.addConnectionHandler(handlerId, new SendBird.ConnectionHandler() {
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

        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            if (handler != null) {
                handler.onConnected(false);
            }
        } else if (SendBird.getConnectionState() == SendBird.ConnectionState.CLOSED) { // push notification or system kill
            final String userId = PrefUtils.getUserId();
            SendBird.connect(userId, new SendBird.ConnectHandler() {
                @Override
                public void onConnected(User user, SendBirdException e) {
                    if (e != null) {
                        return;
                    }

                    SendBirdDesk.authenticate(userId, null, new SendBirdDesk.AuthenticateHandler() {
                        @Override
                        public void onResult(SendBirdException e) {
                            if (e != null) {
                                return;
                            }

                            if (handler != null) {
                                handler.onConnected(false);
                            }
                        }
                    });
                }
            });
        }
    }

    public static void removeConnectionManagementHandler(String handlerId) {
        SendBird.removeConnectionHandler(handlerId);
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

