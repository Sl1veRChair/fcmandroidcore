package me.carda.awesome_notifications_fcm.core.managers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import me.carda.awesome_notifications.core.AwesomeNotificationsExtension;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.managers.RepositoryManager;
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.FcmDefinitions;
import me.carda.awesome_notifications_fcm.core.models.FcmDefaultsModel;

public final class FcmDefaultsManager {

    private static final RepositoryManager<FcmDefaultsModel> shared
                    = new RepositoryManager<>(
                            StringUtils.getInstance(),
                            "FcmDefaultsManager",
                            FcmDefaultsModel.class,
                            "FcmDefaultsModel");

    public static Boolean removeDefault(Context context) throws AwesomeNotificationsException {
        return shared.remove(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults");
    }

    public static void saveDefault(
            @NonNull Context context,
            @NonNull List<String> licenseKeys,
            @Nullable Long dartCallback,
            @Nullable Long silentCallback
    ) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = getDefaults(context);

        defaults.licenseKeys = licenseKeys;
        defaults.reverseDartCallback = dartCallback == null ? null : dartCallback.toString();
        defaults.silentDataCallback = silentCallback == null ? null : silentCallback.toString();

        saveDefault(context, defaults);
    }

    private static void saveDefault(Context context, FcmDefaultsModel defaults) throws AwesomeNotificationsException {
        shared.set(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults", defaults);
    }

    public static FcmDefaultsModel getDefaults(Context context) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = shared.get(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults");
        return defaults != null ? defaults : new FcmDefaultsModel();
    }

    public static long getSilentCallbackDispatcher(Context context) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = getDefaults(context);
        return (defaults.silentDataCallback != null) ? Long.parseLong(defaults.silentDataCallback) : 0L;
    }

    public static long getDartCallbackDispatcher(Context context) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = getDefaults(context);
        return (defaults.reverseDartCallback != null) ? Long.parseLong(defaults.reverseDartCallback) : 0L;
    }

    public static List<String> getLicenseKeys(Context context) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = getDefaults(context);
        return defaults.licenseKeys;
    }

    public static void commitChanges(Context context) throws AwesomeNotificationsException {
        shared.commit(context);
    }

    public static void setAwesomeExtensionClassName(
            Context context,
            Class<? extends AwesomeNotificationsExtension> backgroundHandleClass
    ) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = getDefaults(context);
        defaults.backgroundHandleClass = backgroundHandleClass.getName();
        saveDefault(context, defaults);
    }

    public static String getAwesomeExtensionClassName(
            Context context
    ) throws AwesomeNotificationsException {
        FcmDefaultsModel defaults = getDefaults(context);
        return defaults.backgroundHandleClass;
    }
}
