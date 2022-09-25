package me.carda.awesome_notifications_fcm.core.mocking_google;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.datatransport.Encoding;
import com.google.android.datatransport.Transport;
import com.google.android.datatransport.TransportFactory;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.connector.AnalyticsConnector;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.NotificationParams;
import com.google.firebase.messaging.reporting.MessagingClientEvent;
import com.google.firebase.messaging.reporting.MessagingClientEventExtension;

import java.util.concurrent.ExecutionException;

import me.carda.awesome_notifications.core.AwesomeNotifications;
import me.carda.awesome_notifications.core.utils.StringUtils;


public class NotificationAnalytics {

    public static void logNotificationReceived(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (shouldUploadScionMetrics(intent)) {
            logToScion("_nr", extras);
        }

        if (shouldUploadFirelogAnalytics(intent)) {
            logToFirelog(MessagingClientEvent.Event.MESSAGE_DELIVERED, extras, FirebaseMessaging.getTransportFactory());
        }
    }

    public static void logNotificationOpen(@NonNull Bundle extras) {
        if(!isValidFcmExtras(extras))
            return;

        setUserPropertyIfRequired(extras);
        logToScion("_no", extras);

        if (isDeliveryMetricsExportToBigQueryEnabled()) {
            logToFirelog(MessagingClientEvent.Event.MESSAGE_OPEN, extras, FirebaseMessaging.getTransportFactory());
        }
    }

    public static void logNotificationDismiss(@NonNull Intent intent) {
        if(isValidFcmIntent(intent))
            logToScion("_nd", intent.getExtras());
    }

    public static void logNotificationForeground(@NonNull Intent intent) {
        if(isValidFcmIntent(intent))
            logToScion("_nf", intent.getExtras());
    }

    public static boolean shouldUploadScionMetrics(@NonNull Intent intent) {
        return !isDirectBootMessage(intent) && shouldUploadScionMetrics(intent.getExtras());
    }

    public static boolean shouldUploadScionMetrics(@NonNull Bundle extras) {
        return "1".equals(extras.getString("google.c.a.e"));
    }

    public static boolean shouldUploadFirelogAnalytics(@NonNull Intent intent) {
        return !isDirectBootMessage(intent) && isDeliveryMetricsExportToBigQueryEnabled();
    }

    private static boolean isDirectBootMessage(Intent intent) {
        return "com.google.firebase.messaging.RECEIVE_DIRECT_BOOT".equals(intent.getAction());
    }

    private static boolean isValidFcmIntent(Intent intent){
        if (intent == null) return false;
        return isValidFcmExtras(intent.getExtras());
    }

    private static boolean isValidFcmExtras(Bundle extras){
        if (extras == null) return false;

        String messageId = extras.getString("google.message_id");

        if (messageId == null)
            messageId = extras.getString("message_id");

        return !StringUtils
                    .getInstance()
                    .isNullOrEmpty(messageId);
    }

