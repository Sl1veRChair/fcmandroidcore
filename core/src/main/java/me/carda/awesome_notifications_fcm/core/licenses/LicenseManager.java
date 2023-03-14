package me.carda.awesome_notifications_fcm.core.licenses;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

import me.carda.awesome_notifications.core.AwesomeNotifications;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.managers.FcmDefaultsManager;

public final class LicenseManager {
    public static final String TAG = "LicenseManager";
    public static final String APP_VERSION = "0.7.5-pre.1";

    // ************** SINGLETON PATTERN ***********************

    private static LicenseManager instance;

    protected LicenseManager(){}

    public static LicenseManager getInstance() {
        if(instance == null)
            instance = new LicenseManager();
        return instance;
    }

    // ********************************************************

    public boolean isLicenseKeyValid(
        @NonNull Context context
    ) throws AwesomeNotificationsException
    {
        List<String> licenseKeys = FcmDefaultsManager.getLicenseKeys(context);
        if (licenseKeys == null) {
            printLicenseMessageError(context);
            return false;
        }

        try {
            PublicKey publicKey = Crypto.getPublicKey();
            if(publicKey == null) return false;

            for (String licenseKey : licenseKeys){
                if(StringUtils.getInstance().isNullOrEmpty(licenseKey))
                    return false;

                boolean isSingleVersion = false;
                String base64Encoded;
                if(licenseKey.startsWith("single:")){
                    isSingleVersion = true;
                    if(!licenseKey.startsWith("single:"+APP_VERSION+":")){
                        continue;
                    }
                    base64Encoded = licenseKey
                            .replaceFirst("single:[\\w\\.\\+]+:", "");
                }
                else {
                    base64Encoded = licenseKey;
                }

                if(
                    assignerVerify(
                        AwesomeNotifications.getPackageName(context),
                        isSingleVersion ? APP_VERSION+":" : "",
                        publicKey,
                        Base64.decode(base64Encoded, Base64.DEFAULT))
                ){
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        printLicenseMessageError(context);
        return false;
    }

    @NonNull
    private boolean assignerVerify(
            @NonNull String packageName,
            @NonNull String packageVersion,
            @NonNull PublicKey publicKey,
            @NonNull byte[] signature
    ) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
    {
        Signature publicSign = Signature.getInstance(Crypto.signProtocol);
        publicSign.initVerify(publicKey);
        publicSign.update((packageVersion+packageName)
                .getBytes(StandardCharsets.UTF_8));
        return publicSign.verify(signature);
    }

    public boolean printValidationTest(
            @NonNull Context context
    ) throws AwesomeNotificationsException {
        if(!LicenseManager
                .getInstance()
                .isLicenseKeyValid(context)) {
            printLicenseMessageError(context);
            return false;
        }
        else {
            Logger.d(TAG, "Awesome FCM License key validated");
            return true;
        }
    }

    void printLicenseMessageError(Context context) {
        String licenseMessage =
                "You need to insert a valid license key to use Awesome Notification's FCM " +
                        "plugin in release mode without watermarks (application id: \"" +
                        AwesomeNotifications.getPackageName(context) +
                        "\"). To know more about it, please " +
                        "visit https://www.awesome-notifications.carda.me#prices";


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

        if(isDebuggable) {
            Logger.i(TAG, licenseMessage);
        } else {
            Logger.e(TAG, licenseMessage);
        }
    }
}
