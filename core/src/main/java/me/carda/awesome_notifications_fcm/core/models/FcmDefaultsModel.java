package me.carda.awesome_notifications_fcm.core.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.carda.awesome_notifications.core.Definitions;
import me.carda.awesome_notifications.core.exceptions.AwesomeNotificationsException;
import me.carda.awesome_notifications.core.models.AbstractModel;
import me.carda.awesome_notifications_fcm.core.FcmDefinitions;

public class FcmDefaultsModel extends AbstractModel {

    public String silentDataCallback = "0";
    public String reverseDartCallback = "0";
    public List<String> licenseKeys;
    public String backgroundHandleClass;

    public FcmDefaultsModel(){}

    public FcmDefaultsModel(
            @NonNull List<String> licenseKey,
            @Nullable Long reverseDartCallback,
            @Nullable Long silentDataCallback
    ){
        this.licenseKeys = licenseKey;
        this.silentDataCallback = silentDataCallback == null ? "0" : silentDataCallback.toString();
        this.reverseDartCallback = reverseDartCallback == null ? "0" : reverseDartCallback.toString();
    }

    @Override
    public FcmDefaultsModel fromMap(Map<String, Object> arguments) {
        silentDataCallback    = getValueOrDefault(arguments, FcmDefinitions.SILENT_HANDLE, String.class, "0");
        reverseDartCallback   = getValueOrDefault(arguments, FcmDefinitions.DART_BG_HANDLE, String.class, "0");
        backgroundHandleClass = getValueOrDefault(arguments, Definitions.NOTIFICATION_BG_HANDLE_CLASS, String.class, null);
        licenseKeys           = getValueOrDefaultList(arguments, FcmDefinitions.LICENSE_KEYS, null);
        return this;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> dataMap = new HashMap<>();

        putDataOnSerializedMap(FcmDefinitions.LICENSE_KEYS, dataMap, licenseKeys);
        putDataOnSerializedMap(FcmDefinitions.SILENT_HANDLE, dataMap, silentDataCallback);
        putDataOnSerializedMap(FcmDefinitions.DART_BG_HANDLE, dataMap, reverseDartCallback);
        putDataOnSerializedMap(Definitions.NOTIFICATION_BG_HANDLE_CLASS, dataMap, backgroundHandleClass);

        return dataMap;
    }

    @Override
    public String toJson() {
        return templateToJson();
    }

    @Override
    public FcmDefaultsModel fromJson(String json){
        return (FcmDefaultsModel) super.templateFromJson(json);
    }

    @Override
    public void validate(Context context) throws AwesomeNotificationsException {

    }
}