    static boolean isDeliveryMetricsExportToBigQueryEnabled() {
        try {
            FirebaseApp.getInstance();
        } catch (IllegalStateException illegalStateException) {
            Log.i("FirebaseMessaging", "FirebaseApp has not being initialized. Device might be in direct boot mode. Skip exporting delivery metrics to Big Query");
            return false;
        }

        Context context = FirebaseApp.getInstance().getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences("com.google.firebase.messaging", 0);
        if (preferences.contains("export_to_big_query")) {
            return preferences.getBoolean("export_to_big_query", false);
        } else {
            PackageManager packageManager;
            try {
                packageManager = context.getPackageManager();
            } catch (Exception exception) {
                return false;
            }

            if (packageManager != null) {
                ApplicationInfo applicationInfo;
                try {
                    applicationInfo = packageManager.getApplicationInfo(AwesomeNotifications.getPackageName(context), PackageManager.GET_META_DATA);
                } catch (Exception var6) {
                    return false;
                }

                if (applicationInfo != null) {
                    Bundle var11;
                    try {
                        var11 = applicationInfo.metaData;
                    } catch (Exception exception) {
                        return false;
                    }

                    if (var11 != null) {
                        boolean var12;
                        try {
                            var12 = applicationInfo.metaData.containsKey("delivery_metrics_exported_to_big_query_enabled");
                        } catch (Exception var4) {
                            return false;
                        }

                        if (var12) {
                            try {
                                return applicationInfo.metaData.getBoolean("delivery_metrics_exported_to_big_query_enabled", false);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    private static void setUserPropertyIfRequired(Bundle bundle) {
        if (bundle != null) {
            if ("1".equals(bundle.getString("google.c.a.tc"))) {
                AnalyticsConnector var1 = (AnalyticsConnector)FirebaseApp.getInstance().get(AnalyticsConnector.class);
                if (Log.isLoggable("FirebaseMessaging", Log.DEBUG)) {
                    Log.d("FirebaseMessaging", "Received event with track-conversion=true. Setting user property and reengagement event");
                }

                if (var1 != null) {
                    String c_id = bundle.getString("google.c.a.c_id");
                    var1.setUserProperty("fcm", "_ln", c_id);
                    Bundle tempBundle = new Bundle();
                    tempBundle.putString("source", "Firebase");
                    tempBundle.putString("medium", "notification");
                    tempBundle.putString("campaign", c_id);
                    var1.logEvent("fcm", "_cmp", tempBundle);
                } else {
                    Log.w("FirebaseMessaging", "Unable to set user property for conversion tracking:  analytics library is missing");
                }
            } else if (Log.isLoggable("FirebaseMessaging", Log.DEBUG)) {
                Log.d("FirebaseMessaging", "Received event with track-conversion=false. Do not set user property");
            }
        }
    }

    @VisibleForTesting
    static void logToScion(String tag, Bundle extras) {
        if (extras == null) {
            extras = new Bundle();
        }

        Bundle bundle = new Bundle();
        String composerId = getComposerId(extras);
        if (composerId != null) {
            bundle.putString("_nmid", composerId);
        }

        composerId = getComposerLabel(extras);
        if (composerId != null) {
            bundle.putString("_nmn", composerId);
        }

        composerId = getMessageLabel(extras);
        if (!TextUtils.isEmpty(composerId)) {
            bundle.putString("label", composerId);
        }

        composerId = getMessageChannel(extras);
        if (!TextUtils.isEmpty(composerId)) {
            bundle.putString("message_channel", composerId);
        }

        composerId = getTopic(extras);
        if (composerId != null) {
            bundle.putString("_nt", composerId);
        }

        composerId = getMessageTime(extras);
        if (composerId != null) {
            try {
                bundle.putInt("_nmt", Integer.parseInt(composerId));
            } catch (NumberFormatException var7) {
                Log.w("FirebaseMessaging", "Error while parsing timestamp in GCM event", var7);
            }
        }

        composerId = getUseDeviceTime(extras);
        if (composerId != null) {
            try {
                bundle.putInt("_ndt", Integer.parseInt(composerId));
            } catch (NumberFormatException var6) {
                Log.w("FirebaseMessaging", "Error while parsing use_device_time in GCM event", var6);
            }
        }

        String messageTypeForScion = getMessageTypeForScion(extras);
        if ("_nr".equals(tag) || "_nf".equals(tag)) {
            bundle.putString("_nmc", messageTypeForScion);
        }

        if (Log.isLoggable("FirebaseMessaging", Log.DEBUG)) {
            messageTypeForScion = String.valueOf(bundle);
            int var11 = tag.length();
            String var4 = String.valueOf(messageTypeForScion);
            int var10 = var4.length();
            String stringBuilder =
                    "Logging to scion event=" +
                    tag +
                    " scionPayload=" +
                    messageTypeForScion;
            Log.d("FirebaseMessaging", stringBuilder);
        }

        AnalyticsConnector analyticsConnector = (AnalyticsConnector)FirebaseApp.getInstance().get(AnalyticsConnector.class);
        if (analyticsConnector != null) {
            analyticsConnector.logEvent("fcm", tag, bundle);
        } else {
            Log.w("FirebaseMessaging", "Unable to log event: analytics library is missing");
        }
    }

    private static void logToFirelog(final MessagingClientEvent.Event event, final Bundle bundle, @Nullable final TransportFactory transportFactory) {
        if (transportFactory == null) {
            Log.e("FirebaseMessaging", "TransportFactory is null. Skip exporting message delivery metrics to Big Query");
        } else {
            new Thread(new Runnable() {
                public void run() {
                final MessagingClientEvent clientEvent = eventToProto(event, bundle);
                try {
                    //com.google.android.datatransport.Transformer<com.google.firebase.messaging.reporting.MessagingClientEventExtension,byte[]>
                    Transport transport = transportFactory.getTransport("FCM_CLIENT_EVENT_LOGGING", MessagingClientEventExtension.class, Encoding.of("proto"), MessagingAnalyticsTransformer.instance);
                    MessagingClientEventExtension.Builder builder = MessagingClientEventExtension.newBuilder();
                    builder.setMessagingClientEvent(clientEvent);
                    transport.send(com.google.android.datatransport.Event.ofTelemetry(builder.build()));
                } catch (RuntimeException var3) {
                    Log.w("FirebaseMessaging", "Failed to send big query analytics payload.", var3);
                }
                }
            }).start();
        }
    }

    static void setDeliveryMetricsExportToBigQuery(boolean var0) {
        FirebaseApp.getInstance().getApplicationContext().getSharedPreferences("com.google.firebase.messaging", 0).edit().putBoolean("export_to_big_query", var0).apply();
    }

    @NonNull
    static int getTtl(Bundle bundle) {
        Object ttl = bundle.get("google.ttl");
        if (ttl instanceof Integer) {
            return (Integer)ttl;
        } else {
            if (ttl instanceof String) {
                int lenght;
                try {
                    lenght = Integer.parseInt((String)ttl);
                    return lenght;
                } catch (NumberFormatException var3) {
                    String var5 = String.valueOf(ttl);
                    String var1 = String.valueOf(var5);
                    lenght = var1.length();
                    String builder = "Invalid TTL: " +
                            var5;
                    Log.w("FirebaseMessaging", builder);
                }
            }

            return 0;
        }
    }

    @Nullable
    static String getCollapseKey(Bundle var0) {
        return var0.getString("collapse_key");
    }

    @Nullable
    static String getComposerId(Bundle var0) {
        return var0.getString("google.c.a.c_id");
    }

    @Nullable
    static String getComposerLabel(Bundle var0) {
        return var0.getString("google.c.a.c_l");
    }

    @Nullable
    static String getMessageLabel(Bundle var0) {
        return var0.getString("google.c.a.m_l");
    }

    @Nullable
    static String getMessageChannel(Bundle var0) {
        return var0.getString("google.c.a.m_c");
    }

    @Nullable
    static String getMessageTime(Bundle var0) {
        return var0.getString("google.c.a.ts");
    }

    @Nullable
    static String getMessageId(Bundle var0) {
        String var1 = var0.getString("google.message_id");
        return var1 == null ? var0.getString("message_id") : var1;
    }

    @NonNull
    static String getPackageName() {
        return AwesomeNotifications.getPackageName(FirebaseApp.getInstance().getApplicationContext());
    }

    @NonNull
    static String getInstanceId(Bundle var0) {
        String var3 = var0.getString("google.to");
        if (!TextUtils.isEmpty(var3)) {
            return var3;
        } else {
            try {
                var3 = (String) Tasks.await(FirebaseInstallations.getInstance(FirebaseApp.getInstance()).getId());
                return var3;
            } catch (InterruptedException | ExecutionException var2) {
                throw new RuntimeException(var2);
            }
        }
    }

    @NonNull
    static String getMessageTypeForScion(Bundle var0) {
        return !NotificationParams.isNotification(var0) ? "data" : "display";
    }

    @NonNull
    static MessagingClientEvent.MessageType getMessageTypeForFirelog(Bundle var0) {
        MessagingClientEvent.MessageType var1;
        if (var0 != null && NotificationParams.isNotification(var0)) {
            var1 = MessagingClientEvent.MessageType.DISPLAY_NOTIFICATION;
        } else {
            var1 = MessagingClientEvent.MessageType.DATA_MESSAGE;
        }

        return var1;
    }

    @Nullable
    static String getTopic(Bundle var0) {
        String var1 = var0.getString("from");
        return var1 != null && var1.startsWith("/topics/") ? var1 : null;
    }

    @Nullable
    static String getUseDeviceTime(Bundle var0) {
        return var0.containsKey("google.c.a.udt") ? var0.getString("google.c.a.udt") : null;
    }

    static long getProjectNumber(Bundle bundle) {
        if (bundle.containsKey("google.c.sender.id")) {
            try {
                return Long.parseLong(bundle.getString("google.c.sender.id"));
            } catch (NumberFormatException var11) {
                Log.w("FirebaseMessaging", "error parsing project number", var11);
            }
        }

        FirebaseApp firebaseApp = FirebaseApp.getInstance();
        String senderId = firebaseApp.getOptions().getGcmSenderId();

        if (senderId != null) {
            try {
                return Long.parseLong(senderId);
            } catch (NumberFormatException var10) {
                Log.w("FirebaseMessaging", "error parsing sender ID", var10);
            }
        }

        String applicationId = firebaseApp.getOptions().getApplicationId();
        if (!applicationId.startsWith("1:")) {
            try {
                return Long.parseLong(applicationId);
            } catch (NumberFormatException var8) {
                Log.w("FirebaseMessaging", "error parsing app ID", var8);
            }
        } else {
            String[] appIdSplit = applicationId.split(":");
            if (appIdSplit.length < 2) {
                return 0L;
            }

            applicationId = appIdSplit[1];
            if (applicationId.isEmpty()) {
                return 0L;
            }

            try {
                return Long.parseLong(applicationId);
            } catch (NumberFormatException exception) {
                Log.w("FirebaseMessaging", "error parsing app ID", exception);
            }
        }

        return 0L;
    }

    static MessagingClientEvent eventToProto(MessagingClientEvent.Event event, Intent intent) {
        if (intent == null) {
            return null;
        } else {
            Bundle bundle = intent.getExtras();
            return eventToProto(event, bundle);
        }
    }

    static MessagingClientEvent eventToProto(MessagingClientEvent.Event event, Bundle bundle) {

        if (bundle == null) {
            bundle = Bundle.EMPTY;
        }

        com.google.firebase.messaging.reporting.MessagingClientEvent.Builder builder = MessagingClientEvent.newBuilder();

        builder.setTtl(getTtl(bundle));
        builder.setEvent(event);
        builder.setInstanceId(getInstanceId(bundle));
        builder.setPackageName(getPackageName());
        builder.setSdkPlatform(MessagingClientEvent.SDKPlatform.ANDROID);
        builder.setMessageType(getMessageTypeForFirelog(bundle));

        String messageId = getMessageId(bundle);
        if (messageId != null) {
            builder.setMessageId(messageId);
        }

        messageId = getTopic(bundle);
        if (messageId != null) {
            builder.setTopic(messageId);
        }

        messageId = getCollapseKey(bundle);
        if (messageId != null) {
            builder.setCollapseKey(messageId);
        }

        messageId = getMessageLabel(bundle);
        if (messageId != null) {
            builder.setAnalyticsLabel(messageId);
        }

        messageId = getComposerLabel(bundle);
        if (messageId != null) {
            builder.setComposerLabel(messageId);
        }

        long projectNumber = getProjectNumber(bundle);
        if (projectNumber > 0L) {
            builder.setProjectNumber(projectNumber);
        }

        return builder.build();
    }
}
