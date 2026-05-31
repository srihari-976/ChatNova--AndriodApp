package com.chatnova.firebase;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.app.RemoteInput;

import com.chatnova.models.User;
import com.chatnova.utilities.Constants;
import com.chatnova.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Constants.ACTION_REPLY.equals(action)) {
            handleReply(context, intent);
        } else if (Constants.ACTION_DECLINE_CALL.equals(action)) {
            handleDeclineCall(context, intent);
        }
    }

    private void handleReply(Context context, Intent intent) {
        Bundle results = RemoteInput.getResultsFromIntent(intent);
        if (results == null) return;

        String replyText = results.getString(Constants.KEY_NOTIFICATION_REPLY);
        if (replyText == null || replyText.isEmpty()) return;

        PreferenceManager preferenceManager = new PreferenceManager(context);
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        String currentName = preferenceManager.getString(Constants.KEY_NAME);
        String currentImage = preferenceManager.getString(Constants.KEY_IMAGE);

        if (currentUserId == null) return;

        User receiverUser = (User) intent.getSerializableExtra(Constants.KEY_USER);
        if (receiverUser == null) return;

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, currentUserId);
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, replyText);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        // Update conversation
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, currentUserId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String convId = query.getDocuments().get(0).getId();
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                .document(convId)
                                .update(Constants.KEY_LAST_MESSAGE, replyText,
                                        Constants.KEY_TIMESTAMP, new Date());
                    }
                });

        dismissNotification(context, intent);
    }

    private void handleDeclineCall(Context context, Intent intent) {
        String callId = intent.getStringExtra(Constants.KEY_CALL_ID);
        if (callId != null) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            HashMap<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_CALL_STATUS, Constants.CALL_STATUS_ENDED);
            database.collection(Constants.KEY_COLLECTION_CALLS).document(callId)
                    .update(updates);
        }
        dismissNotification(context, intent);
    }

    private void dismissNotification(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(Constants.KEY_NOTIFICATION_ID, 0);
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId);
        }
    }
}
