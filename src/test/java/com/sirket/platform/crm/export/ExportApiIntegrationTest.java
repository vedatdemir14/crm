package com.sirket.platform.crm.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.crm.contact.domain.Company;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.CompanyRepository;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExportApiIntegrationTest extends IntegrationTestBase {

    private static final String BOM = "﻿";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    private User admin;
    private User rep;

    @BeforeEach
    void seedExportData() {
        admin = createUser("export-admin", "ROLE_ADMIN");
        rep = createUser("export-rep", "ROLE_SALES_REP");

        Company company = companyRepository.save(
                new Company("Acme A.Ş.", "Yazılım", "acme.test", "İstanbul", rep.getId()));
        contactRepository.save(new Contact("Ayşe", "Yılmaz", "ayse@acme.test", "+905551112233",
                "Satın Alma Müdürü", company, "FUAR", rep.getId()));
    }

    private String exportContacts(String delimiter) throws Exception {
        var request = get("/api/crm/export")
                .param("from", "2000-01-01").param("to", "2099-12-31")
                .param("entity", "contacts")
                .with(jwtFor(admin));
        if (delimiter != null) {
            request = request.param("delimiter", delimiter);
        }
        return body(request);
    }

    /**
     * The endpoint returns a StreamingResponseBody, so MockMvc only produces the payload after the
     * async dispatch; without it the recorded response is empty.
     */
    private String body(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        MvcResult started = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void contactsAreExportedWithHeaderAndData() throws Exception {
        String csv = exportContacts(null);
        String[] lines = csv.split("\r\n");

        assertTrue(lines[0].startsWith(BOM + "id"), "ilk satır BOM ve başlık ile başlamalı");
        assertTrue(lines[0].contains("eposta"), "başlık satırı alan adlarını içermeli");
        assertTrue(lines[1].contains("Ayşe"), "Türkçe karakterler bozulmadan yazılmalı: " + lines[1]);
        assertTrue(lines[1].contains("Yılmaz"));
        assertTrue(lines[1].contains("Acme A.Ş."));
        assertEquals(2, lines.length, "başlık artı tek kayıt beklenir");
    }

    /**
     * Excel on Windows reads a BOM-less UTF-8 CSV in the system code page, which mangles Turkish
     * characters. The BOM is what stops that.
     */
    @Test
    void fileStartsWithAByteOrderMark() throws Exception {
        assertTrue(exportContacts(null).startsWith(BOM));
    }

    @Test
    void defaultDelimiterIsSemicolonAndCommaCanBeRequested() throws Exception {
        String semicolonCsv = exportContacts(null);
        assertTrue(semicolonCsv.split("\r\n")[0].contains("id;ad;soyad"), semicolonCsv.split("\r\n")[0]);

        String commaCsv = exportContacts(",");
        assertTrue(commaCsv.split("\r\n")[0].contains("id,ad,soyad"), commaCsv.split("\r\n")[0]);
    }

    /**
     * A field containing the delimiter, a quote or a newline has to be quoted, or the row splits
     * into the wrong number of columns when it is read back.
     */
    @Test
    void fieldsContainingDelimitersOrQuotesAreQuoted() throws Exception {
        contactRepository.save(new Contact("Zor", "Veri", "zor@test.test", "+90555",
                "Müdür; Yönetici", null, "NOT \"özel\"", rep.getId()));

        String csv = exportContacts(null);
        assertTrue(csv.contains("\"Müdür; Yönetici\""), "noktalı virgül içeren alan tırnaklanmalı");
        assertTrue(csv.contains("\"NOT \"\"özel\"\"\""), "tırnak içeren alan kaçışlanmalı: " + csv);
    }

    /**
     * Excel evaluates a cell starting with =, +, - or @ as a formula, so exported user text could
     * run as code on whoever opens the file.
     */
    @Test
    void fieldsThatWouldBecomeExcelFormulasAreNeutralised() throws Exception {
        contactRepository.save(new Contact("=1+1", "Enjeksiyon", "inject@test.test", "+90555",
                "@SUM(A1:A9)", null, "WEB", rep.getId()));

        String csv = exportContacts(null);
        assertTrue(csv.contains("'=1+1"), "formül ile başlayan alan kesme işareti ile etkisizleştirilmeli");
        assertTrue(csv.contains("'@SUM(A1:A9)"), csv);
        assertFalse(csv.contains(";=1+1;"), "ham formül doğrudan yazılmamalı");
    }

    @Test
    void recordsOutsideTheDateRangeAreExcluded() throws Exception {
        String csv = body(get("/api/crm/export")
                .param("from", "2000-01-01").param("to", "2000-12-31")
                .param("entity", "contacts").with(jwtFor(admin)));

        assertEquals(1, csv.split("\r\n").length, "aralık dışı kayıt gelmemeli, yalnızca başlık kalmalı");
    }

    @Test
    void opportunitiesDatasetIsAlsoExportable() throws Exception {
        mockMvc.perform(get("/api/crm/export")
                        .param("from", "2000-01-01").param("to", "2099-12-31")
                        .param("entity", "opportunities").with(jwtFor(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"opportunities-2000-01-01_2099-12-31.csv\""));
    }

    @Test
    void invalidParametersAreRejected() throws Exception {
        mockMvc.perform(get("/api/crm/export").param("from", "2026-01-01").param("to", "2099-12-31")
                        .param("entity", "musteriler").with(jwtFor(admin)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/crm/export").param("from", "2099-01-01").param("to", "2026-12-31")
                        .param("entity", "contacts").with(jwtFor(admin)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/crm/export").param("from", "2000-01-01").param("to", "2099-12-31")
                        .param("entity", "contacts").param("format", "xlsx").with(jwtFor(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/crm/export")
                        .param("from", "2000-01-01").param("to", "2099-12-31")
                        .param("entity", "contacts").with(jwtFor(rep)))
                .andExpect(status().isForbidden());
    }
}
