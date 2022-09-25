package me.carda.awesome_notifications_fcm.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.lang.ref.WeakReference;

import me.carda.awesome_notifications.core.AwesomeNotificationsExtension;
import me.carda.awesome_notifications.core.Definitions;
import me.carda.awesome_notifications.core.broadcasters.receivers.AwesomeEventsReceiver;
import me.carda.awesome_notifications.core.enumerators.NotificationSource;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.listeners.AwesomeActionEventListener;
import me.carda.awesome_notifications.core.listeners.AwesomeNotificationEventListener;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications.core.models.returnedData.ActionReceived;
import me.carda.awesome_notifications.core.models.returnedData.NotificationReceived;
import me.carda.awesome_notifications_fcm.core.broadcasters.receivers.AwesomeFcmEventsReceiver;
import me.carda.awesome_notifications_fcm.core.licenses.LicenseManager;
import me.carda.awesome_notifications_fcm.core.listeners.AwesomeFcmSilentListener;
import me.carda.awesome_notifications_fcm.core.listeners.AwesomeFcmTokenListener;
import me.carda.awesome_notifications_fcm.core.managers.FcmDefaultsManager;
import me.carda.awesome_notifications_fcm.core.managers.TokenManager;
import me.carda.awesome_notifications_fcm.core.mocking_google.NotificationAnalytics;
import me.carda.awesome_notifications_fcm.core.services.AwesomeFcmService;

