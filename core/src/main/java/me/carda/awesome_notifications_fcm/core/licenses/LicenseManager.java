package me.carda.awesome_notifications_fcm.core.licenses;

import android.content.Context;
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
import me.carda.awesome_notifications.core.utils.StringUtils;
import me.carda.awesome_notifications_fcm.core.managers.FcmDefaultsManager;

public final class LicenseManager {

    public static final String TAG = "LicenseManager";

    // ************** SINGLETON PATTERN ***********************

    private static LicenseManager instance;

    protected LicenseManager(){}

    public static LicenseManager getInstance() {
        if(instance == null)
            instance = new LicenseManager();
        return instance;
    }

    // ********************************************************

    public final boolean isLicenseKeyValid(
        @NonNull Context context
    ) throws AwesomeNotificationsException
    {
        List<String> licenseKeys = FcmDefaultsManager.getLicenseKeys(context);
        if (licenseKeys == null) return false;
        for (String licenseKey : licenseKeys){
            if(StringUtils.getInstance().isNullOrEmpty(licenseKey))
                return false;
            try {
                PublicKey publicKey = Crypto.getPublicKey();
                if(publicKey == null) return false;

                if(
                    assignerVerify(
                        AwesomeNotifications.getPackageName(context),
                        publicKey,
                        Base64.decode(licenseKey, Base64.DEFAULT))
                ){
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @NonNull
    private boolean assignerVerify(
            @NonNull String packageName,
            @NonNull PublicKey publicKey,
            @NonNull byte[] signature
    ) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException
    {
        Signature publicSign = Signature.getInstance(Crypto.signProtocol);
        publicSign.initVerify(publicKey);
        publicSign.update(packageName.getBytes(StandardCharsets.UTF_8));
        return publicSign.verify(signature);
    }
}
