package com.sirket.platform.crm.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.crm.activity.domain.Activity;
import com.sirket.platform.crm.activity.repository.ActivityRepository;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import com.sirket.platform.crm.integration.domain.ExternalMessage;
import com.sirket.platform.crm.integration.domain.ExternalMessageType;
import com.sirket.platform.crm.integration.domain.MessageDirection;
import com.sirket.platform.crm.integration.repository.IntegrationSyncStateRepository;
import com.sirket.platform.crm.integration.service.ExternalMessageLinker;
import com.sirket.platform.crm.integration.service.MailCalendarSyncJob;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MailCalendarIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ExternalMessageLinker linker;

    @Autowired
    private MailCalendarSyncJob syncJob;

    @Autowired
    private IntegrationSyncStateRepository syncStateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User repA;
    private User repB;
    private Contact ayse;
    private Contact mehmet;

    @BeforeEach
    void seedIntegrationData() {
        admin = createUser("int-admin", "ROLE_ADMIN");
        repA = createUser("int-rep-a", "ROLE_SALES_REP");
        repB = createUser("int-rep-b", "ROLE_SALES_REP");

        ayse = contactRepository.save(new Contact("Ayşe", "Yılmaz", "ayse@acme.test",
                "+905551112233", "Satın Alma", null, "FUAR", repA.getId()));
        mehmet = contactRepository.save(new Contact("Mehmet", "Demir", "mehmet@beta.test",
                "+905554445566", "CTO", null, "WEB", repB.getId()));
    }

    private ExternalMessage message(String externalId, ExternalMessageType type, MessageDirection direction,
            String subject, List<String> participants) {
        return new ExternalMessage(externalId, "test-provider", type, direction, subject,
                "mesaj gövdesi", participants, Instant.parse("2026-08-20T09:00:00Z"));
    }

    @Test
    void messageIsFiledOnTheTimelineOfTheMatchingContact() {
        ExternalMessageLinker.LinkResult result = linker.link(message(
                "msg-1", ExternalMessageType.EMAIL, MessageDirection.INBOUND,
                "Teklif hakkında", List.of("ayse@acme.test")));

        assertTrue(result.matched());
        assertEquals(1, result.createdActivities());

        Activity imported = activityRepository.findAll().getFirst();
        assertEquals(ayse.getId(), imported.getContact().getId());
        assertEquals("Gelen: Teklif hakkında", imported.getSubject());
        assertEquals("test-provider", imported.getExternalSource());
        assertTrue(imported.isImported());
        // Attributed to the contact's owner, since no user is acting during a sync.
        assertEquals(repA.getId(), imported.getCreatedBy());
    }

    /**
     * Addresses arrive in whatever case the mail system used; matching has to ignore that or most
     * real messages would silently fail to link.
     */
    @Test
    void participantMatchingIgnoresCaseAndSurroundingWhitespace() {
        ExternalMessageLinker.LinkResult result = linker.link(message(
                "msg-case", ExternalMessageType.EMAIL, MessageDirection.OUTBOUND,
                "Büyük harfli adres", List.of("  AYSE@ACME.TEST  ")));

        assertTrue(result.matched());
        assertEquals(1, result.createdActivities());
    }

    /**
     * A meeting with two known contacts belongs on both timelines, so one message legitimately
     * becomes two activities.
     */
    @Test
    void meetingWithTwoKnownContactsLandsOnBothTimelines() {
        ExternalMessageLinker.LinkResult result = linker.link(message(
                "msg-multi", ExternalMessageType.MEETING, MessageDirection.OUTBOUND,
                "Ortak toplantı", List.of("ayse@acme.test", "mehmet@beta.test", "kolega@sirket.test")));

        assertEquals(2, result.createdActivities(), "yalnızca tanınan iki kişi için aktivite üretilmeli");
        assertEquals(2, activityRepository.count());
    }

    @Test
    void messageWithNoKnownParticipantIsSkippedRatherThanInventingAContact() {
        ExternalMessageLinker.LinkResult result = linker.link(message(
                "msg-unknown", ExternalMessageType.EMAIL, MessageDirection.INBOUND,
                "Tanınmayan gönderen", List.of("kimse@bilinmeyen.test")));

        assertEquals(false, result.matched());
        assertEquals(0, result.createdActivities());
        assertEquals(0, activityRepository.count());
    }

    /**
     * A sync runs repeatedly and providers commonly re-deliver the same items, so importing twice
     * must not double the timeline.
     */
    @Test
    void reimportingTheSameMessageDoesNotDuplicateTheActivity() {
        ExternalMessage msg = message("msg-repeat", ExternalMessageType.EMAIL, MessageDirection.INBOUND,
                "Tekrar gelen", List.of("ayse@acme.test"));

        assertEquals(1, linker.link(msg).createdActivities());
        ExternalMessageLinker.LinkResult second = linker.link(msg);

        assertEquals(0, second.createdActivities());
        assertEquals(1, second.alreadyImported());
        assertEquals(1, activityRepository.count());
    }

    @Test
    void importedActivityAppearsOnTheContactTimelineEndpoint() throws Exception {
        linker.link(message("msg-timeline", ExternalMessageType.EMAIL, MessageDirection.INBOUND,
                "Zaman çizelgesinde", List.of("ayse@acme.test")));

        mockMvc.perform(get("/api/crm/contacts/{id}/activities", ayse.getId()).with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].subject").value("Gelen: Zaman çizelgesinde"))
                .andExpect(jsonPath("$.content[0].type").value("EMAIL"));
    }

    @Test
    void messagesCanBeImportedThroughTheEndpoint() throws Exception {
        String body = objectMapper.writeValueAsString(new ImportBody(List.of(
                new MessageBody("api-1", "outlook-export", "EMAIL", "INBOUND", "API üzerinden",
                        "gövde", List.of("ayse@acme.test"), Instant.parse("2026-08-20T09:00:00Z")),
                new MessageBody("api-2", "outlook-export", "MEETING", "OUTBOUND", "Bilinmeyen kişi",
                        "gövde", List.of("yok@bilinmeyen.test"), Instant.parse("2026-08-21T09:00:00Z")))));

        mockMvc.perform(post("/api/crm/integrations/mail-calendar/messages").with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messagesReceived").value(2))
                .andExpect(jsonPath("$.createdActivities").value(1))
                .andExpect(jsonPath("$.unmatchedMessages").value(1));
    }

    @Test
    void importEndpointIsAdminOnly() throws Exception {
        String body = objectMapper.writeValueAsString(new ImportBody(List.of(
                new MessageBody("api-x", "src", "EMAIL", "INBOUND", "konu", "gövde",
                        List.of("ayse@acme.test"), Instant.parse("2026-08-20T09:00:00Z")))));

        mockMvc.perform(post("/api/crm/integrations/mail-calendar/messages").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void importRequestWithoutParticipantsIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(new ImportBody(List.of(
                new MessageBody("api-empty", "src", "EMAIL", "INBOUND", "konu", "gövde",
                        List.of(), Instant.parse("2026-08-20T09:00:00Z")))));

        mockMvc.perform(post("/api/crm/integrations/mail-calendar/messages").with(jwtFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    /**
     * The stub provider returns nothing, so the sync should complete quietly and still record where
     * it got to — that bookkeeping is what a real provider will rely on.
     */
    @Test
    void syncWithTheStubProviderIsANoOpButStillAdvancesTheCursor() {
        ExternalMessageLinker.BatchResult result = syncJob.run();

        assertEquals(0, result.messagesReceived());
        assertEquals(0, result.createdActivities());
        assertNotNull(syncStateRepository.findById("stub").orElse(null),
                "senkronizasyon imleci kaydedilmeli");
    }

    @Test
    void syncEndpointRunsTheJobOnDemand() throws Exception {
        mockMvc.perform(post("/api/crm/integrations/mail-calendar/sync").with(jwtFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messagesReceived").value(0));
    }

    private record ImportBody(List<MessageBody> messages) {
    }

    private record MessageBody(String externalId, String source, String type, String direction, String subject,
            String body, List<String> participants, Instant occurredAt) {
    }
}
