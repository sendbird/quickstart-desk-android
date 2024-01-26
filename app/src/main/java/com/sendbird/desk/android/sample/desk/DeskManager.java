package com.sendbird.desk.android.sample.desk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.firebase.messaging.RemoteMessage;
import com.sendbird.android.ConnectionState;
import com.sendbird.android.SendbirdChat;
import com.sendbird.android.channel.BaseChannel;
import com.sendbird.android.channel.GroupChannel;
import com.sendbird.android.exception.SendbirdException;
import com.sendbird.android.handler.GroupChannelHandler;
import com.sendbird.android.message.AdminMessage;
import com.sendbird.android.message.BaseMessage;
import com.sendbird.android.message.FileMessage;
import com.sendbird.android.message.UserMessage;
import com.sendbird.android.shadow.com.google.gson.JsonElement;
import com.sendbird.android.shadow.com.google.gson.JsonParser;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.Ticket;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.activity.chat.ChatActivity;
import com.sendbird.desk.android.sample.utils.PrefUtils;

import java.util.concurrent.ConcurrentHashMap;

public class DeskManager {

    public static final String CONNECTION_HANDLER_ID_OPEN_TICKETS = "CONNECTION_HANDLER_ID_OPEN_TICKETS";
    public static final String CONNECTION_HANDLER_ID_CLOSED_TICKETS = "CONNECTION_HANDLER_ID_CLOSED_TICKETS";
    public static final String CONNECTION_HANDLER_ID_CHAT = "CONNECTION_HANDLER_ID_CHAT";

    public static final String TICKET_HANDLER_ID_GLOBAL = "TICKET_HANDLER_ID_GLOBAL";
    public static final String TICKET_HANDLER_ID_OPEN_TICKETS = "TICKET_HANDLER_ID_OPEN_TICKETS";
    public static final String TICKET_HANDLER_ID_CHAT = "TICKET_HANDLER_ID_CHAT";


    private static DeskManager sInstance;
    private final ConcurrentHashMap<String, TicketHandler> mTicketHandlers;
    private boolean mHandlePushNotification = true;


