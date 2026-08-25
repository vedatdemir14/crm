package com.sirket.platform.crm.activity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.crm.activity.domain.Activity;
import com.sirket.platform.crm.activity.domain.ActivityType;
import com.sirket.platform.crm.activity.repository.ActivityRepository;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.repository.ContactRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "crm.activity.edit-window=PT1H")
class ActivityApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User repA;
    private User repB;
    private Contact contactOfRepA;

    @BeforeEach
    void seedActivityData() {
        repA = createUser("act-rep-a", "ROLE_SALES_REP");
        repB = createUser("act-rep-b", "ROLE_SALES_REP");
        contactOfRepA = contactRepository.save(new Contact("Ayşe", "Yılmaz", "ayse@acme.test",
                "+905551112233", "Satın Alma", null, "FUAR", repA.getId()));
    }

    @Test
    void activityIsLoggedAgainstAContact() throws Exception {
        mockMvc.perform(post("/api/crm/activities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(ActivityType.CALL, "Tanışma görüşmesi", contactOfRepA.getId(),
                                Instant.parse("2026-08-20T09:00:00Z"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CALL"))
                .andExpect(jsonPath("$.subject").value("Tanışma görüşmesi"))
                .andExpect(jsonPath("$.contactName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.createdBy").value(repA.getId().toString()));
    }

    @Test
    void activityWithoutAContactOrOpportunityIsRejected() throws Exception {
        mockMvc.perform(post("/api/crm/activities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(ActivityType.NOTE, "Boşta duran not", null,
                                Instant.parse("2026-08-20T09:00:00Z"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void timelineIsOrderedNewestFirst() throws Exception {
        log(ActivityType.CALL, "Birinci arama", Instant.parse("2026-08-01T09:00:00Z"));
        log(ActivityType.MEETING, "Sonraki toplantı", Instant.parse("2026-08-15T09:00:00Z"));
        log(ActivityType.EMAIL, "En son e-posta", Instant.parse("2026-08-20T09:00:00Z"));

        mockMvc.perform(get("/api/crm/contacts/{id}/activities", contactOfRepA.getId()).with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].subject").value("En son e-posta"))
                .andExpect(jsonPath("$.content[1].subject").value("Sonraki toplantı"))
                .andExpect(jsonPath("$.content[2].subject").value("Birinci arama"));
    }

    @Test
    void anotherRepCannotReadTheTimelineOfAContactTheyCannotSee() throws Exception {
        log(ActivityType.CALL, "Gizli kalmalı", Instant.parse("2026-08-20T09:00:00Z"));

        mockMvc.perform(get("/api/crm/contacts/{id}/activities", contactOfRepA.getId()).with(jwtFor(repB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void authorCanCorrectAnActivityWithinTheEditWindow() throws Exception {
        UUID id = log(ActivityType.CALL, "Yanlış konu", Instant.parse("2026-08-20T09:00:00Z"));

        mockMvc.perform(put("/api/crm/activities/{id}", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBody(
                                ActivityType.MEETING, "Düzeltilmiş konu", "detay",
                                Instant.parse("2026-08-21T10:00:00Z")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MEETING"))
                .andExpect(jsonPath("$.subject").value("Düzeltilmiş konu"));
    }

    @Test
    void someoneElseCannotEditAnotherUsersActivity() throws Exception {
        UUID id = log(ActivityType.CALL, "Sahibi repA", Instant.parse("2026-08-20T09:00:00Z"));

        mockMvc.perform(put("/api/crm/activities/{id}", id).with(jwtFor(repB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBody(
                                ActivityType.NOTE, "Ele geçirildi", null,
                                Instant.parse("2026-08-21T10:00:00Z")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    /**
     * The edit window is one hour under this test's properties, so an activity written two hours
     * ago must already be frozen.
     */
    @Test
    void anActivityOlderThanTheEditWindowCanNoLongerBeChanged() throws Exception {
        UUID id = log(ActivityType.CALL, "Eski kayıt", Instant.parse("2026-08-20T09:00:00Z"));
        backdateCreatedAt(id, Instant.now().minus(2, ChronoUnit.HOURS));

        mockMvc.perform(put("/api/crm/activities/{id}", id).with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBody(
                                ActivityType.NOTE, "Geç düzeltme", null,
                                Instant.parse("2026-08-21T10:00:00Z")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        mockMvc.perform(delete("/api/crm/activities/{id}", id).with(jwtFor(repA)))
                .andExpect(status().isConflict());
    }

    @Test
    void deletedActivityLeavesTheTimeline() throws Exception {
        UUID id = log(ActivityType.NOTE, "Silinecek", Instant.parse("2026-08-20T09:00:00Z"));

        mockMvc.perform(delete("/api/crm/activities/{id}", id).with(jwtFor(repA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/crm/contacts/{id}/activities", contactOfRepA.getId()).with(jwtFor(repA)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private UUID log(ActivityType type, String subject, Instant occurredAt) throws Exception {
        String response = mockMvc.perform(post("/api/crm/activities").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(type, subject, contactOfRepA.getId(), occurredAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private String body(ActivityType type, String subject, UUID contactId, Instant occurredAt) {
        return objectMapper.writeValueAsString(
                new CreateBody(type, subject, "açıklama", contactId, null, occurredAt));
    }

    /**
     * Rewrites created_at through the repository so the edit window can be exercised without
     * making the test sleep.
     */
    private void backdateCreatedAt(UUID id, Instant createdAt) {
        Activity activity = activityRepository.findById(id).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(activity, "createdAt", createdAt);
        activityRepository.saveAndFlush(activity);
    }

    private record CreateBody(ActivityType type, String subject, String description, UUID contactId,
            UUID opportunityId, Instant occurredAt) {
    }

    private record UpdateBody(ActivityType type, String subject, String description, Instant occurredAt) {
    }
}
