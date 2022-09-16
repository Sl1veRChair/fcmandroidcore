package me.carda.awesome_notifications_fcm.core.mocking_google;

import com.google.android.datatransport.Transformer;
import com.google.firebase.messaging.reporting.MessagingClientEventExtension;

final class MessagingAnalyticsTransformer implements Transformer {
    static final MessagingAnalyticsTransformer instance;

    private MessagingAnalyticsTransformer() {
    }

    public Object apply(Object var1) {
        return ((MessagingClientEventExtension)var1).toByteArray();
    }

    static {
        instance = new MessagingAnalyticsTransformer();
    }
}
