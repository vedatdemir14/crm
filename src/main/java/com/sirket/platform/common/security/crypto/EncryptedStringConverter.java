package com.sirket.platform.common.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Applies {@link FieldEncryptionService} transparently, so entity code treats an encrypted column
 * as an ordinary String and the encryption stays out of the domain model.
 * <p>
 * Registered as a Spring bean because it needs the key injected; Spring Boot hands Hibernate a bean
 * container, so the converter is resolved from the context rather than instantiated reflectively.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldEncryptionService encryptionService;

    public EncryptedStringConverter(FieldEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
