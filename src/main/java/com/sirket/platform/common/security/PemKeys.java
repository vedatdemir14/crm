package com.sirket.platform.common.security;

import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemKeys {

    private PemKeys() {
    }

    static java.security.interfaces.RSAPublicKey readPublicKey(String pem) {
        byte[] der = decode(pem);
        try {
            return (java.security.interfaces.RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        }
        catch (Exception ex) {
            throw new IllegalStateException("Configured JWT public key is not a valid X.509 RSA key", ex);
        }
    }

    static java.security.interfaces.RSAPrivateKey readPrivateKey(String pem) {
        byte[] der = decode(pem);
        try {
            return (java.security.interfaces.RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        }
        catch (Exception ex) {
            throw new IllegalStateException("Configured JWT private key is not a valid PKCS#8 RSA key", ex);
        }
    }

    private static byte[] decode(String pem) {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
