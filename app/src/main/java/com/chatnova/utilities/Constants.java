package com.chatnova.utilities;

import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_USER_ID = "userid";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOTE_MSG_DATA = "data";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";

    public static final String KEY_MESSAGE_TYPE = "messageType";
    public static final String KEY_MESSAGE_FILE = "fileData";
    public static final String KEY_MESSAGE_FILE_NAME = "fileName";
    public static final String KEY_MESSAGE_FILE_SIZE = "fileSize";
    public static final String KEY_MESSAGE_FILE_MIME = "fileMime";
    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_VIDEO = "video";
    public static final String MESSAGE_TYPE_AUDIO = "audio";
    public static final String MESSAGE_TYPE_DOCUMENT = "document";

    public static final String KEY_COLLECTION_CALLS = "calls";
    public static final String KEY_CALL_TYPE = "callType";
    public static final String KEY_CALL_STATUS = "callStatus";
    public static final String KEY_CALLER_ID = "callerId";
    public static final String KEY_CALLEE_ID = "calleeId";
    public static final String KEY_CALL_OFFER = "offer";
    public static final String KEY_CALL_ANSWER = "answer";
    public static final String KEY_ICE_CANDIDATES = "iceCandidates";
    public static final String KEY_CALL_SDP = "sdp";
    public static final String KEY_CALL_SDP_TYPE = "sdpType";

    public static final String CALL_TYPE_AUDIO = "audio";
    public static final String CALL_TYPE_VIDEO = "video";
    public static final String CALL_STATUS_RINGING = "ringing";
    public static final String CALL_STATUS_CONNECTED = "connected";
    public static final String CALL_STATUS_ENDED = "ended";

    public static final String ACTION_REPLY = "com.chatnova.ACTION_REPLY";
    public static final String ACTION_ACCEPT_CALL = "com.chatnova.ACTION_ACCEPT_CALL";
    public static final String ACTION_DECLINE_CALL = "com.chatnova.ACTION_DECLINE_CALL";
    public static final String KEY_NOTIFICATION_REPLY = "key_notification_reply";
    public static final String KEY_CALL_ID = "callId";
    public static final String KEY_NOTIFICATION_ID = "notificationId";

    public static HashMap<String, String> remoteMsgHeaders = null;

    public static HashMap<String, String> getRemoteMsgHeaders() {
        if (remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(
                    REMOTE_MSG_AUTHORIZATION,
                    "key=69917aa0f4b0c29e9f0ce5fac49407250ec994c8"
            );
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }
}
