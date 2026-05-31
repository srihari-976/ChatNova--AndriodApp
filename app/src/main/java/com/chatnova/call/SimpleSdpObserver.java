package com.chatnova.call;

import androidx.annotation.Nullable;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SimpleSdpObserver implements SdpObserver {
    @Override
    public void onCreateSuccess(SessionDescription sdp) {}

    @Override
    public void onSetSuccess() {}

    @Override
    public void onCreateFailure(String error) {}

    @Override
    public void onSetFailure(String error) {}
}
