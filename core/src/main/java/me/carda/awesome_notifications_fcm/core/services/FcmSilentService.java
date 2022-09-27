package me.carda.awesome_notifications_fcm.core.services;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import me.carda.awesome_notifications.core.AwesomeNotifications;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications_fcm.core.AwesomeNotificationsFcm;
import me.carda.awesome_notifications_fcm.core.background.FcmBackgroundExecutor;
import me.carda.awesome_notifications_fcm.core.managers.FcmDefaultsManager;

public class FcmSilentService extends JobIntentService {
    private static final String TAG = "FcmSilentService";

    @Override
    protected void onHandleWork(@NonNull final Intent intent) {
        Logger.d(TAG, "A new silent background service has started");
        try {
            Long dartCallbackHandle = getDartCallbackDispatcher(this);
            if (dartCallbackHandle == 0L) {
                ExceptionFactory
                        .getInstance()
                        .registerNewAwesomeException(
                                TAG,
                                ExceptionCode.CODE_BACKGROUND_EXECUTION_EXCEPTION,
                                "A silent background data could not be handled in Dart" +
                                " because there is no onActionReceivedMethod handler registered.",
                                ExceptionCode.DETAILED_INVALID_ARGUMENTS+".silentDataCallback");
                return;
            }

            Long silentCallbackHandle = getSilentCallbackDispatcher(this);
            if (silentCallbackHandle == 0L) {
                ExceptionFactory
                        .getInstance()
                        .registerNewAwesomeException(
                                TAG,
                                ExceptionCode.CODE_BACKGROUND_EXECUTION_EXCEPTION,
                                "A silent background data could not be handled in Dart" +
                                " because there is no dart background handler registered.",
                                ExceptionCode.DETAILED_INVALID_ARGUMENTS+".fcmSilentCallback");
                return;
            }

            FcmBackgroundExecutor.runBackgroundExecutor(
                    this,
                    intent,
                    dartCallbackHandle,
                    silentCallbackHandle);

        } catch (AwesomeNotificationsException ignore) {
        } catch (Exception e) {
            ExceptionFactory
                    .getInstance()
                    .registerNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_BACKGROUND_EXECUTION_EXCEPTION,
                            "A new silent background service could not be executed",
                            ExceptionCode.DETAILED_INVALID_ARGUMENTS,
                            e);
        }
    }

    public static Long getDartCallbackDispatcher(Context context) throws AwesomeNotificationsException {
        return FcmDefaultsManager.getDartCallbackDispatcher(context);
    }

    public static Long getSilentCallbackDispatcher(Context context) throws AwesomeNotificationsException {
        return FcmDefaultsManager.getSilentCallbackDispatcher(context);
    }
}