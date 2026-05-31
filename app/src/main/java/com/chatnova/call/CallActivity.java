package com.chatnova.call;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chatnova.R;
import com.chatnova.databinding.ActivityCallBinding;
import com.chatnova.models.User;
import com.chatnova.utilities.Constants;
import com.chatnova.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Map;

public class CallActivity extends AppCompatActivity implements WebRTCClient.WebRTCListener {

    private ActivityCallBinding binding;
    private WebRTCClient webRTCClient;
    private String callId, callType;
    private User remoteUser;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private boolean isMicEnabled = true, isSpeakerEnabled = false, isVideoEnabled;
    private boolean isCameraEnabled = true;
    private org.webrtc.EglBase eglBase;
    private org.webrtc.SurfaceViewRenderer localRenderer, remoteRenderer;
    private DocumentReference callDocRef;
    private boolean isCaller;
    private Handler timerHandler;
    private int seconds = 0;
    private boolean isConnected = false;
    private String pendingOfferSdp = null;
    private boolean webRtcReady = false;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "CallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "setContentView finished");

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();

        callId = getIntent().getStringExtra(Constants.KEY_CALL_ID);
        callType = getIntent().getStringExtra(Constants.KEY_CALL_TYPE);
        isCaller = getIntent().getBooleanExtra("isCaller", false);
        remoteUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);

        if (callId == null || remoteUser == null) {
            finish();
            return;
        }

        isVideoEnabled = Constants.CALL_TYPE_VIDEO.equals(callType);
        isCameraEnabled = isVideoEnabled;
        binding.textCallerName.setText(remoteUser.name != null ? remoteUser.name : "Unknown");

        Log.d(TAG, "isVideoEnabled: " + isVideoEnabled + ", isCaller: " + isCaller);

        if (remoteUser.image != null) {
            try {
                byte[] bytes = Base64.decode(remoteUser.image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) binding.remoteProfileImage.setImageBitmap(bitmap);
            } catch (Exception ignored) {}
        }

        setupUiForCallRole();
        setListeners();
        checkPermissionsAndInit();
        initCallListener();
    }

    private void setupUiForCallRole() {
        if (isCaller) {
            binding.textCallStatus.setText("Calling...");
            binding.controlsContainer.setVisibility(View.VISIBLE);
            binding.incomingCallContainer.setVisibility(View.GONE);
        } else {
            binding.textCallStatus.setText("Incoming Call...");
            binding.controlsContainer.setVisibility(View.GONE);
            binding.incomingCallContainer.setVisibility(View.VISIBLE);
        }

        if (isVideoEnabled) {
            binding.videoViewLocal.setVisibility(View.VISIBLE);
            binding.videoViewRemote.setVisibility(View.VISIBLE);
            binding.buttonToggleVideo.setVisibility(View.VISIBLE);
            binding.controlsContainer.setBackgroundColor(0x00000000);
        } else {
            binding.buttonToggleVideo.setVisibility(View.GONE);
            binding.audioWaveContainer.setVisibility(View.VISIBLE);
            binding.remoteProfileImage.setVisibility(View.VISIBLE);
            binding.videoViewRemote.setVisibility(View.GONE);
            binding.videoViewLocal.setVisibility(View.GONE);
        }
    }

    private void setListeners() {
        binding.buttonEndCall.setOnClickListener(v -> endCall());
        binding.buttonToggleMic.setOnClickListener(v -> toggleMic());
        binding.buttonToggleSpeaker.setOnClickListener(v -> toggleSpeaker());
        binding.buttonToggleVideo.setOnClickListener(v -> toggleCamera());
        binding.buttonAcceptCall.setOnClickListener(v -> acceptCall());
        binding.buttonDeclineCall.setOnClickListener(v -> {
            updateCallStatus(Constants.CALL_STATUS_ENDED);
            finish();
        });
    }

    private void checkPermissionsAndInit() {
        String[] permissions;
        if (isVideoEnabled) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        } else {
            permissions = new String[]{Manifest.permission.RECORD_AUDIO};
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            initWebRTC();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions required for calls", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
            initWebRTC();
        }
    }

    private void initWebRTC() {
        Log.d(TAG, "initWebRTC started");
        eglBase = org.webrtc.EglBase.create();
        webRTCClient = new WebRTCClient(getApplicationContext(), this, eglBase);
        webRTCClient.createPeerConnection(isCaller);

        if (isVideoEnabled && isCameraEnabled) {
            localRenderer = webRTCClient.createSurfaceRenderer(this);
            remoteRenderer = webRTCClient.createSurfaceRenderer(this);

            localRenderer.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            remoteRenderer.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            binding.videoViewLocal.addView(localRenderer);
            binding.videoViewRemote.addView(remoteRenderer);
            localRenderer.setZOrderMediaOverlay(true);
            webRTCClient.startVideo(localRenderer);
            Log.d(TAG, "Renderers added and video started");
        }

        webRTCClient.startAudio();
        webRtcReady = true;
        Log.d(TAG, "initWebRTC finished");

        if (pendingOfferSdp != null) {
            processOffer(pendingOfferSdp);
            pendingOfferSdp = null;
        }

        if (isCaller) {
            new Handler().postDelayed(() -> webRTCClient.createOffer(), 500);
        }
    }

    private void processOffer(String offerSdp) {
        webRTCClient.setRemoteDescription(
                new SessionDescription(SessionDescription.Type.OFFER, offerSdp));
        webRTCClient.createAnswer();
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_CALL_STATUS, Constants.CALL_STATUS_CONNECTED);
        callDocRef.update(updates);
    }

    private void initCallListener() {
        callDocRef = database.collection(Constants.KEY_COLLECTION_CALLS).document(callId);
        callDocRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) return;

            String status = snapshot.getString(Constants.KEY_CALL_STATUS);
            if (Constants.CALL_STATUS_CONNECTED.equals(status) && !isConnected) {
                isConnected = true;
                runOnUiThread(() -> {
                    binding.textCallStatus.setText("Connected");
                    binding.controlsContainer.setVisibility(View.VISIBLE);
                    binding.incomingCallContainer.setVisibility(View.GONE);
                    startTimer();
                    startService(new Intent(this, CallService.class));
                });
            } else if (Constants.CALL_STATUS_ENDED.equals(status)) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show();
                    cleanupAndFinish();
                });
                return;
            }

            if (isCaller) {
                String answerSdp = snapshot.getString(Constants.KEY_CALL_ANSWER);
                if (answerSdp != null && webRTCClient != null) {
                    webRTCClient.setRemoteDescription(
                            new SessionDescription(SessionDescription.Type.ANSWER, answerSdp));
                }
            } else {
                String offerSdp = snapshot.getString(Constants.KEY_CALL_OFFER);
                if (offerSdp != null) {
                    if (webRTCClient != null && webRtcReady) {
                        processOffer(offerSdp);
                    } else {
                        pendingOfferSdp = offerSdp;
                    }
                }
            }

            Object iceObj = snapshot.get(Constants.KEY_ICE_CANDIDATES);
            if (iceObj instanceof Map && webRTCClient != null) {
                Map<String, Object> iceMap = (Map<String, Object>) iceObj;
                for (Map.Entry<String, Object> entry : iceMap.entrySet()) {
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> cand = (Map<String, Object>) entry.getValue();
                        String sdpMid = (String) cand.get("sdpMid");
                        int sdpMLineIndex = cand.get("sdpMLineIndex") instanceof Long
                                ? ((Long) cand.get("sdpMLineIndex")).intValue() : 0;
                        String candidate = (String) cand.get("candidate");
                        if (sdpMid != null && candidate != null) {
                            webRTCClient.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, candidate));
                        }
                    }
                }
            }
        });
    }

    private void acceptCall() {
        binding.incomingCallContainer.setVisibility(View.GONE);
        binding.controlsContainer.setVisibility(View.VISIBLE);
        binding.textCallStatus.setText("Connecting...");
        checkPermissionsAndInit();
    }

    private void toggleMic() {
        isMicEnabled = !isMicEnabled;
        if (webRTCClient != null) webRTCClient.toggleMic(isMicEnabled);
        binding.imageMic.setImageResource(isMicEnabled ? R.drawable.ic_mic : R.drawable.ic_mic_off);
    }

    private void toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled;
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) audioManager.setSpeakerphoneOn(isSpeakerEnabled);
        binding.imageSpeaker.setImageResource(
                isSpeakerEnabled ? R.drawable.ic_speaker : R.drawable.ic_speaker_off);
    }

    private void toggleCamera() {
        isCameraEnabled = !isCameraEnabled;
        if (webRTCClient != null) {
            if (isCameraEnabled) {
                if (localRenderer == null) {
                    localRenderer = webRTCClient.createSurfaceRenderer(this);
                    binding.videoViewLocal.addView(localRenderer);
                    localRenderer.setZOrderMediaOverlay(true);
                }
                if (remoteRenderer == null) {
                    remoteRenderer = webRTCClient.createSurfaceRenderer(this);
                    binding.videoViewRemote.addView(remoteRenderer);
                }
                webRTCClient.startVideo(localRenderer);
                binding.videoViewRemote.setVisibility(View.VISIBLE);
            } else {
                webRTCClient.stopVideo();
                binding.videoViewRemote.setVisibility(View.GONE);
            }
        }
        binding.videoViewLocal.setVisibility(isCameraEnabled ? View.VISIBLE : View.GONE);
        binding.imageVideo.setImageResource(
                isCameraEnabled ? R.drawable.ic_video : R.drawable.ic_video_off);
    }

    private void endCall() {
        updateCallStatus(Constants.CALL_STATUS_ENDED);
        cleanupAndFinish();
    }

    private void updateCallStatus(String status) {
        if (callDocRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put(Constants.KEY_CALL_STATUS, status);
            callDocRef.update(updates);
        }
    }

    private void startTimer() {
        timerHandler = new Handler();
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                int min = seconds / 60;
                int sec = seconds % 60;
                binding.textCallStatus.setText(String.format("%02d:%02d", min, sec));
                seconds++;
                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    private void cleanupAndFinish() {
        if (timerHandler != null) timerHandler.removeCallbacksAndMessages(null);
        if (webRTCClient != null) webRTCClient.cleanup();
        if (eglBase != null) eglBase.release();
        if (localRenderer != null) localRenderer.release();
        if (remoteRenderer != null) remoteRenderer.release();
        stopService(new Intent(this, CallService.class));
        finish();
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        if (callDocRef != null) {
            Map<String, Object> iceMap = new HashMap<>();
            iceMap.put("sdpMid", candidate.sdpMid);
            iceMap.put("sdpMLineIndex", candidate.sdpMLineIndex);
            iceMap.put("candidate", candidate.sdp);
            callDocRef.update(Constants.KEY_ICE_CANDIDATES + "." + System.currentTimeMillis(), iceMap);
        }
    }

    @Override
    public void onLocalSdp(SessionDescription sdp) {
        if (callDocRef == null) return;
        String field = sdp.type == SessionDescription.Type.OFFER
                ? Constants.KEY_CALL_OFFER : Constants.KEY_CALL_ANSWER;
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, sdp.description);
        if (sdp.type == SessionDescription.Type.OFFER) {
            updates.put(Constants.KEY_CALL_STATUS, Constants.CALL_STATUS_RINGING);
        }
        callDocRef.update(updates);
    }

    @Override
    public void onRemoteStreamAdded(MediaStream stream) {
        runOnUiThread(() -> {
            if (stream.videoTracks.size() > 0 && remoteRenderer != null) {
                VideoTrack remoteVideoTrack = stream.videoTracks.get(0);
                remoteVideoTrack.addSink(remoteRenderer);
                binding.videoViewRemote.setVisibility(View.VISIBLE);
                binding.remoteProfileImage.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onConnectionStateChanged(PeerConnection.PeerConnectionState state) {
        runOnUiThread(() -> {
            switch (state) {
                case CONNECTED:
                    if (!isConnected) {
                        isConnected = true;
                        binding.textCallStatus.setText("Connected");
                        binding.incomingCallContainer.setVisibility(View.GONE);
                        binding.controlsContainer.setVisibility(View.VISIBLE);
                        startTimer();
                        startService(new Intent(this, CallService.class));
                    }
                    break;
                case DISCONNECTED:
                case CLOSED:
                    Toast.makeText(this, "Call disconnected", Toast.LENGTH_SHORT).show();
                    cleanupAndFinish();
                    break;
                case FAILED:
                    Toast.makeText(this, "Call failed", Toast.LENGTH_SHORT).show();
                    cleanupAndFinish();
                    break;
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
        });
    }

    @Override
    protected void onDestroy() {
        cleanupAndFinish();
        super.onDestroy();
    }
}
