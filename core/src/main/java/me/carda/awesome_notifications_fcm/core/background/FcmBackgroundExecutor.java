package me.carda.awesome_notifications_fcm.core.background;

import android.content.Context;
import android.content.Intent;

import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;

public abstract class FcmBackgroundExecutor {
    private static final String TAG = "FcmBackgroundExecutor";

    private static FcmBackgroundExecutor runningInstance;

    protected Long dartCallbackHandle = 0L;
    protected Long silentCallbackHandle = 0L;

    private static Class<? extends FcmBackgroundExecutor> awesomeBackgroundExecutorClass;

    public static void setBackgroundExecutorClass (
            Class<? extends FcmBackgroundExecutor> awesomeBackgroundExecutorClass
    ){
        FcmBackgroundExecutor.awesomeBackgroundExecutorClass =
                awesomeBackgroundExecutorClass;
    }

    public abstract boolean isDone();
    public abstract boolean runBackgroundAction(Context context, Intent silentIntent);

    public static void runBackgroundExecutor(
        Context context,
        Intent silentIntent,
        Long dartCallbackHandle,
        Long silentCallbackHandle
    ) throws AwesomeNotificationsException {

        try {
            if(awesomeBackgroundExecutorClass == null)
                throw ExceptionFactory
                        .getInstance()
                        .createNewAwesomeException(
                                TAG,
                                ExceptionCode.CODE_INITIALIZATION_EXCEPTION,
                                "There is no fcm background executor available to run.",
                                ExceptionCode.DETAILED_INSUFFICIENT_REQUIREMENTS
                                        +".fcmBackgroundExecutorClass");

            if(runningInstance == null || runningInstance.isDone()) {
                runningInstance =
                        awesomeBackgroundExecutorClass.newInstance();

                runningInstance.dartCallbackHandle = dartCallbackHandle;
                runningInstance.silentCallbackHandle = silentCallbackHandle;
            }

            if(!runningInstance.runBackgroundAction(
                    context,
                    silentIntent
            )){
                runningInstance = null;
                throw ExceptionFactory
                        .getInstance()
                        .createNewAwesomeException(
                                TAG,
                                ExceptionCode.CODE_BACKGROUND_EXECUTION_EXCEPTION,
                                "The background executor could not be started.",
                                ExceptionCode.DETAILED_INSUFFICIENT_REQUIREMENTS
                                        +".backgroundExecutor.run");
            }

        } catch (IllegalAccessException | InstantiationException e) {
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_BACKGROUND_EXECUTION_EXCEPTION,
                            String.format("%s", e.getLocalizedMessage()),
                            e);
        }
    }
}
