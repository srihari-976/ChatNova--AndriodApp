package com.chatnova.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.chatnova.adapters.RecentConversationsAdapter;
import com.chatnova.databinding.ActivityMainBinding;
import com.chatnova.listeners.ConversionListener;
import com.chatnova.models.ChatMessage;
import com.chatnova.models.User;
import com.chatnova.utilities.Constants;
import com.chatnova.utilities.PermissionHelper;
import com.chatnova.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        loadUserDetails();
        init();
        getToken();
        setListeners();
        listenConversations();
        PermissionHelper.requestAllPermissions(this);
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        binding.imageSignOut.setOnClickListener(v -> signOut());
        binding.imageProfile.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));
        binding.fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), UsersActivity.class)));
    }

    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        String encodedImage = preferenceManager.getString(Constants.KEY_IMAGE);
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.imageProfile.setImageBitmap(bitmap);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId == null) return;

        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                .addSnapshotListener(this, eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, userId)
                .addSnapshotListener(this, eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null || value == null) {
            return;
        }
        
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        for (DocumentChange documentChange : value.getDocumentChanges()) {
            String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
            String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
            
            if (senderId == null || receiverId == null) continue;

            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.senderId = senderId;
                chatMessage.receiverId = receiverId;
                if (currentUserId != null && currentUserId.equals(senderId)) {
                    chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                    chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                    chatMessage.conversationId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                } else {
                    chatMessage.conversationImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                    chatMessage.conversationName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                    chatMessage.conversationId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                }
                chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                conversations.add(chatMessage);
            } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                for (int i = 0; i < conversations.size(); i++) {
                    if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                        conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                        conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(conversations, (obj1, obj2) -> {
            if (obj1.dateObject == null && obj2.dateObject == null) return 0;
            if (obj1.dateObject == null) return 1;
            if (obj2.dateObject == null) return -1;
            return obj2.dateObject.compareTo(obj1.dateObject);
        });
        
        conversationsAdapter.notifyDataSetChanged();
        binding.conversationsRecyclerView.smoothScrollToPosition(0);
        binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
        binding.ProgressBar.setVisibility(View.GONE);
    };

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId == null) return;

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut() {
        showToast("Signing out...");
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId == null) {
            preferenceManager.clear();
            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
            finish();
            return;
        }

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                });
    }

    @Override
    public void onConversationClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}
