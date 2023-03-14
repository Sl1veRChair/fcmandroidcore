package me.carda.awesome_notifications_fcm.core.managers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.logs.Logger;
import me.carda.awesome_notifications_fcm.core.broadcasters.receivers.AwesomeFcmEventsReceiver;
import me.carda.awesome_notifications_fcm.core.listeners.AwesomeFcmTokenListener;

public class TokenManager {

    public static String TAG = "AwesomeFcmEventsReceiver";
    String lastToken;
    boolean recovered = false;

    // ************** SINGLETON PATTERN ***********************

    private static TokenManager instance;

    private TokenManager(){}
    public static TokenManager getInstance() {
        if (instance == null)
            instance = new TokenManager();
        return instance;
    }

    // ********************************************************

    public void setLastToken(@Nullable String lastToken) {
        if(this.lastToken == null && lastToken == null) return;
        if(this.lastToken != null && this.lastToken.equals(lastToken)) return;
        this.lastToken = lastToken;

        AwesomeFcmEventsReceiver
                .getInstance()
                .addNewFcmTokenEvent(lastToken);

        AwesomeFcmEventsReceiver
                .getInstance()
                .addNewNativeTokenEvent(lastToken);
    }

    public void recoverLostFcmToken(){
        if (this.lastToken == null) return;

        AwesomeFcmEventsReceiver
                .getInstance()
                .addNewFcmTokenEvent(lastToken);

        AwesomeFcmEventsReceiver
                .getInstance()
                .addNewNativeTokenEvent(lastToken);
    }

    public void requestNewFcmToken(AwesomeFcmTokenListener tokenListener){
        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        boolean successful = task.isSuccessful();
                        String token = successful ? task.getResult() : "";

                        if (successful) {
                            // Fire the new FCM registration token
                            TokenManager
                                    .getInstance()
                                    .setLastToken(token);
                            Logger.d(TAG, "FCM token successfully registered");
                        }
                        else
                            ExceptionFactory
                                    .getInstance()
                                    .registerNewAwesomeException(
                                            TAG,
                                            ExceptionCode.CODE_MISSING_ARGUMENTS,
                                            "Fetching FCM registration token failed",
                                            ExceptionCode.DETAILED_REQUIRED_ARGUMENTS+".fcm.token");

                        tokenListener.onNewNativeTokenReceived(token);
                        tokenListener.onNewFcmTokenReceived(token);
                    }
                });
    }

    @Nullable
    public String getLastToken() {
        return lastToken;
    }
}
