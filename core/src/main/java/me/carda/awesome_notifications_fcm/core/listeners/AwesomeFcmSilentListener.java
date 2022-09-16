package me.carda.awesome_notifications_fcm.core.listeners;

import androidx.annotation.NonNull;

import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications_fcm.core.models.SilentDataModel;

public interface AwesomeFcmSilentListener {
    public void onNewSilentDataReceived(@NonNull SilentDataModel silentReceived) throws AwesomeNotificationsException;
}
