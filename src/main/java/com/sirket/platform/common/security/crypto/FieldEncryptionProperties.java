package com.sirket.platform.common.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param key base64 of a 32-byte AES key. Supplied at runtime from an environment variable or a
 *            secret manager and never committed (Kriptografi ve Güvenlik Standartları §5).
 */
@ConfigurationProperties(prefix = "app.security.field-encryption")
public record FieldEncryptionProperties(String key) {
}
