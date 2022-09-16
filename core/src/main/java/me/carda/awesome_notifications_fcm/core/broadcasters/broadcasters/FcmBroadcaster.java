package me.carda.awesome_notifications_fcm.core.broadcasters.broadcasters;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import me.carda.awesome_notifications_fcm.core.builders.FcmNotificationBuilder;
import me.carda.awesome_notifications_fcm.core.managers.TokenManager;
import me.carda.awesome_notifications_fcm.core.models.SilentDataModel;
import me.carda.awesome_notifications_fcm.core.services.FcmSilentService;

public class FcmBroadcaster {
    private static final String TAG = "FcmBroadcaster";

    public static void SendBroadcastNewFcmToken(@NonNull String token){
        TokenManager
                .getInstance()
                .setLastToken(token);
    }

    public static void SendBroadcastSilentData(Context context, SilentDataModel silentData) {
        Intent serviceIntent =
                FcmNotificationBuilder
                        .getNewBuilder()
                        .buildSilentIntentFromSilentModel(
                                context,
                                silentData,
                                FcmSilentService.class);

        JobIntentService.enqueueWork(
                context,
                FcmSilentService.class,
                999,
                serviceIntent);
    }
}
