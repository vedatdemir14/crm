package com.sirket.platform.crm.contact.controller;

import com.sirket.platform.crm.contact.dto.ContactDtos;
import com.sirket.platform.crm.contact.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/contacts")
@Tag(name = "CRM — Kişiler")
@PreAuthorize("hasAnyRole('SALES_REP', 'SALES_MANAGER', 'ADMIN')")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "Kişi listesi (satış temsilcisi yalnızca kendi kayıtlarını görür)")
    public Page<ContactDtos.ContactResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID owner,
            @PageableDefault(size = 20) Pageable pageable) {
        return contactService.search(name, companyId, source, owner, pageable);
    }

    @GetMapping("/check-duplicate")
    @Operation(summary = "Mükerrer kayıt kontrolü — eşleşme varsa mevcut kayıtları döner, engellemez")
    public ContactDtos.DuplicateCheckResponse checkDuplicate(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return contactService.checkDuplicate(email, phone);
    }

    @PostMapping
    @Operation(summary = "Yeni kişi oluşturur")
    public ResponseEntity<ContactDtos.ContactResponse> create(@Valid @RequestBody ContactDtos.ContactRequest request) {
        ContactDtos.ContactResponse created = contactService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/contacts/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kişi detayı")
    public ContactDtos.ContactResponse get(@PathVariable UUID id) {
        return contactService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kişi bilgilerini günceller")
    public ContactDtos.ContactResponse update(
            @PathVariable UUID id, @Valid @RequestBody ContactDtos.ContactRequest request) {
        return contactService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kişiyi siler (soft delete)")
    @PreAuthorize("hasAnyRole('SALES_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
