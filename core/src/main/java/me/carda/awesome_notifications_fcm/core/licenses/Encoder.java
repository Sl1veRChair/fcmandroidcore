package me.carda.awesome_notifications_fcm.core.licenses;

import android.util.Base64;

final class Encoder {

    static String encodeBase64(byte[] data){
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    static byte[] decodeBase64(String encoded){
        return Base64.decode(encoded, Base64.DEFAULT);
    }
}
