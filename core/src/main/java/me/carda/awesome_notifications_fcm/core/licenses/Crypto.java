package me.carda.awesome_notifications_fcm.core.licenses;

import androidx.annotation.Nullable;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

final class Crypto {

    static final String keyType = "RSA";
    static final String signProtocol = "SHA256with"+keyType;

    static final String publicKey =
        "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgFf9hnX01Ey13U22dcPIbvkvEbF8" +
        "6dxGDWFpp67x6/HdAmCEYCRKD0VgiZy53TOU9byI1KGECeneEAkdinY8GvxOtoJ0" +
        "9OWQOR+0/2IDY7DrsXiw9n0Fm1kEGVzzD5EubglhOdg7yFpoF1iN7hpFja2BBldp" +
        "XSnFAPBN0uAgiBdZAgMBAAE=";

    @Nullable
    static PublicKey getPublicKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return KeyFactory.getInstance(keyType).generatePublic(
            new X509EncodedKeySpec(Encoder.decodeBase64(publicKey))
        );
    }
}
