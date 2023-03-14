package me.carda.awesome_notifications_fcm.core.broadcasters.receivers;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.carda.awesome_notifications.core.AwesomeNotifications;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.listeners.AwesomeFcmSilentListener;
import me.carda.awesome_notifications_fcm.core.listeners.AwesomeFcmTokenListener;
import me.carda.awesome_notifications_fcm.core.models.SilentDataModel;

public class AwesomeFcmEventsReceiver {

    public static String TAG = "AwesomeFcmEventsReceiver";

    // ************** SINGLETON PATTERN ***********************

    private static AwesomeFcmEventsReceiver instance;

    private AwesomeFcmEventsReceiver(StringUtils stringUtils){
        this.stringUtils = stringUtils;
    }

    public static AwesomeFcmEventsReceiver getInstance() {
        if (instance == null)
            instance = new AwesomeFcmEventsReceiver(
                    StringUtils.getInstance());
        return instance;
    }

    public boolean isFcmTokenListenersEmpty(){
        return notificationTokenListeners.isEmpty();
    }
    public boolean isFcmSilentListenersEmpty(){
        return notificationSilentListeners.isEmpty();
    }

    // ********************************************************

    protected final StringUtils stringUtils;

    /// **************  OBSERVER PATTERN  *********************

    static List<AwesomeFcmTokenListener> notificationTokenListeners = new ArrayList<>();
    public AwesomeFcmEventsReceiver subscribeOnFcmEvents(AwesomeFcmTokenListener listener) {
        notificationTokenListeners.add(listener);

        if(AwesomeNotifications.debug)
            Logger.d(TAG, listener.getClass().getSimpleName() + " subscribed to receive FCM events");

        return this;
    }
    public AwesomeFcmEventsReceiver unsubscribeOnNotificationEvents(AwesomeFcmTokenListener listener) {
        notificationTokenListeners.remove(listener);

        if(AwesomeNotifications.debug)
            Logger.d(TAG, listener.getClass().getSimpleName() + " unsubscribed from notification events");

        return this;
    }

    // ********************************************************

    static List<AwesomeFcmSilentListener> notificationSilentListeners = new ArrayList<>();
    public AwesomeFcmEventsReceiver subscribeOnFcmSilentDataEvents(AwesomeFcmSilentListener listener) {
        notificationSilentListeners.add(listener);

        if(AwesomeNotifications.debug)
            Logger.d(TAG, listener.getClass().getSimpleName() + " subscribed to receive FCM events");

        return this;
    }
    public AwesomeFcmEventsReceiver unsubscribeOnFcmSilentDataEvents(AwesomeFcmSilentListener listener) {
        notificationSilentListeners.remove(listener);

        if(AwesomeNotifications.debug)
            Logger.d(TAG, listener.getClass().getSimpleName() + " unsubscribed from notification events");

        return this;
    }

    // ********************************************************

    public void addNewFcmTokenEvent(@Nullable String token) {
        if(AwesomeNotifications.debug && notificationTokenListeners.isEmpty())
            Logger.e(TAG, "New fcm token event ignored, as there is no listeners waiting for new fcm events");

        for (AwesomeFcmTokenListener listener : notificationTokenListeners)
            listener.onNewFcmTokenReceived(token);
    }

    public void addNewNativeTokenEvent(@Nullable String token) {
        if(AwesomeNotifications.debug && notificationTokenListeners.isEmpty())
            Logger.e(TAG, "New native token event ignored, as there is no listeners waiting for new fcm events");

        for (AwesomeFcmTokenListener listener : notificationTokenListeners)
            listener.onNewNativeTokenReceived(token);
    }

    public void addNewSilentDataEvent(SilentDataModel silentData) throws AwesomeNotificationsException {
        if(AwesomeNotifications.debug && notificationSilentListeners.isEmpty())
            Logger.e(TAG, "New silent event ignored, as there is no listeners waiting for new fcm events");

        for (AwesomeFcmSilentListener listener : notificationSilentListeners)
            listener.onNewSilentDataReceived(silentData);
    }
}
