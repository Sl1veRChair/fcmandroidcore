package me.carda.awesome_notifications_fcm.core.models;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import me.carda.awesome_notifications.core.Definitions;
import me.carda.awesome_notifications.core.enumerators.NotificationLifeCycle;
import me.carda.awesome_notifications.core.enumerators.NotificationSource;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.exceptions.ExceptionCode;
import me.carda.awesome_notifications.core.exceptions.ExceptionFactory;
import me.carda.awesome_notifications.core.models.AbstractModel;
import me.carda.awesome_notifications.core.models.NotificationModel;
import me.carda.awesome_notifications.core.utils.CalendarUtils;

public class SilentDataModel extends AbstractModel {

    public static final String TAG = "SilentDataModel";

    public Integer id;
    public Calendar createdDate;
    public NotificationSource createdSource;
    public NotificationLifeCycle createdLifeCycle;
    public Map<String, String> data = new HashMap<>();

    public SilentDataModel() {}

    public SilentDataModel(NotificationModel notificationModel){
        if(notificationModel.content != null && notificationModel.content.payload != null){
            data.clear();
            data.putAll(notificationModel.content.payload);
        }
    }

    public void registerCreationEvent(
            @NonNull NotificationSource createdSource,
            @NonNull NotificationLifeCycle createdLifeCycle
    ) throws AwesomeNotificationsException {
        CalendarUtils calendarUtils = CalendarUtils.getInstance();
        this.createdSource = createdSource;
        this.createdLifeCycle = createdLifeCycle;
        this.createdDate =
                calendarUtils.getCurrentCalendar(
                        calendarUtils.getUtcTimeZone());
    }

    @Override
    public AbstractModel fromMap(Map<String, Object> arguments) {
        data.clear();
        for (String key : arguments.keySet()) {
            switch (key) {
                case Definitions.NOTIFICATION_ID:
                    id = getValueOrDefault(arguments, Definitions.NOTIFICATION_ID,
                            Integer.class, null);
                case Definitions.NOTIFICATION_MODEL_CONTENT:
                case Definitions.NOTIFICATION_MODEL_SCHEDULE:
                case Definitions.NOTIFICATION_MODEL_BUTTONS:
                    continue;
                case Definitions.NOTIFICATION_CREATED_SOURCE:
                    createdSource = getValueOrDefault(arguments, Definitions.NOTIFICATION_CREATED_SOURCE,
                            NotificationSource.class, NotificationSource.Firebase);
                    continue;
                case Definitions.NOTIFICATION_CREATED_DATE:
                    createdDate = getValueOrDefault(arguments, Definitions.NOTIFICATION_CREATED_DATE, Calendar.class, null);
                    continue;
                case Definitions.NOTIFICATION_CREATED_LIFECYCLE:
                    createdLifeCycle = getValueOrDefault(arguments, Definitions.NOTIFICATION_CREATED_LIFECYCLE,
                            NotificationLifeCycle.class, null);
                    continue;
                default:
                    Object value = arguments.get(key);
                    if(value != null)
                        data.put(key, value.toString());
            }
        }
        return this;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> mapData = new HashMap<>(data);

        putDataOnSerializedMap(Definitions.NOTIFICATION_ID, mapData, this.id);
        putDataOnSerializedMap(Definitions.NOTIFICATION_CREATED_SOURCE, mapData, this.createdSource);
        putDataOnSerializedMap(Definitions.NOTIFICATION_CREATED_LIFECYCLE, mapData, this.createdLifeCycle);
        putDataOnSerializedMap(Definitions.NOTIFICATION_CREATED_DATE, mapData, this.createdDate);

        return mapData;
    }

    @Override
    public String toJson() {
        return templateToJson();
    }

    @Override
    public SilentDataModel fromJson(String json){
        return (SilentDataModel) super.templateFromJson(json);
    }

    @Override
    public void validate(Context context) throws AwesomeNotificationsException {
        if(data == null ||  data.isEmpty())
            throw ExceptionFactory
                    .getInstance()
                    .createNewAwesomeException(
                            TAG,
                            ExceptionCode.CODE_MISSING_ARGUMENTS,
                            "Notification data is empty",
                            ExceptionCode.DETAILED_REQUIRED_ARGUMENTS+".silentData.data");
    }
}
