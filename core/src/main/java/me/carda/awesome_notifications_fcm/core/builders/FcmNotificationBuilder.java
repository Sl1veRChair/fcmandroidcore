package me.carda.awesome_notifications_fcm.core.builders;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.reflect.TypeToken;
import com.google.firebase.messaging.NotificationParams;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.carda.awesome_notifications.core.Definitions;
import me.carda.awesome_notifications.core.enumerators.NotificationLayout;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.managers.ChannelManager;
import me.carda.awesome_notifications.core.models.NotificationModel;
import me.carda.awesome_notifications.core.utils.JsonUtils;
import me.carda.awesome_notifications.core.utils.ListUtils;
import me.carda.awesome_notifications.core.utils.MapUtils;
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.FcmDefinitions;
import me.carda.awesome_notifications_fcm.core.models.SilentDataModel;


public class FcmNotificationBuilder {

    public static final String TAG = "FcmNotificationBuilder";

    StringUtils stringUtils;

    // *************** DEPENDENCY INJECTION CONSTRUCTOR ***************

    FcmNotificationBuilder(
            @NonNull StringUtils stringUtils
    ){
        this.stringUtils = stringUtils;
    }

    public static FcmNotificationBuilder getNewBuilder() {
        return new FcmNotificationBuilder(
                StringUtils.getInstance());
    }

    // ****************************************************************

    public NotificationModel buildNotificationFromExtras(
            @NonNull Context context,
            @NonNull int notificationId,
            @NonNull RemoteMessage remoteMessage,
            @NonNull NotificationParams notificationParams
    ) throws AwesomeNotificationsException {

        Map<String, String> remoteData = remoteMessage.getData();
        Map<String, Object> awesomeData, androidCustomData = null;

        remoteData.remove(FcmDefinitions.NOTIFICATION_MODEL_IOS);

        if(remoteData.containsKey(Definitions.NOTIFICATION_MODEL_CONTENT)) {
            if (remoteData.containsKey(FcmDefinitions.NOTIFICATION_MODEL_ANDROID)) {
                androidCustomData = JsonUtils.fromJson(remoteData.get(FcmDefinitions.NOTIFICATION_MODEL_ANDROID));
            }
            awesomeData = receiveAwesomeNotificationContent(notificationId, remoteMessage, remoteData);
            if (androidCustomData != null) {
                awesomeData = new HashMap<String, Object>(MapUtils.deepMerge(awesomeData, androidCustomData));
            }
        }
        else {
            awesomeData = receiveStandardNotificationContent(notificationId, context, remoteMessage);
        }

        NotificationModel notificationModel = new NotificationModel();
        return notificationModel.fromMap(awesomeData);
    }

    private Map<String, Object> receiveAwesomeNotificationContent(
            @NonNull int notificationId,
            @NonNull RemoteMessage remoteMessage,
            @NonNull Map<String, String> remoteData
    ) throws AwesomeNotificationsException {
        RemoteMessage.Notification remoteNotification = remoteMessage.getNotification();
        Map<String, String> localRemoteData = new HashMap<>(remoteData);

        Map<String, Object> parsedNotificationContent =
                extractNotificationData(Definitions.NOTIFICATION_MODEL_CONTENT, localRemoteData);
        Map<String, Object> parsedSchedule =
                extractNotificationData(Definitions.NOTIFICATION_MODEL_SCHEDULE, localRemoteData);
        List<Map<String, Object>> parsedActionButtons =
                extractNotificationDataList(Definitions.NOTIFICATION_MODEL_BUTTONS, localRemoteData);
        Map<String, Object> parsedLocalizations =
                extractNotificationData(Definitions.NOTIFICATION_MODEL_LOCALIZATIONS, localRemoteData);

        Map<String, Object> originalNotificationData =
                extractFcmNotificationIntoAwesome(notificationId, remoteMessage, remoteNotification);

        parsedNotificationContent = MapUtils.deepMerge(
                parsedNotificationContent,
                originalNotificationData);

        Map<String, Object> parsedRemoteMessage = new HashMap<>();
        parsedRemoteMessage.put(Definitions.NOTIFICATION_MODEL_CONTENT, parsedNotificationContent);

        if(!MapUtils.isNullOrEmpty(parsedSchedule))
            parsedRemoteMessage.put(Definitions.NOTIFICATION_MODEL_SCHEDULE, parsedSchedule);

        if(!ListUtils.isNullOrEmpty(parsedActionButtons))
            parsedRemoteMessage.put(Definitions.NOTIFICATION_MODEL_BUTTONS, parsedActionButtons);

        if(!MapUtils.isNullOrEmpty(parsedLocalizations))
            parsedRemoteMessage.put(Definitions.NOTIFICATION_MODEL_LOCALIZATIONS, parsedLocalizations);

        return parsedRemoteMessage;
    }

    private Map<String, Object>  receiveStandardNotificationContent(
            @NonNull int notificationId,
            @NonNull Context context,
            @NonNull RemoteMessage remoteMessage
    ) throws AwesomeNotificationsException {
        RemoteMessage.Notification remoteNotification = remoteMessage.getNotification();

        Map<String, Object> contentData =
                extractFcmNotificationIntoAwesome(notificationId, remoteMessage, remoteNotification);

        if (!contentData.containsKey(Definitions.NOTIFICATION_CHANNEL_KEY)){
            String channelKey = getFirstAvailableChannelKey(context);
            contentData.put(Definitions.NOTIFICATION_CHANNEL_KEY, channelKey);
        }

        Map<String, String> payload = remoteMessage.getData();
        if(!payload.isEmpty()){
            contentData.put(Definitions.NOTIFICATION_PAYLOAD, payload);
        }

        Map<String, Object> parsedRemoteMessage = new HashMap<>();
        parsedRemoteMessage.put(Definitions.NOTIFICATION_MODEL_CONTENT, contentData);
        return parsedRemoteMessage;
    }

