package com.sirket.platform.crm.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.crm.task.service.TaskReminderJob;
import java.time.LocalDate;
import java.util.UUID;
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
class TaskApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskReminderJob reminderJob;

    @Autowired
    private ObjectMapper objectMapper;

    private User repA;
    private User repB;
    private User manager;

    @BeforeEach
    void seedTaskData() {
        repA = createUser("task-rep-a", "ROLE_SALES_REP");
        repB = createUser("task-rep-b", "ROLE_SALES_REP");
        manager = createUser("task-manager", "ROLE_SALES_MANAGER");
    }

    @Test
    void taskIsCreatedOpenAndAssignedToItsCreator() throws Exception {
        mockMvc.perform(post("/api/crm/tasks").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Teklifi hazırla", LocalDate.of(2026, 9, 30), repA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").value(repA.getId().toString()))
                .andExpect(jsonPath("$.createdBy").value(repA.getId().toString()));
    }

    /**
     * Task visibility follows the assignee, so letting a rep assign work to a colleague would make
     * the task vanish from the creator's own list.
     */
    @Test
    void salesRepCannotAssignWorkToSomeoneElseButAManagerCan() throws Exception {
        mockMvc.perform(post("/api/crm/tasks").with(jwtFor(repA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Başkasına iş", LocalDate.of(2026, 9, 30), repB.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(post("/api/crm/tasks").with(jwtFor(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Müdürden görev", LocalDate.of(2026, 9, 30), repB.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedTo").value(repB.getId().toString()));
    }

    @Test
    void repSeesOnlyItsOwnTasksWhileManagerSeesAll() throws Exception {
        createTask(repA, "A'nın görevi", LocalDate.of(2026, 9, 30), repA.getId());
        createTask(repB, "B'nin görevi", LocalDate.of(2026, 9, 30), repB.getId());

        mockMvc.perform(get("/api/crm/tasks").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("A'nın görevi"));

        mockMvc.perform(get("/api/crm/tasks").with(jwtFor(manager)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listIsOrderedByDueDateAndCanBeFiltered() throws Exception {
        createTask(repA, "Geç", LocalDate.of(2026, 12, 1), repA.getId());
        createTask(repA, "Erken", LocalDate.of(2026, 9, 1), repA.getId());

        mockMvc.perform(get("/api/crm/tasks").with(jwtFor(repA)))
                .andExpect(jsonPath("$.content[0].title").value("Erken"))
                .andExpect(jsonPath("$.content[1].title").value("Geç"));

        mockMvc.perform(get("/api/crm/tasks").param("dueBefore", "2026-10-01").with(jwtFor(repA)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Erken"));
    }

    @Test
    void completingATaskRecordsTheTimeAndCannotBeRepeated() throws Exception {
        UUID id = createTask(repA, "Bitecek", LocalDate.of(2026, 9, 30), repA.getId());

        mockMvc.perform(patch("/api/crm/tasks/{id}/complete", id).with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mockMvc.perform(patch("/api/crm/tasks/{id}/complete", id).with(jwtFor(repA)))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherRepCannotSeeOrCompleteSomeoneElsesTask() throws Exception {
        UUID id = createTask(repA, "repA'nın", LocalDate.of(2026, 9, 30), repA.getId());

        mockMvc.perform(get("/api/crm/tasks/{id}", id).with(jwtFor(repB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/crm/tasks/{id}/complete", id).with(jwtFor(repB)))
                .andExpect(status().isNotFound());
    }

    // --- FR-CRM-07: reminders ---

    @Test
    void overdueAndDueSoonTasksProduceTheMatchingNotification() throws Exception {
        LocalDate today = LocalDate.of(2026, 9, 15);
        createTask(repA, "Süresi geçmiş", today.minusDays(3), repA.getId());
        createTask(repA, "Yarın bitiyor", today.plusDays(1), repA.getId());
        createTask(repA, "Çok ileride", today.plusDays(30), repA.getId());

        int created = reminderJob.run(today);
        org.junit.jupiter.api.Assertions.assertEquals(2, created, "yalnızca iki görev hatırlatma üretmeli");

        mockMvc.perform(get("/api/notifications").with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/notifications").param("unreadOnly", "true").with(jwtFor(repA)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * The job runs on a schedule, so a second pass on the same day must not pile up duplicates.
     */
    @Test
    void rerunningTheReminderJobDoesNotDuplicateNotifications() throws Exception {
        LocalDate today = LocalDate.of(2026, 9, 15);
        createTask(repA, "Süresi geçmiş", today.minusDays(3), repA.getId());

        org.junit.jupiter.api.Assertions.assertEquals(1, reminderJob.run(today));
        org.junit.jupiter.api.Assertions.assertEquals(0, reminderJob.run(today), "ikinci geçiş yeni bildirim üretmemeli");

        mockMvc.perform(get("/api/notifications").with(jwtFor(repA)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void completedTasksAreNotRemindedAbout() throws Exception {
        LocalDate today = LocalDate.of(2026, 9, 15);
        UUID id = createTask(repA, "Bitmiş görev", today.minusDays(3), repA.getId());
        mockMvc.perform(patch("/api/crm/tasks/{id}/complete", id).with(jwtFor(repA)))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(0, reminderJob.run(today));
    }

    @Test
    void notificationsAreReadableAndMarkableOnlyByTheirOwner() throws Exception {
        LocalDate today = LocalDate.of(2026, 9, 15);
        createTask(repA, "Süresi geçmiş", today.minusDays(3), repA.getId());
        reminderJob.run(today);

        String response = mockMvc.perform(get("/api/notifications").with(jwtFor(repA)))
                .andReturn().getResponse().getContentAsString();
        UUID notificationId = UUID.fromString(
                objectMapper.readTree(response).get("content").get(0).get("id").asString());

        mockMvc.perform(get("/api/notifications").with(jwtFor(repB)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId).with(jwtFor(repB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId).with(jwtFor(repA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/notifications/unread-count").with(jwtFor(repA)))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    private UUID createTask(User actor, String title, LocalDate dueDate, UUID assignee) throws Exception {
        String response = mockMvc.perform(post("/api/crm/tasks").with(jwtFor(actor))
                        .contentType(MediaType.APPLICATION_JSON).content(body(title, dueDate, assignee)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private String body(String title, LocalDate dueDate, UUID assignedTo) {
        return objectMapper.writeValueAsString(
                new TaskBody(title, "açıklama", dueDate, assignedTo, null, null));
    }

    private record TaskBody(String title, String description, LocalDate dueDate, UUID assignedTo,
            UUID relatedContactId, UUID relatedOpportunityId) {
    }
}
