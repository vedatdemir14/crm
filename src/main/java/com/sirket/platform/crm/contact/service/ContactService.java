package com.sirket.platform.crm.contact.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.dto.ContactDtos;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyService companyService;
    private final CrmAccessPolicy accessPolicy;

    public ContactService(ContactRepository contactRepository, CompanyService companyService,
            CrmAccessPolicy accessPolicy) {
        this.contactRepository = contactRepository;
        this.companyService = companyService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public Page<ContactDtos.ContactResponse> search(String name, UUID companyId, String source, UUID owner,
            Pageable pageable) {
        return contactRepository.search(name, companyId, source, owner, accessPolicy.ownerRestriction(), pageable)
                .map(ContactDtos.ContactResponse::from);
    }

    @Transactional(readOnly = true)
    public ContactDtos.ContactResponse get(UUID id) {
        return ContactDtos.ContactResponse.from(requireVisible(id));
    }

    /**
     * FR-CRM-02: reports existing records that look like duplicates so the UI can warn before
     * submitting. Deliberately does not block creation.
     */
    @Transactional(readOnly = true)
    public ContactDtos.DuplicateCheckResponse checkDuplicate(String email, String phone) {
        String normalisedEmail = blankToNull(email);
        String normalisedPhone = blankToNull(phone);
        if (normalisedEmail == null && normalisedPhone == null) {
            throw new ApiExceptions.BadRequest("E-posta veya telefon parametrelerinden en az biri gereklidir");
        }
        List<ContactDtos.ContactResponse> matches =
                contactRepository.findPotentialDuplicates(normalisedEmail, normalisedPhone).stream()
                        .filter(contact -> accessPolicy.canSeeAllRecords()
                                || contact.getOwnerUserId().equals(accessPolicy.currentUserId()))
                        .map(ContactDtos.ContactResponse::from)
                        .toList();
        return new ContactDtos.DuplicateCheckResponse(!matches.isEmpty(), matches);
    }

    @Transactional
    public ContactDtos.ContactResponse create(ContactDtos.ContactRequest request) {
        Contact contact = new Contact(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.title(),
                resolveCompany(request.companyId()),
                request.source(),
                accessPolicy.currentUserId());
        return ContactDtos.ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public ContactDtos.ContactResponse update(UUID id, ContactDtos.ContactRequest request) {
        Contact contact = requireVisible(id);
        accessPolicy.requireModifiable(contact.getOwnerUserId());
        contact.update(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.title(),
                resolveCompany(request.companyId()),
                request.source());
        return ContactDtos.ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public void delete(UUID id) {
        contactRepository.delete(requireVisible(id));
    }

    private Company resolveCompany(UUID companyId) {
        return companyId == null ? null : companyService.requireExisting(companyId);
    }

    private Contact requireVisible(UUID id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Kişi bulunamadı: " + id));
        accessPolicy.requireVisible(contact.getOwnerUserId());
        return contact;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