public class AwesomeNotificationsFcm
    implements
        AwesomeActionEventListener,
        AwesomeNotificationEventListener
{
    private static final String TAG = "AwesomeNotificationsFcm";

    public static boolean debug = false;
    public boolean isInitialized = false;
    public static boolean firebaseEnabled = false;

    private final WeakReference<Context> wContext;


    // *****************************************************************
    ///      CONSTRUCTOR
    // *****************************************************************

    public AwesomeNotificationsFcm(
            Context applicationContext
    ) throws AwesomeNotificationsException {
        this.wContext = new WeakReference<>(applicationContext);
        initialize(applicationContext);
    }


    // *****************************************************************
    //      INITIALIZATION METHOD
    // *****************************************************************

    public static AwesomeNotificationsExtension awesomeFcmExtensions;
    public static boolean areExtensionsLoaded = false;

    public static Class awesomeFcmServiceClass;

    public static void initialize(
            @NonNull Context context
    ) throws AwesomeNotificationsException {
        if(areExtensionsLoaded) return;

        if(awesomeFcmExtensions == null)
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_CLASS_NOT_FOUND,
                            "Awesome's plugin extension reference was not found.",
                            ExceptionCode.DETAILED_INITIALIZATION_FAILED+".awesomeNotifications.extensions");

        awesomeFcmExtensions.loadExternalExtensions(context);
        areExtensionsLoaded = true;
    }


    // *****************************************************************
    ///      EVENT INTERFACES
    // *****************************************************************

    public AwesomeNotificationsFcm subscribeOnAwesomeFcmTokenEvents(AwesomeFcmTokenListener eventListener){
        AwesomeFcmEventsReceiver
                .getInstance()
                .subscribeOnFcmEvents(eventListener);
        return this;
    }

    public AwesomeNotificationsFcm unsubscribeOnAwesomeFcmTokenEvents(AwesomeFcmTokenListener eventListener){
        AwesomeFcmEventsReceiver
                .getInstance()
                .unsubscribeOnNotificationEvents(eventListener);
        return this;
    }

    public AwesomeNotificationsFcm subscribeOnAwesomeSilentEvents(AwesomeFcmSilentListener eventListener){
        AwesomeFcmEventsReceiver
                .getInstance()
                .subscribeOnFcmSilentDataEvents(eventListener);
        return this;
    }

    public AwesomeNotificationsFcm unsubscribeOnAwesomeSilentEvents(AwesomeFcmSilentListener eventListener){
        AwesomeFcmEventsReceiver
                .getInstance()
                .unsubscribeOnFcmSilentDataEvents(eventListener);
        return this;
    }


    // *****************************************************************
    ///      INTERFACE INITIALIZATIONS
    // *****************************************************************

    public boolean enableFirebaseMessaging() {
        if (firebaseEnabled) return true;

        if (debug)
            Logger.d(TAG, "Enabling Awesome FCM");

        if (!firebaseEnabled){
            Context context = this.wContext.get();

            FirebaseApp firebaseApp = FirebaseApp.initializeApp(context);
            firebaseEnabled = firebaseApp != null;

//            ComponentName componentName = new ComponentName(context, awesomeFcmServiceClass);
//            context
//                .getPackageManager()
//                .setComponentEnabledSetting(
//                        componentName,
//                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//                        PackageManager.DONT_KILL_APP);
        }

        if (debug)
            Logger.d(TAG, "Awesome FCM "+(firebaseEnabled ? "enabled" : "not enabled"));

        return firebaseEnabled;
    }

    public boolean initialize(
            String licenseKey,
            long dartCallback,
            long silentCallback,
            boolean debug
    ) throws AwesomeNotificationsException {
        Context context = wContext.get();
        AwesomeNotificationsFcm.debug = debug;

        FcmDefaultsManager
                .saveDefault(
                        context,
                        licenseKey,
                        dartCallback,
                        silentCallback);

        FcmDefaultsManager
                .commitChanges(context);

        AwesomeEventsReceiver
            .getInstance()
            .subscribeOnActionEvents(this)
            .subscribeOnNotificationEvents(this);

        isInitialized = true;

        if(!isGooglePlayServicesAvailable(context))
            Logger.i(TAG,"Google play services are not available on this device.");

        if(!LicenseManager
                .getInstance()
                .isLicenseKeyValid(context))
            Logger.i(TAG,
                    "You need to insert a valid license key to use Awesome Notification's companion " +
                            "plugin in release mode without watermarks (application id \""+
                            context.getPackageName()+
                            "\"). To know more about it, please " +
                            "visit www.carda.me/awesome-notifications#prices");
        else
            Logger.d(TAG, "Awesome FCM License key validated");

        TokenManager
                .getInstance()
                .recoverLostFcmToken();

        return true;
    }

    public boolean isGooglePlayServicesAvailable(Context context){
        GoogleApiAvailabilityLight apiAvailability = GoogleApiAvailabilityLight.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    // *****************************************************************
    ///      FCM METHODS
    // *****************************************************************

    public void requestFcmCode(
            @NonNull final AwesomeFcmTokenListener fcmTokenListener
    ) throws AwesomeNotificationsException {
        assertFirebaseServiceEnabled();
        TokenManager
                .getInstance()
                .requestNewFcmToken(fcmTokenListener);
    }

    private void assertFirebaseServiceEnabled() throws AwesomeNotificationsException {
        if (!firebaseEnabled)
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_INVALID_ARGUMENTS,
                            "Firebase service is not available (check if you have google-services.json file)",
                            ExceptionCode.DETAILED_INVALID_ARGUMENTS+".fcm.subscribe.topicName");
    }

    public void subscribeOnFcmTopic(String topicReference) throws AwesomeNotificationsException {
        assertFirebaseServiceEnabled();
        FirebaseMessaging
                .getInstance()
                .subscribeToTopic(topicReference);
    }

    public void unsubscribeOnFcmTopic(String topicReference) throws AwesomeNotificationsException {
        assertFirebaseServiceEnabled();
        FirebaseMessaging
                .getInstance()
                .unsubscribeFromTopic(topicReference);
    }


    // *****************************************************************
    ///      ANALYTIC TRACKING METHODS
    // *****************************************************************

    private Boolean trackNotificationAction(Intent intent) {

        AwesomeFcmService.printIntentExtras(intent);
        Bundle analyticsExtras = intent.getBundleExtra("gcm.n.analytics_data");
        AwesomeFcmService.printBundleExtras(analyticsExtras);

        // Remote Message ID can be either one of the following...
        Bundle extras = intent.getExtras();
        String messageId = extras.getString("google.message_id");

        if (messageId == null) messageId = intent.getExtras().getString("message_id");
        if (messageId == null) return false;

        String action = intent.getAction();
        NotificationAnalytics.logNotificationOpen(extras);
        NotificationAnalytics.logNotificationOpen(analyticsExtras);
        return true;

//        if (MessagingAnalytics.shouldUploadScionMetrics(intent) || !StringUtils.isNullOrEmpty(messageId)) {
//            MessagingAnalytics.logNotificationOpen(intent.getExtras());
//        }
//        return true;
//        Bundle firebaseBundle = intent.getBundleExtra(FcmDefinitions.FIREBASE_ORIGINAL_EXTRAS);
//
//        if(firebaseBundle != null){
//            MessagingAnalytics.logNotificationOpen(firebaseBundle);
//            if(AwesomeNotifications.debug)
//                Log.d(TAG, "Firebase action received");
//        }
//
//        return true;
    }

    @Override
    public void onNewActionReceived(String eventName, ActionReceived actionReceived) {
        if(
            actionReceived.createdSource != NotificationSource.Firebase ||
            actionReceived.originalIntent == null
        )
            return;

        switch (eventName){

            case Definitions.EVENT_DEFAULT_ACTION:
            case Definitions.EVENT_SILENT_ACTION:
                NotificationAnalytics
                        .logNotificationOpen(actionReceived.originalIntent.getExtras());
                break;

            case Definitions.EVENT_NOTIFICATION_DISMISSED:
                NotificationAnalytics
                        .logNotificationDismiss(actionReceived.originalIntent);
                break;
        }
    }

    @Override
    public boolean onNewActionReceivedWithInterruption(String eventName, ActionReceived actionReceived) {
        return false;
    }

    @Override
    public void onNewNotificationReceived(String eventName, NotificationReceived notificationReceived) {
        if(
            notificationReceived.createdSource != NotificationSource.Firebase ||
            notificationReceived.originalIntent == null
        )
            return;

        switch (eventName){

            case Definitions.EVENT_NOTIFICATION_CREATED:
                NotificationAnalytics
                        .logNotificationReceived(notificationReceived.originalIntent);
                break;

            case Definitions.EVENT_NOTIFICATION_DISPLAYED:
                NotificationAnalytics
                        .logNotificationForeground(notificationReceived.originalIntent);
                break;
        }
    }

    public void dispose() {
    }
}
