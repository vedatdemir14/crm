package com.sirket.platform.crm.contact.dto;

import com.sirket.platform.crm.contact.domain.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CompanyRequest(
            @NotBlank(message = "Firma adı zorunludur") @Size(max = 255) String name,
            @Size(max = 120) String industry,
            @Size(max = 255) String website,
            String address) {
    }

    public record CompanyResponse(
            UUID id,
            String name,
            String industry,
            String website,
            String address,
            UUID ownerUserId,
            Instant createdAt,
            Instant updatedAt) {

        public static CompanyResponse from(Company company) {
            return new CompanyResponse(
                    company.getId(),
                    company.getName(),
                    company.getIndustry(),
                    company.getWebsite(),
                    company.getAddress(),
                    company.getOwnerUserId(),
                    company.getCreatedAt(),
                    company.getUpdatedAt());
        }
    }
}
