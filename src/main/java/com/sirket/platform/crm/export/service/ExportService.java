package com.sirket.platform.crm.export.service;

import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.opportunity.repository.OpportunityRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-CRM-11: CSV export of contacts and opportunities created within a date range.
 * <p>
 * Rows are fetched and written a page at a time rather than collected into a list, so exporting a
 * large range does not hold the whole result set in memory.
 */
@Service
public class ExportService {

    private static final int PAGE_SIZE = 500;

    private static final List<String> CONTACT_HEADERS = List.of(
            "id", "ad", "soyad", "eposta", "telefon", "unvan", "firma", "kaynak", "sorumlu_kullanici_id",
            "olusturulma");

    private static final List<String> OPPORTUNITY_HEADERS = List.of(
            "id", "ad", "kisi", "firma", "asama", "tutar", "olasilik", "beklenen_kapanis", "durum",
            "kayip_nedeni", "kapanis_zamani", "sorumlu_kullanici_id", "olusturulma");

    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;

    public ExportService(ContactRepository contactRepository, OpportunityRepository opportunityRepository) {
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
    }

    @Transactional(readOnly = true)
    public void writeContacts(OutputStream outputStream, LocalDate from, LocalDate to, char delimiter)
            throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        CsvWriter csv = new CsvWriter(writer, delimiter);
        csv.writeRow(CONTACT_HEADERS);

        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Contact> page;
        do {
            page = contactRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                    startOfDay(from), startOfDayAfter(to), pageable);
            for (Contact contact : page.getContent()) {
                Company company = contact.getCompany();
                csv.writeRow(List.of(
                        contact.getId().toString(),
                        text(contact.getFirstName()),
                        text(contact.getLastName()),
                        text(contact.getEmail()),
                        text(contact.getPhone()),
                        text(contact.getTitle()),
                        company != null ? text(company.getName()) : "",
                        text(contact.getSource()),
                        contact.getOwnerUserId().toString(),
                        contact.getCreatedAt().toString()));
            }
            csv.flush();
            pageable = pageable.next();
        }
        while (page.hasNext());
    }

    @Transactional(readOnly = true)
    public void writeOpportunities(OutputStream outputStream, LocalDate from, LocalDate to, char delimiter)
            throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        CsvWriter csv = new CsvWriter(writer, delimiter);
        csv.writeRow(OPPORTUNITY_HEADERS);

        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Opportunity> page;
        do {
            page = opportunityRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                    startOfDay(from), startOfDayAfter(to), pageable);
            for (Opportunity opportunity : page.getContent()) {
                Contact contact = opportunity.getContact();
                Company company = opportunity.getCompany();
                csv.writeRow(List.of(
                        opportunity.getId().toString(),
                        text(opportunity.getName()),
                        contact != null ? text(contact.getFirstName() + " " + contact.getLastName()) : "",
                        company != null ? text(company.getName()) : "",
                        text(opportunity.getStage().getName()),
                        amount(opportunity.getAmount()),
                        Objects.toString(opportunity.getProbability(), ""),
                        Objects.toString(opportunity.getExpectedCloseDate(), ""),
                        opportunity.getStatus().name(),
                        text(opportunity.getLostReason()),
                        Objects.toString(opportunity.getClosedAt(), ""),
                        opportunity.getOwnerUserId().toString(),
                        opportunity.getCreatedAt().toString()));
            }
            csv.flush();
            pageable = pageable.next();
        }
        while (page.hasNext());
    }

    private String text(String value) {
        return value != null ? value : "";
    }

    private String amount(BigDecimal value) {
        return value != null ? value.toPlainString() : "";
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * Exclusive upper bound at the following midnight, so records created at any time on the
     * {@code to} date are included.
     */
    private Instant startOfDayAfter(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
