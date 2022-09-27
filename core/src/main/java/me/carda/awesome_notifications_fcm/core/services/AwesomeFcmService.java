package me.carda.awesome_notifications_fcm.core.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.MessagingAnalytics;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.carda.awesome_notifications.core.AwesomeNotifications;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications.core.utils.IntegerUtils;
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.AwesomeNotificationsFcm;
import me.carda.awesome_notifications_fcm.core.broadcasters.broadcasters.FcmBroadcaster;
import me.carda.awesome_notifications_fcm.core.interpreters.FcmInterpreter;
import me.carda.awesome_notifications_fcm.core.mocking_google.SendException;


public abstract class AwesomeFcmService extends FirebaseMessagingService {
    private static final String TAG = "AwesomeFcmService";
    private static final Queue<String> recentlyReceivedMessageIds = new ArrayDeque(10);

    public abstract void initializeExternalPlugins(Context context) throws Exception;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void handleIntent(Intent intent){
        Logger.d(TAG, "A new Awesome FCM service has started");
        try {
            initializeExternalPlugins(this);
            AwesomeNotifications.initialize(this);
            AwesomeNotificationsFcm.initialize(this);


            String action = intent.getAction();
            if (
                    !"com.google.android.c2dm.intent.RECEIVE".equals(action) &&
                            !ACTION_DIRECT_BOOT_REMOTE_INTENT.equals(action)
            ) {
                if ("com.google.firebase.messaging.NEW_TOKEN".equals(action)) {
                    this.onNewToken(intent.getStringExtra("token"));
                } else {
                    action = String.valueOf(action);
                    String errorMsg = "Unknown intent action: ";
                    if (action.length() != 0) {
                        errorMsg = errorMsg + action;
                    }

                    Logger.d("FirebaseMessaging", errorMsg);
                }
            } else {
                this.handleMessageIntent(intent);
            }

        } catch (AwesomeNotificationsException ignored) {
        } catch (Exception e) {
            ExceptionFactory
                    .getInstance()
                    .registerNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_BACKGROUND_EXECUTION_EXCEPTION,
                            "A new Awesome FCM service could not be executed",
                            ExceptionCode.DETAILED_INVALID_ARGUMENTS,
                            e);
        }
    }

    /// Called when a new token for the default Firebase project is generated.
    @Override
    public void onNewToken(@NonNull String token) {
        Logger.d(TAG, "received a new fcm token");
        FcmBroadcaster.SendBroadcastNewFcmToken(token);
    }

    @Override
    public void onMessageSent(@NonNull String message) {
        super.onMessageSent(message);
    }

    public static void printIntentExtras(Intent intent){
        printBundleExtras(intent.getExtras());
    }

    public static void printBundleExtras(Bundle bundle){
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                System.out.println(key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
            }
        }
    }

    private String getMessageIdInString(Intent intent) {
        String messageId = intent.getStringExtra("google.message_id");
        return messageId == null ? intent.getStringExtra("message_id") : messageId;
    }

    private int getMessageIdInInteger(Intent intent) {
        String messageId = intent.getStringExtra("google.message_id");

        if(!StringUtils.getInstance().isNullOrEmpty(messageId)) {
            String pattern = "^.*:(\\d{1,8})\\%";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(messageId);

            if (matcher.find())
                return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        }

        return IntegerUtils.generateNextRandomId();
    }

    private void handleMessageIntent(Intent intent) {
        String fcmMessageId = getMessageIdInString(intent);

        if(!StringUtils.getInstance().isNullOrEmpty(fcmMessageId)){
            Logger.d(TAG, "received a new fcm push (id: "+fcmMessageId+")");

            if (!this.alreadyReceivedMessage(fcmMessageId))
                this.passMessageIntentToSdk(intent);
        }
    }

    private boolean alreadyReceivedMessage(String messageId) {
        if (TextUtils.isEmpty(messageId)) {
            return false;
        } else if (recentlyReceivedMessageIds.contains(messageId)) {
            Logger.d(TAG, "Received duplicated message: " + messageId);
            return true;
        } else {
            if (recentlyReceivedMessageIds.size() >= 10) {
                recentlyReceivedMessageIds.remove();
            }

            recentlyReceivedMessageIds.add(messageId);
            return false;
        }
    }

    private void passMessageIntentToSdk(Intent intent) {
        String messageType = intent.getStringExtra("message_type");
        if (messageType == null)
            messageType = "gcm";

        switch(messageType) {

            case "deleted_messages":
                this.onDeletedMessages();
                break;

            case "send_event":
                this.onMessageSent(intent.getStringExtra("google.message_id"));
                break;

            case "gcm":
                MessagingAnalytics.logNotificationReceived(intent);
                RemoteMessage remoteMessage = FcmInterpreter.executeRemoteInstructions(this, intent);
                if(remoteMessage != null)
                    this.onMessageReceived(remoteMessage);
                break;

            case "send_error":
                messageType = this.getMessageIdInString(intent);
                SendException exception = new SendException(intent.getStringExtra("error"));
                this.onSendError(messageType, exception);
                break;

            default:
                String errorMessage = "Received message with unknown type: ";
                if (messageType.length() != 0) {
                    errorMessage += messageType;
                }
                Logger.w(TAG, errorMessage);
        }
    }
}