    private String getFirstAvailableChannelKey(Context context) throws AwesomeNotificationsException {
        return ChannelManager
                .getInstance()
                .listChannels(context)
                .get(0)
                .channelKey;
    }

    @NonNull
    private Map<String, Object> extractFcmNotificationIntoAwesome(
            @Nullable int notificationId,
            @Nullable RemoteMessage remoteMessage,
            @Nullable RemoteMessage.Notification remoteNotification
    ){
        Map<String, Object> parsedData = new HashMap<>();

        if(remoteMessage == null)
            return parsedData;

        if(remoteNotification != null){

            parsedData.put(Definitions.NOTIFICATION_ID, notificationId);

            if(!stringUtils.isNullOrEmpty(remoteNotification.getChannelId()))
                parsedData.put(
                        Definitions.NOTIFICATION_CHANNEL_KEY, remoteNotification.getChannelId());

            if(!stringUtils.isNullOrEmpty(remoteNotification.getTitle()))
                parsedData.put(
                        Definitions.NOTIFICATION_TITLE, remoteNotification.getTitle());

            if(!stringUtils.isNullOrEmpty(remoteNotification.getBody()))
                parsedData.put(
                        Definitions.NOTIFICATION_BODY, remoteNotification.getBody());

            if(!stringUtils.isNullOrEmpty(remoteNotification.getTicker()))
                parsedData.put(
                        Definitions.NOTIFICATION_TICKER, remoteNotification.getTicker());

            parsedData.put(Definitions.NOTIFICATION_AUTO_DISMISSIBLE, !remoteNotification.getSticky());

            Uri imageUri = remoteNotification.getImageUrl();
            if(imageUri != null) {
                parsedData.put(Definitions.NOTIFICATION_BIG_PICTURE, imageUri.toString());
                parsedData.put(Definitions.NOTIFICATION_LAYOUT, NotificationLayout.BigPicture.getSafeName());
                parsedData.put(Definitions.NOTIFICATION_HIDE_LARGE_ICON_ON_EXPAND, true);
            }
        }

        Map<String, String> payload = remoteMessage.getData();

        payload.remove(Definitions.NOTIFICATION_MODEL_CONTENT);
        payload.remove(Definitions.NOTIFICATION_MODEL_SCHEDULE);
        payload.remove(Definitions.NOTIFICATION_MODEL_BUTTONS);
        payload.remove(Definitions.NOTIFICATION_MODEL_LOCALIZATIONS);
        payload.remove(FcmDefinitions.NOTIFICATION_MODEL_ANDROID);
        payload.remove(FcmDefinitions.NOTIFICATION_MODEL_IOS);

        parsedData.put(Definitions.NOTIFICATION_PAYLOAD, payload);

        return parsedData;
    }

    private Map<String, Object> extractNotificationData(
            @NonNull String reference,
            @NonNull Map<String, String> remoteData
    ) throws AwesomeNotificationsException {
        String jsonData = remoteData.get(reference);
        Map<String, Object> notification = null;
        try {
            if (jsonData != null) {
                notification = JsonUtils.fromJson(jsonData);
            }
        } catch (Exception exception) {
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_INVALID_ARGUMENTS,
                            "Invalid Firebase notification content "+reference,
                            ExceptionCode.DETAILED_INVALID_ARGUMENTS+".fcm.extractNotificationData",
                            exception);
        }
        return notification;
    }

    private List<Map<String, Object>> extractNotificationDataList(
            @NonNull String reference,
            @NonNull Map<String, String> remoteData
    ) throws AwesomeNotificationsException {
        String jsonData = remoteData.get(reference);
        List<Map<String, Object>> list = null;
        try {
            if (jsonData != null) {
                Type mapType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                list = new Gson().fromJson(jsonData, mapType);
            }
        } catch (Exception exception) {
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_INVALID_ARGUMENTS,
                            "Invalid Firebase notification content "+reference,
                            ExceptionCode.DETAILED_INVALID_ARGUMENTS+".fcm.extractNotificationDataList",
                            exception);
        }
        return list;
    }

    private Map<String, Map<String, Object>> extractNotificationDataMap(
            @NonNull String reference,
            @NonNull Map<String, String> remoteData
    ) throws AwesomeNotificationsException {
        String jsonData = remoteData.get(reference);
        Map<String, Map<String, Object>> map = null;
        try {
            if (jsonData != null) {
                Type mapType = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
                map = new Gson().fromJson(jsonData, mapType);
            }
        } catch (Exception exception) {
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_INVALID_ARGUMENTS,
                            "Invalid Firebase notification content "+reference,
                            ExceptionCode.DETAILED_INVALID_ARGUMENTS+".fcm.extractNotificationDataMap",
                            exception);
        }
        return map;
    }

    public Intent buildSilentIntentFromSilentModel(
            @NonNull Context context,
            @NonNull SilentDataModel silentData,
            @NonNull Class<?> targetAction
    ) {
        Intent intent = new Intent(context, targetAction);
        intent.setAction(FcmDefinitions.NOTIFICATION_SILENT_DATA);

        Bundle extras = intent.getExtras();
        if(extras == null)
            extras = new Bundle();

        String jsonData = silentData.toJson();
        extras.putString(FcmDefinitions.NOTIFICATION_SILENT_DATA, jsonData);
        intent.putExtras(extras);

        return intent;
    }

    public SilentDataModel buildSilentDataFromIntent(
            @NonNull Context context,
            @NonNull Intent intent
    ) {
        return new SilentDataModel().fromJson(
                intent.getStringExtra(FcmDefinitions.NOTIFICATION_SILENT_DATA));
    }
}
