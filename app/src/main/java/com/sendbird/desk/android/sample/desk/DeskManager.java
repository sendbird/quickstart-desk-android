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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.firebase.messaging.RemoteMessage;
import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;
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
    public synchronized static void init(Context context) {
        if (sInstance == null) {
            sInstance = new DeskManager(context);
            sInstance.addGlobalTicketHandler(context);
        }
    }

    public synchronized static DeskManager getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("DeskManager instance hasn't been initialized.");
        }
        return sInstance;
    }
    //- public methods


    //+ push notification
    public static void updatePushToken(String token) {
        if (token != null && token.length() > 0) {
            PrefUtils.setPushToken(token);

            boolean pushPref = PrefUtils.getPushNotification();

            // If connection has been made and push preference is on, registers the token.
            if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN && pushPref) {
                SendBird.registerPushTokenForCurrentUser(token, false, null);
            }
        }
    }

    public void handlePushNotification(boolean handle) {
        mHandlePushNotification = handle;
    }

    public static void showPushNotification(Context context, BaseChannel baseChannel, BaseMessage baseMessage) {
        String message = "";
        String channelUrl = baseChannel.getUrl();
        String title = baseChannel.getName();

        boolean showNoti = true;

        if (baseMessage instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) baseMessage;
            message = userMessage.getSender().getNickname() + ": " + userMessage.getMessage();
        } else if (baseMessage instanceof FileMessage) {
            FileMessage fileMessage = (FileMessage) baseMessage;
            message = fileMessage.getSender().getNickname() + ": (FILE)";
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

    public static void buildNotification(Context context, String message, String title, String channelUrl) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHANNEL_URL, channelUrl);
        intent.putExtra(ChatActivity.EXTRA_TITLE, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ChatActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent((int) System.currentTimeMillis(), PendingIntent.FLAG_UPDATE_CURRENT);

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

    public static void parsePushMessage(RemoteMessage remoteMessage, ParsePushMessageHandler handler) {
        if (handler != null) {
            try {
                if (remoteMessage.getData().size() > 0 && remoteMessage.getData().containsKey("sendbird")) {
                    JsonElement payload = new JsonParser().parse(remoteMessage.getData().get("sendbird"));

                    String message = remoteMessage.getData().get("message");
                    String channelName = payload.getAsJsonObject().get("channel").getAsJsonObject().get("name").getAsString();
                    String channelUrl = payload.getAsJsonObject().get("channel").getAsJsonObject().get("channel_url").getAsString();

                    handler.onResult(message, channelName, channelUrl, null);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.onResult(null, null, null, new SendBirdException("Wrong message type."));
        }
    }

    public interface ParsePushMessageHandler {
        void onResult(String message, String channelName, String channelUrl, SendBirdException e);
    }
    //- push notification


    //+ ticket methods
    public static boolean isTicketClosed(Ticket ticket) {
        boolean result = false;
        if (ticket != null && ticket.getStatus().equals("CLOSED")) {
            result = true;
        }
        return result;
    }

    public static BaseMessage getLastMessage(Ticket ticket) {
        BaseMessage lastMessage = null;
        GroupChannel groupChannel = ticket.getChannel();
        if (groupChannel != null) {
            lastMessage = groupChannel.getLastMessage();
        }
        return lastMessage;
    }

    public static int getUnreadMessageCount(Ticket ticket) {
        int unreadMessageCount = 0;
        GroupChannel groupChannel = ticket.getChannel();
        if (groupChannel != null) {
            unreadMessageCount = groupChannel.getUnreadMessageCount();
        }
        return unreadMessageCount;
    }

    public static void addTicketHandler(String identifier, final TicketHandler handler) {
        if (identifier == null || identifier.length() == 0 || handler == null) {
            return;
        }

        SendBird.addChannelHandler(identifier, new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                if (!SendBirdDesk.isDeskChannel(baseChannel)) return;

                handler.onMessageReceived(baseChannel, baseMessage);
            }

            @Override
            public void onChannelChanged(BaseChannel channel) {
                if (!SendBirdDesk.isDeskChannel(channel)) return;

                handler.onChannelChanged(channel);
            }

            @Override
            public void onMessageUpdated(BaseChannel channel, BaseMessage message) {
                if (!SendBirdDesk.isDeskChannel(channel)) return;

                handler.onMessageUpdated(channel, message);
            }
        });

        getInstance().mTicketHandlers.put(identifier, handler);
    }

    public static TicketHandler removeTicketHandler(String identifier) {
        if (identifier == null || identifier.length() == 0) {
            return null;
        }

        SendBird.removeChannelHandler(identifier);

        return getInstance().mTicketHandlers.remove(identifier);
    }

    public static abstract class TicketHandler {
        public abstract void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage);

        public void onChannelChanged(BaseChannel channel) {
        }

        public void onMessageUpdated(BaseChannel channel, BaseMessage message) {
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
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                if (PrefUtils.isPushNotificationEnabled() && mHandlePushNotification) {
                    showPushNotification(context, baseChannel, baseMessage);
                }
            }
        });
    }
    //- private methods


    //+ network receiver
    public class NetworkReceiver extends BroadcastReceiver {
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
                        SendBird.reconnect();
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
