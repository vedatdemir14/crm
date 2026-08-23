package com.sirket.platform.crm.contact.dto;

import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContactDtos {

    private ContactDtos() {
    }

    public record ContactRequest(
            @NotBlank(message = "Ad zorunludur") @Size(max = 100) String firstName,
            @NotBlank(message = "Soyad zorunludur") @Size(max = 100) String lastName,
            @Email(message = "Geçerli bir e-posta adresi giriniz") @Size(max = 255) String email,
            @Size(max = 50) String phone,
            @Size(max = 120) String title,
            UUID companyId,
            @Size(max = 50) String source) {
    }

    public record ContactResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String title,
            UUID companyId,
            String companyName,
            String source,
            UUID ownerUserId,
            Instant createdAt,
            Instant updatedAt) {

        public static ContactResponse from(Contact contact) {
            Company company = contact.getCompany();
            return new ContactResponse(
                    contact.getId(),
                    contact.getFirstName(),
                    contact.getLastName(),
                    contact.getEmail(),
                    contact.getPhone(),
                    contact.getTitle(),
                    company != null ? company.getId() : null,
                    company != null ? company.getName() : null,
                    contact.getSource(),
                    contact.getOwnerUserId(),
                    contact.getCreatedAt(),
                    contact.getUpdatedAt());
        }
    }

    /**
     * FR-CRM-02: duplicates are reported back to the user as a warning; creation is not blocked.
     */
    public record DuplicateCheckResponse(boolean duplicateFound, List<ContactResponse> matches) {
    }
}
