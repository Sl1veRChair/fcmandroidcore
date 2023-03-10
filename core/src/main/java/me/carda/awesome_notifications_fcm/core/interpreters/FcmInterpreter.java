package me.carda.awesome_notifications_fcm.core.interpreters;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.MessagingAnalytics;
import com.google.firebase.messaging.NotificationParams;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.carda.awesome_notifications.core.AwesomeNotifications;
import me.carda.awesome_notifications.core.builders.NotificationBuilder;
import me.carda.awesome_notifications.core.completion_handlers.NotificationThreadCompletionHandler;
import me.carda.awesome_notifications.core.enumerators.NotificationSource;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications.core.managers.CancellationManager;
import me.carda.awesome_notifications.core.models.NotificationModel;
import me.carda.awesome_notifications.core.threads.NotificationScheduler;
import me.carda.awesome_notifications.core.threads.NotificationSender;
import me.carda.awesome_notifications.core.utils.IntegerUtils;
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.FcmDefinitions;
import me.carda.awesome_notifications_fcm.core.broadcasters.broadcasters.FcmBroadcaster;
import me.carda.awesome_notifications_fcm.core.builders.FcmNotificationBuilder;
import me.carda.awesome_notifications_fcm.core.licenses.LicenseManager;
import me.carda.awesome_notifications_fcm.core.models.SilentDataModel;


public class FcmInterpreter {

    private static final String TAG = "FcmInterpreter";
    private static final Queue<String> recentlyReceivedMessageIds = new ArrayDeque(10);

