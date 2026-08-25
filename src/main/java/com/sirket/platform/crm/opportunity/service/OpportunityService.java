package com.sirket.platform.crm.opportunity.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.access.CrmAccessPolicy;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.service.CompanyService;
import com.sirket.platform.crm.contact.service.ContactService;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.opportunity.domain.OpportunityStatus;
import com.sirket.platform.crm.opportunity.dto.OpportunityDtos;
import com.sirket.platform.crm.opportunity.repository.OpportunityRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final PipelineStageService stageService;
    private final ContactService contactService;
    private final CompanyService companyService;
    private final CrmAccessPolicy accessPolicy;

    public OpportunityService(OpportunityRepository opportunityRepository, PipelineStageService stageService,
            ContactService contactService, CompanyService companyService, CrmAccessPolicy accessPolicy) {
        this.opportunityRepository = opportunityRepository;
        this.stageService = stageService;
        this.contactService = contactService;
        this.companyService = companyService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public Page<OpportunityDtos.OpportunityResponse> search(UUID stageId, OpportunityStatus status, UUID contactId,
            UUID owner, Pageable pageable) {
        return opportunityRepository
                .search(stageId, status, contactId, owner, accessPolicy.ownerRestriction(), pageable)
                .map(OpportunityDtos.OpportunityResponse::from);
    }

    @Transactional(readOnly = true)
    public OpportunityDtos.OpportunityResponse get(UUID id) {
        return OpportunityDtos.OpportunityResponse.from(requireVisible(id));
    }

    @Transactional
    public OpportunityDtos.OpportunityResponse create(OpportunityDtos.CreateOpportunityRequest request) {
        Opportunity opportunity = new Opportunity(
                request.name(),
                resolveContact(request.contactId()),
                resolveCompany(request.companyId()),
                stageService.requireExisting(request.stageId()),
                request.amount(),
                request.probability(),
                request.expectedCloseDate(),
                accessPolicy.currentUserId());
        return OpportunityDtos.OpportunityResponse.from(opportunityRepository.save(opportunity));
    }

    @Transactional
    public OpportunityDtos.OpportunityResponse update(UUID id, OpportunityDtos.UpdateOpportunityRequest request) {
        Opportunity opportunity = requireModifiable(id);
        opportunity.update(
                request.name(),
                resolveContact(request.contactId()),
                resolveCompany(request.companyId()),
                request.amount(),
                request.probability(),
                request.expectedCloseDate());
        return OpportunityDtos.OpportunityResponse.from(opportunityRepository.save(opportunity));
    }

    /**
     * FR-CRM-04. The stage move itself rejects won/lost targets so FR-CRM-09's mandatory lost
     * reason cannot be sidestepped by simply dragging a card onto "Kaybedildi".
     */
    @Transactional
    public OpportunityDtos.OpportunityResponse changeStage(UUID id, UUID stageId) {
        Opportunity opportunity = requireModifiable(id);
        opportunity.moveToStage(stageService.requireExisting(stageId));
        return OpportunityDtos.OpportunityResponse.from(opportunityRepository.save(opportunity));
    }

    @Transactional
    public OpportunityDtos.OpportunityResponse close(UUID id, boolean won, String lostReason) {
        Opportunity opportunity = requireModifiable(id);
        opportunity.close(won, lostReason, stageService.requireClosingStage(won));
        return OpportunityDtos.OpportunityResponse.from(opportunityRepository.save(opportunity));
    }

    @Transactional
    public void delete(UUID id) {
        opportunityRepository.delete(requireVisible(id));
    }

    private Contact resolveContact(UUID contactId) {
        return contactId == null ? null : contactService.requireVisible(contactId);
    }

    private Company resolveCompany(UUID companyId) {
        return companyId == null ? null : companyService.requireVisible(companyId);
    }

    /**
     * Exposed for other CRM packages that hang their own records off an opportunity; the caller
     * must already be allowed to see it.
     */
    public Opportunity requireVisible(UUID id) {
        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Fırsat bulunamadı: " + id));
        accessPolicy.requireVisible(opportunity.getOwnerUserId());
        return opportunity;
    }

    private Opportunity requireModifiable(UUID id) {
        Opportunity opportunity = requireVisible(id);
        accessPolicy.requireModifiable(opportunity.getOwnerUserId());
        return opportunity;
    }
}
