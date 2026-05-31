package com.chatnova.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.chatnova.R;
import com.chatnova.activities.ChatActivity;
import com.chatnova.call.CallActivity;
import com.chatnova.models.User;
import com.chatnova.utilities.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String callType = remoteMessage.getData().get(Constants.KEY_CALL_TYPE);

        if (callType != null) {
            showCallNotification(remoteMessage, callType);
        } else {
            showChatNotification(remoteMessage);
        }
    }

    private void showChatNotification(RemoteMessage remoteMessage) {
        User user = new User();
        user.id = remoteMessage.getData().get(Constants.KEY_USER_ID);
        user.name = remoteMessage.getData().get(Constants.KEY_NAME);
        user.token = remoteMessage.getData().get(Constants.KEY_FCM_TOKEN);

        int notificationId = new Random().nextInt(10000);
        String channelId = "chat_message";
        String messageText = remoteMessage.getData().get(Constants.KEY_MESSAGE);
        String senderName = remoteMessage.getData().get(Constants.KEY_NAME);

        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.KEY_USER, user);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Reply action
        Intent replyIntent = new Intent(this, com.chatnova.firebase.NotificationReceiver.class);
        replyIntent.putExtra(Constants.KEY_USER, user);
        replyIntent.putExtra(Constants.KEY_NOTIFICATION_ID, notificationId);
        replyIntent.setAction(Constants.ACTION_REPLY);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                this, notificationId, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        RemoteInput remoteInput = new RemoteInput.Builder(Constants.KEY_NOTIFICATION_REPLY)
                .setLabel("Reply")
                .build();

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_send, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(senderName != null ? senderName : "ChatNova");
        builder.setContentText(messageText);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageText));
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.addAction(replyAction);

        createChannel(channelId, "Chat Message", NotificationManager.IMPORTANCE_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private void showCallNotification(RemoteMessage remoteMessage, String callType) {
        User user = new User();
        user.id = remoteMessage.getData().get(Constants.KEY_USER_ID);
        user.name = remoteMessage.getData().get(Constants.KEY_NAME);
        user.image = remoteMessage.getData().get(Constants.KEY_IMAGE);

        int notificationId = new Random().nextInt(10000);
        String channelId = "call_channel";
        String callId = remoteMessage.getData().get(Constants.KEY_CALL_ID);

        boolean isVideo = Constants.CALL_TYPE_VIDEO.equals(callType);

        // Accept call intent
        Intent acceptIntent = new Intent(this, CallActivity.class);
        acceptIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        acceptIntent.putExtra(Constants.KEY_CALL_ID, callId);
        acceptIntent.putExtra(Constants.KEY_CALL_TYPE, callType);
        acceptIntent.putExtra(Constants.KEY_USER, user);
        acceptIntent.putExtra("isCaller", false);
        PendingIntent acceptPendingIntent = PendingIntent.getActivity(
                this, notificationId, acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Decline call intent
        Intent declineIntent = new Intent(this, com.chatnova.firebase.NotificationReceiver.class);
        declineIntent.putExtra(Constants.KEY_CALL_ID, callId);
        declineIntent.putExtra(Constants.KEY_NOTIFICATION_ID, notificationId);
        declineIntent.setAction(Constants.ACTION_DECLINE_CALL);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                this, notificationId + 1, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = isVideo ? "Incoming Video Call" : "Incoming Voice Call";
        String content = user.name != null ? user.name + " is calling..." : "Incoming call...";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setCategory(NotificationCompat.CATEGORY_CALL);
        builder.setFullScreenIntent(acceptPendingIntent, true);
        builder.setOngoing(true);
        builder.setAutoCancel(false);
        builder.addAction(R.drawable.ic_end_call, "Decline", declinePendingIntent);
        builder.addAction(isVideo ? R.drawable.ic_video : R.drawable.ic_call, "Accept", acceptPendingIntent);

        createChannel(channelId, "Calls", NotificationManager.IMPORTANCE_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private void createChannel(String channelId, String name, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription("Notifications for " + name);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
