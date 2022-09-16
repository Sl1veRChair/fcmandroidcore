package me.carda.awesome_notifications_fcm.core.listeners;

import androidx.annotation.NonNull;

public interface AwesomeFcmTokenListener {
    public void onNewFcmTokenReceived(@NonNull String token);
}
