package com.chatnova.call;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebRTCClient {
    private static final String TAG = "WebRTCClient";
    private static final String STUN_SERVER = "stun:stun.l.google.com:19302";

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private AudioSource audioSource;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private EglBase eglBase;
    private Context context;
    private WebRTCListener listener;
    private boolean isCaller;

    public interface WebRTCListener {
        void onIceCandidate(IceCandidate candidate);
        void onLocalSdp(SessionDescription sdp);
        void onRemoteStreamAdded(MediaStream stream);
        void onConnectionStateChanged(PeerConnection.PeerConnectionState state);
        void onError(String error);
    }

    public WebRTCClient(Context context, WebRTCListener listener, EglBase eglBase) {
        this.context = context;
        this.listener = listener;
        this.eglBase = eglBase;
        initializePeerConnectionFactory();
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setFieldTrials("")
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(
                eglBase.getEglBaseContext());

        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();
    }

    public void createPeerConnection(boolean isCaller) {
        this.isCaller = isCaller;

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder(STUN_SERVER).createIceServer());

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(config, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                if (listener != null) listener.onIceCandidate(candidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {}

            @Override
            public void onSignalingChange(PeerConnection.SignalingState state) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState state) {}

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}

            @Override
            public void onAddStream(MediaStream stream) {
                // Log.d(TAG, "onAddStream (deprecated in Unified Plan)");
            }

            @Override
            public void onRemoveStream(MediaStream stream) {}

            @Override
            public void onDataChannel(DataChannel channel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(org.webrtc.RtpReceiver receiver, MediaStream[] streams) {
                Log.d(TAG, "onAddTrack");
                if (streams.length > 0 && listener != null) {
                    listener.onRemoteStreamAdded(streams[0]);
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                if (listener != null) listener.onConnectionStateChanged(newState);
            }
        });
    }

    public void startVideo(SurfaceViewRenderer localRenderer) {
        if (videoCapturer != null) {
            videoTrack.setEnabled(true);
            return;
        }
        videoCapturer = createVideoCapturer();
        if (videoCapturer == null) return;

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoSource = peerConnectionFactory.createVideoSource(false);
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        videoTrack = peerConnectionFactory.createVideoTrack("videoTrack", videoSource);
        videoTrack.addSink(localRenderer);

        peerConnection.addTrack(videoTrack, Collections.singletonList("localStream"));
    }

    public void startAudio() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        audioTrack = peerConnectionFactory.createAudioTrack("audioTrack", audioSource);
        audioTrack.setEnabled(true);

        peerConnection.addTrack(audioTrack, Collections.singletonList("localStream"));
    }

    public void createOffer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sdp);
                if (listener != null) listener.onLocalSdp(sdp);
            }
        }, constraints);
    }

    public void createAnswer() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sdp);
                if (listener != null) listener.onLocalSdp(sdp);
            }
        }, constraints);
    }

    public void setRemoteDescription(SessionDescription sdp) {
        peerConnection.setRemoteDescription(new SimpleSdpObserver(), sdp);
    }

    public void addIceCandidate(IceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    public void toggleMic(boolean enable) {
        if (audioTrack != null) audioTrack.setEnabled(enable);
    }

    public void stopVideo() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (videoTrack != null) {
            videoTrack.setEnabled(false);
        }
    }

    public void switchCamera() {
        if (videoCapturer instanceof org.webrtc.CameraVideoCapturer) {
            ((org.webrtc.CameraVideoCapturer) videoCapturer).switchCamera(null);
        }
    }

    public void cleanup() {
        Log.d(TAG, "Cleanup started");
        new Thread(() -> {
            try {
                if (videoCapturer != null) {
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException ignored) {}
                    videoCapturer.dispose();
                    videoCapturer = null;
                }
                if (surfaceTextureHelper != null) {
                    surfaceTextureHelper.dispose();
                    surfaceTextureHelper = null;
                }
                if (peerConnection != null) {
                    peerConnection.close();
                    peerConnection.dispose();
                    peerConnection = null;
                }
                if (videoSource != null) {
                    videoSource.dispose();
                    videoSource = null;
                }
                if (audioSource != null) {
                    audioSource.dispose();
                    audioSource = null;
                }
                if (peerConnectionFactory != null) {
                    peerConnectionFactory.dispose();
                    peerConnectionFactory = null;
                }
                Log.d(TAG, "Cleanup finished");
            } catch (Exception e) {
                Log.e(TAG, "Cleanup error: " + e.getMessage());
            }
        }).start();
    }

    private VideoCapturer createVideoCapturer() {
        if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator enumerator = new Camera2Enumerator(context);
            return createCapturer(enumerator);
        } else {
            Camera1Enumerator enumerator = new Camera1Enumerator(true);
            return createCapturer(enumerator);
        }
    }

    private VideoCapturer createCapturer(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();
        for (String name : deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null);
            }
        }
        for (String name : deviceNames) {
            return enumerator.createCapturer(name, null);
        }
        return null;
    }

    public SurfaceViewRenderer createSurfaceRenderer(Context context) {
        SurfaceViewRenderer renderer = new SurfaceViewRenderer(context);
        renderer.setMirror(true);
        renderer.setEnableHardwareScaler(true);
        renderer.init(eglBase.getEglBaseContext(), null);
        return renderer;
    }

    public void attachRemoteRenderer(SurfaceViewRenderer renderer) {
        // Remote stream is attached via the onAddStream callback
    }
}