    public static RemoteMessage executeRemoteInstructions(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        try {
            for (String key : extras.keySet())
                switch (key){

                    case FcmDefinitions.RPC_DISMISS:
                        dismissNotifications(context, extras.getString(key));
                        extras.remove(key);
                        break;

                    case FcmDefinitions.RPC_DISMISS_BY_CHANNEL:
                        dismissNotificationsByChannel(context, extras.getString(key));
                        extras.remove(key);
                        break;

                    case FcmDefinitions.RPC_DISMISS_BY_GROUP:
                        dismissNotificationsByGroup(context, extras.getString(key));
                        extras.remove(key);
                        break;

                    case FcmDefinitions.RPC_DISMISS_ALL:
                        dismissAllNotifications(context);
                        extras.remove(key);
                        break;
                }
            return processPushContent(context, intent);
        } catch (AwesomeNotificationsException ignored) {
        } catch (Exception exception) {
            ExceptionFactory
                    .getInstance()
                    .registerNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_UNKNOWN_EXCEPTION,
                            ExceptionCode.DETAILED_UNEXPECTED_ERROR+"."+exception.getClass().getSimpleName(),
                            exception);
        }
        return null;
    }

    private static RemoteMessage processPushContent(Context context, Intent intent) throws AwesomeNotificationsException {
        Bundle extras = intent.getExtras();
        if (extras == null)
            extras = new Bundle();

        RemoteMessage remoteMessage = new RemoteMessage(extras);

        deliveryAwesomeNotification(
                context,
                intent,
                extras,
                remoteMessage,
                new NotificationThreadCompletionHandler() {
                    @Override
                    public void handle(boolean wasDisplayed, AwesomeNotificationsException exception) {
                        if (wasDisplayed && MessagingAnalytics.shouldUploadScionMetrics(intent)) {
                            MessagingAnalytics.logNotificationForeground(intent);
                        }
                    }
                });

        return remoteMessage;
    }

    static boolean deliveryAwesomeNotification(
            @NonNull Context context,
            @NonNull Intent intent,
            @NonNull Bundle extras,
            @NonNull RemoteMessage remoteMessage,
            @NonNull NotificationThreadCompletionHandler completionHandler
    ) throws AwesomeNotificationsException {
        if (!extras.getBoolean("gcm.n.noui")) {
            int notificationId = getMessageIdInInteger(intent);

            NotificationParams notificationParams = new NotificationParams(extras);
            boolean isNotification = NotificationParams.isNotification(extras);

            if(isNotification){
                NotificationModel notificationModel =
                        FcmNotificationBuilder
                                .getNewBuilder()
                                .buildNotificationFromExtras(
                                        context,
                                        notificationId,
                                        remoteMessage,
                                        notificationParams);

                if (notificationModel.content.id == null || notificationModel.content.id < 0)
                    notificationModel.content.id = IntegerUtils.generateNextRandomId();

                notificationModel.validate(context);

                boolean isDebuggable = false;
                try {
                    isDebuggable = ( 0 != (
                            context
                            .getPackageManager()
                            .getApplicationInfo(
                                    AwesomeNotifications.getPackageName(context),
                                    ApplicationInfo.FLAG_DEBUGGABLE)
                            .flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if(
                    !isDebuggable &&
                    !LicenseManager
                        .getInstance()
                        .printValidationTest(context)
                ){
                    if(!StringUtils.getInstance().isNullOrEmpty(notificationModel.content.title))
                        notificationModel.content.title =
                                "[DEMO] "+ notificationModel.content.title;
                    else if(!StringUtils.getInstance().isNullOrEmpty(notificationModel.content.body))
                        notificationModel.content.body =
                                "[DEMO] "+ notificationModel.content.body;
                }

                receiveNotificationContent(
                        context,
                        notificationModel,
                        intent,
                        completionHandler);

                return true;
            }
            else {
                Map<String, Object> arguments = new HashMap<>(remoteMessage.getData());
                SilentDataModel silentData =
                        (SilentDataModel) new SilentDataModel()
                                                    .fromMap(arguments);
                receiveSilentDataContent(
                        context,
                        silentData,
                        completionHandler);

                return false;
            }
        }
        return false;
    }

    private static int getMessageIdInInteger(Intent intent) {
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

    private static void receiveSilentDataContent(
            @NonNull Context context,
            @NonNull SilentDataModel silentDataModel,
            @NonNull NotificationThreadCompletionHandler completionHandler
    ) throws AwesomeNotificationsException {
        if(AwesomeNotifications.debug)
            Logger.d(TAG, "New silent push received");

        FcmBroadcaster.SendBroadcastSilentData(context, silentDataModel);
    }

    private static void receiveNotificationContent(
            @NonNull Context context,
            @NonNull NotificationModel notificationModel,
            @Nullable Intent originalIntent,
            @NonNull NotificationThreadCompletionHandler completionHandler
    ) throws AwesomeNotificationsException {
        if(AwesomeNotifications.debug)
            Logger.d(TAG, "New push notification received");

        if(notificationModel.schedule == null)
            NotificationSender
                    .send(
                        context,
                        NotificationBuilder.getNewBuilder(),
                        NotificationSource.Firebase,
                        AwesomeNotifications.getApplicationLifeCycle(),
                        notificationModel,
                        originalIntent,
                        completionHandler);
        else
            NotificationScheduler
                    .schedule(
                        context,
                        NotificationSource.Firebase,
                        notificationModel,
                        completionHandler);
    }

    private static void saveOriginalIntentExtras(@NonNull Intent intent){
        Bundle bundle;
        if ((bundle = intent.getExtras()) != null) {
            Bundle originalBundle = (Bundle) bundle.clone();
            bundle.putBundle(FcmDefinitions.FIREBASE_ORIGINAL_EXTRAS, originalBundle);
            intent.replaceExtras(bundle);
        }
    }

    private static void loadOriginalIntentExtras(@NonNull Intent intent){
        Bundle bundle;
        if ((bundle = intent.getExtras()) != null) {
            Bundle originalBundle = bundle.getBundle(FcmDefinitions.FIREBASE_ORIGINAL_EXTRAS);
            if(originalBundle != null){
                intent.putExtras(originalBundle);
            }
        }
    }

    private static List<String> getListFromJsonText(String textValue){
        final String regex = "\\[?([^,\\[\\]]+)*\\]?";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(textValue);

        List<String> valuesList = new ArrayList<>();
        while (matcher.find())
            for (int i = 1; i <= matcher.groupCount(); i++)
                valuesList.add(matcher.group(i));

        return valuesList;
    }

    private static void dismissNotifications(@NonNull Context context, String textValue) throws AwesomeNotificationsException {
        if(StringUtils.getInstance().isNullOrEmpty(textValue))
            return;

        List<String> ids = getListFromJsonText(textValue);
        CancellationManager cancellationManager = CancellationManager.getInstance();

        for (String id: ids) {
            cancellationManager
                    .dismissNotification(context, Integer.parseInt(id));
        }
    }

    private static void dismissNotificationsByChannel(@NonNull Context context, String textValue) throws AwesomeNotificationsException {
        if(StringUtils.getInstance().isNullOrEmpty(textValue))
            return;

        List<String> channels = getListFromJsonText(textValue);
        CancellationManager cancellationManager = CancellationManager.getInstance();

        for (String channel: channels)
            cancellationManager
                    .dismissNotificationsByChannelKey(context, channel);
    }

    private static void dismissNotificationsByGroup(@NonNull Context context, String textValue) throws AwesomeNotificationsException {
        if(StringUtils.getInstance().isNullOrEmpty(textValue))
            return;

        List<String> groups = getListFromJsonText(textValue);
        CancellationManager cancellationManager = CancellationManager.getInstance();

        for (String group: groups)
            cancellationManager
                    .dismissNotificationsByGroupKey(context, group);
    }

    private static void dismissAllNotifications(@NonNull Context context) throws AwesomeNotificationsException {
        CancellationManager
                .getInstance()
                .dismissAllNotifications(context);
    }
}
