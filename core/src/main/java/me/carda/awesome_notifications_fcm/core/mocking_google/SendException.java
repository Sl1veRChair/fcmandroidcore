package me.carda.awesome_notifications_fcm.core.mocking_google;

import java.util.Locale;

public final class SendException extends Exception {
    public static final int ERROR_UNKNOWN = 0;
    public static final int ERROR_INVALID_PARAMETERS = 1;
    public static final int ERROR_SIZE = 2;
    public static final int ERROR_TTL_EXCEEDED = 3;
    public static final int ERROR_TOO_MANY_MESSAGES = 4;
    private final int errorCode;

    public SendException(String var1) {
        super(var1);
        this.errorCode = this.parseErrorCode(var1);
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    private int parseErrorCode(String var1) {
        if (var1 == null) {
            return 0;
        } else {
            byte var3;
            label35: {
                String var2 = var1.toLowerCase(Locale.US);
                switch(var2.hashCode()) {
                    case -1743242157:
                        if (var2.equals("service_not_available")) {
                            var3 = 3;
                            break label35;
                        }
                        break;
                    case -1290953729:
                        if (var2.equals("toomanymessages")) {
                            var3 = 4;
                            break label35;
                        }
                        break;
                    case -920906446:
                        if (var2.equals("invalid_parameters")) {
                            var3 = 0;
                            break label35;
                        }
                        break;
                    case -617027085:
                        if (var2.equals("messagetoobig")) {
                            var3 = 2;
                            break label35;
                        }
                        break;
                    case -95047692:
                        if (var2.equals("missing_to")) {
                            var3 = 1;
                            break label35;
                        }
                }

                var3 = -1;
            }

            switch(var3) {
                case 0:
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                default:
                    return 0;
            }
        }
    }
}

