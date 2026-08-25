package com.sirket.platform.common.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM encryption for individual database columns (Kriptografi ve Güvenlik Standartları §5).
 * GCM is used rather than a plain mode because it authenticates as well as hides: ciphertext that
 * has been tampered with fails to decrypt instead of yielding altered plaintext.
 * <p>
 * A fresh 12-byte IV is generated per value and stored in front of the ciphertext. That is what
 * stops two employees with the same national id from producing identical ciphertext, which would
 * leak the fact that they match. The cost is that encrypted columns cannot be searched or given a
 * unique constraint — the same input encrypts differently every time.
 */
@Component
public class FieldEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptionService(FieldEncryptionProperties properties) {
        this.key = new SecretKeySpec(decodeKey(properties.key()), "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Alan şifrelenemedi", ex);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalStateException("Şifreli değer beklenenden kısa");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Alan çözülemedi; anahtar değişmiş veya veri bozulmuş olabilir", ex);
        }
    }

    /**
     * Deliberately fails at startup rather than falling back to a generated key. An ephemeral
     * signing key only invalidates tokens, but an ephemeral <em>data</em> key would make every row
     * written before the restart permanently unreadable.
     */
    private byte[] decodeKey(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("""
                    Alan şifreleme anahtarı tanımlı değil. app.security.field-encryption.key \
                    (FIELD_ENCRYPTION_KEY) base64 kodlanmış 32 baytlık bir anahtar olmalıdır. \
                    Üretmek için: openssl rand -base64 32""");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured.trim());
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Alan şifreleme anahtarı geçerli base64 değil", ex);
        }
        if (decoded.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "Alan şifreleme anahtarı 32 bayt olmalıdır (AES-256), bulunan: " + decoded.length);
        }
        return decoded;
    }
}
