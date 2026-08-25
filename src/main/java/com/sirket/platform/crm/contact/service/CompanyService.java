package com.sirket.platform.crm.contact.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.access.CrmAccessPolicy;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.dto.CompanyDtos;
import com.sirket.platform.crm.contact.repository.CompanyRepository;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final CrmAccessPolicy accessPolicy;

    public CompanyService(CompanyRepository companyRepository, ContactRepository contactRepository,
            CrmAccessPolicy accessPolicy) {
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public Page<CompanyDtos.CompanyResponse> search(String name, String industry, UUID owner, Pageable pageable) {
        return companyRepository.search(name, industry, owner, accessPolicy.ownerRestriction(), pageable)
                .map(CompanyDtos.CompanyResponse::from);
    }

    @Transactional(readOnly = true)
    public CompanyDtos.CompanyResponse get(UUID id) {
        return CompanyDtos.CompanyResponse.from(requireVisible(id));
    }

    @Transactional
    public CompanyDtos.CompanyResponse create(CompanyDtos.CompanyRequest request) {
        Company company = new Company(
                request.name(),
                request.industry(),
                request.website(),
                request.address(),
                accessPolicy.currentUserId());
        return CompanyDtos.CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public CompanyDtos.CompanyResponse update(UUID id, CompanyDtos.CompanyRequest request) {
        Company company = requireVisible(id);
        accessPolicy.requireModifiable(company.getOwnerUserId());
        company.update(request.name(), request.industry(), request.website(), request.address());
        return CompanyDtos.CompanyResponse.from(companyRepository.save(company));
    }

    /**
     * Soft delete — Hibernate turns this into an update of {@code deleted_at}, so the row stays
     * available for historical reporting.
     */
    @Transactional
    public void delete(UUID id) {
        Company company = requireVisible(id);
        if (contactRepository.existsByCompanyId(id)) {
            throw new ApiExceptions.Conflict("Firmaya bağlı kişi kayıtları var, önce onları taşıyın veya silin");
        }
        companyRepository.delete(company);
    }

    /**
     * Exposed for other CRM packages that link a company onto their own records. Visibility is
     * always checked, so a caller cannot attach — and thereby discover — a company they may not see.
     */
    public Company requireVisible(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Firma bulunamadı: " + id));
        accessPolicy.requireVisible(company.getOwnerUserId());
        return company;
    }
}
