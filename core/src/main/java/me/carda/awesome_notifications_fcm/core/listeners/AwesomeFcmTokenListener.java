package me.carda.awesome_notifications_fcm.core.listeners;

import androidx.annotation.Nullable;

public interface AwesomeFcmTokenListener {
    void onNewFcmTokenReceived(@Nullable String token);
    void onNewNativeTokenReceived(@Nullable String token);
}