    //+ public methods
    public synchronized static void init(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new DeskManager(context);
            sInstance.addGlobalTicketHandler(context);
        }
    }

    @NonNull
    public synchronized static DeskManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("DeskManager instance hasn't been initialized.");
        }
        return sInstance;
    }
    //- public methods


    //+ push notification
    public static void updatePushToken(@NonNull String token) {
        if (token.length() > 0) {
            PrefUtils.setPushToken(token);

            boolean pushPref = PrefUtils.getPushNotification();

            // If connection has been made and push preference is on, registers the token.
            if (SendbirdChat.getConnectionState() == ConnectionState.OPEN && pushPref) {
                SendbirdChat.registerPushToken(token, false, null);
            }
        }
    }

    public void handlePushNotification(boolean handle) {
        mHandlePushNotification = handle;
    }

    public static void showPushNotification(@NonNull Context context, @NonNull BaseChannel baseChannel, @NonNull BaseMessage baseMessage) {
        String message = "";
        String channelUrl = baseChannel.getUrl();
        String title = baseChannel.getName();

        boolean showNoti = true;

        final String nickname = baseMessage.getSender() != null ? baseMessage.getSender().getNickname() : "(Unknown)";
        if (baseMessage instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) baseMessage;
            message = nickname + ": " + userMessage.getMessage();
        } else if (baseMessage instanceof FileMessage) {
            message = nickname + ": (FILE)";
        } else if (baseMessage instanceof AdminMessage) {
            message = ((AdminMessage) baseMessage).getMessage();

            if (DeskAdminMessage.is(baseMessage)) {
                showNoti = false;
            }
        }

        if (showNoti) {
            buildNotification(context, message, title, channelUrl);
        }
    }

    public static void buildNotification(@NonNull Context context, @Nullable String message, @Nullable String title, @Nullable String channelUrl) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHANNEL_URL, channelUrl);
        intent.putExtra(ChatActivity.EXTRA_TITLE, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent((int) System.currentTimeMillis(), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        TypedArray ta = context.obtainStyledAttributes(R.style.Theme_Desk_Custom,
                new int[]{R.attr.deskNotificationIcon, R.attr.deskNotificationLargeIcon, R.attr.deskServiceName});
        int iconId = ta.getResourceId(0, R.drawable.img_notification);
        int largeIconId = ta.getResourceId(1, R.drawable.img_notification_large);
        int nameId = ta.getResourceId(2, R.string.desk_service_name);
        ta.recycle();

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        final String DESK_CHANNEL_ID = "DESK_CHANNEL_ID";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, DESK_CHANNEL_ID)
                .setSmallIcon(iconId)
                .setColor(Color.parseColor("#7469C4"))  // small icon background color
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), largeIconId))
                .setContentTitle(context.getResources().getString(nameId))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel mChannel = new NotificationChannel(DESK_CHANNEL_ID, "Desk Channel", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationManager.notify(0, notificationBuilder.build());
    }

    public static void parsePushMessage(@NonNull RemoteMessage remoteMessage, @Nullable ParsePushMessageHandler handler) {
        if (handler != null) {
            try {
                if (remoteMessage.getData().size() > 0 && remoteMessage.getData().containsKey("sendbird")) {
                    String data = remoteMessage.getData().get("sendbird");
                    if (data == null) return;
                    JsonElement payload = (JsonElement) JsonParser.parseString(data);

                    String message = remoteMessage.getData().get("message");
                    String channelName = payload.getAsJsonObject().get("channel").getAsJsonObject().get("name").getAsString();
                    String channelUrl = payload.getAsJsonObject().get("channel").getAsJsonObject().get("channel_url").getAsString();

                    handler.onResult(message, channelName, channelUrl, null);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.onResult(null, null, null, new SendbirdException("Wrong message type."));
        }
    }

    public interface ParsePushMessageHandler {
        void onResult(@Nullable String message, @Nullable String channelName, @Nullable String channelUrl, @Nullable SendbirdException e);
    }
    //- push notification


    //+ ticket methods
    public static boolean isTicketClosed(@Nullable Ticket ticket) {
        return ticket != null && ticket.getStatus2().equals("CLOSED");
    }

    @Nullable
    public static BaseMessage getLastMessage(@Nullable Ticket ticket) {
        if (ticket == null) return null;
        BaseMessage lastMessage = null;
        GroupChannel groupChannel = ticket.getChannel();
        if (groupChannel != null) {
            lastMessage = groupChannel.getLastMessage();
        }
        return lastMessage;
    }

    public static int getUnreadMessageCount(@Nullable Ticket ticket) {
        if (ticket == null) return 0;
        int unreadMessageCount = 0;
        GroupChannel groupChannel = ticket.getChannel();
        if (groupChannel != null) {
            unreadMessageCount = groupChannel.getUnreadMessageCount();
        }
        return unreadMessageCount;
    }

    public static void addTicketHandler(@Nullable String identifier, final @Nullable TicketHandler handler) {
        if (identifier == null || identifier.length() == 0 || handler == null) {
            return;
        }

        SendbirdChat.addChannelHandler(identifier, new GroupChannelHandler() {
            @Override
            public void onMessageReceived(@NonNull BaseChannel baseChannel, @NonNull BaseMessage baseMessage) {
                if (!SendBirdDesk.isDeskChannel(baseChannel)) return;

                handler.onMessageReceived(baseChannel, baseMessage);
            }

            @Override
            public void onChannelChanged(@NonNull BaseChannel channel) {
                if (!SendBirdDesk.isDeskChannel(channel)) return;

                handler.onChannelChanged(channel);
            }

            @Override
            public void onMessageUpdated(@NonNull BaseChannel channel, @NonNull BaseMessage message) {
                if (!SendBirdDesk.isDeskChannel(channel)) return;

                handler.onMessageUpdated(channel, message);
            }
        });

        getInstance().mTicketHandlers.put(identifier, handler);
    }

    @Nullable
    public static TicketHandler removeTicketHandler(@Nullable String identifier) {
        if (identifier == null || identifier.length() == 0) {
            return null;
        }

        SendbirdChat.removeChannelHandler(identifier);

        return getInstance().mTicketHandlers.remove(identifier);
    }

    public static abstract class TicketHandler {
        public abstract void onMessageReceived(@NonNull BaseChannel baseChannel, @NonNull BaseMessage baseMessage);

        public void onChannelChanged(@NonNull BaseChannel channel) {
        }

        public void onMessageUpdated(@NonNull BaseChannel channel, @NonNull BaseMessage message) {
        }
    }
    //- ticket methods


    //+ private methods
    private DeskManager(Context context) {
        mTicketHandlers = new ConcurrentHashMap<>();

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(new NetworkReceiver(), filter);
    }

    private void addGlobalTicketHandler(final Context context) {
        addTicketHandler(TICKET_HANDLER_ID_GLOBAL, new TicketHandler() {
            @Override
            public void onMessageReceived(@NonNull BaseChannel baseChannel, @NonNull BaseMessage baseMessage) {
                if (PrefUtils.isPushNotificationEnabled() && mHandlePushNotification) {
                    showPushNotification(context, baseChannel, baseMessage);
                }
            }
        });
    }
    //- private methods


    //+ network receiver
    public static class NetworkReceiver extends BroadcastReceiver {
        boolean needReconnect;

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conn = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = conn.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                if (needReconnect) {
                    needReconnect = false;

                    try {
                        SendbirdChat.reconnect();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            } else if (networkInfo == null) {
                needReconnect = true;
            }
        }
    }
    //- network receiver
}
