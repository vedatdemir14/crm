package com.sirket.platform.common.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Stores a monetary amount as ciphertext while the entity keeps working with {@link BigDecimal}.
 * <p>
 * The value is encrypted from its plain string form, so the column is text and the amount cannot be
 * summed or compared in SQL. That is the trade the cryptography standards ask for on payroll
 * figures (Veri Modeli §6); anything that needs to aggregate salaries has to do it in the
 * application, where the values are decrypted.
 */
@Component
@Converter
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    private final FieldEncryptionService encryptionService;

    public EncryptedBigDecimalConverter(FieldEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? null : encryptionService.encrypt(attribute.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        String plain = encryptionService.decrypt(dbData);
        return plain == null ? null : new BigDecimal(plain);
    }
}
