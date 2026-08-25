package com.sirket.platform.hr.onboarding;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.domain.EmploymentType;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
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
class OnboardingApiIntegrationTest extends IntegrationTestBase {

    private static final LocalDate HIRE_DATE = LocalDate.of(2024, 3, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User hrAdmin;
    private User itUser;
    private User outsider;
    private Employee employee;

    @BeforeEach
    void seedOnboardingData() {
        hrAdmin = createUser("onb-hr", "ROLE_HR_ADMIN");
        itUser = createUser("onb-it", "ROLE_EMPLOYEE");
        outsider = createUser("onb-outsider", "ROLE_EMPLOYEE");

        employee = employeeRepository.save(new Employee(null, "Ayşe", "Yılmaz", "ayse@sirket.test",
                null, null, LocalDate.of(1990, 5, 12), HIRE_DATE, EmploymentType.FULL_TIME,
                null, "Uzman", null));
    }

    // --- FR-HR-06: joining ---

    @Test
    void seededTemplatesCoverBothDirections() throws Exception {
        mockMvc.perform(get("/api/hr/onboarding-task-templates").param("type", "ONBOARDING")
                        .with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].name").value("Ekipman teslimi (dizüstü, telefon)"));

        mockMvc.perform(get("/api/hr/onboarding-task-templates").param("type", "OFFBOARDING")
                        .with(jwtFor(hrAdmin)))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].name").value("Devir teslim tamamlanması"));
    }

    /**
     * The point of templates: HR applies the checklist once instead of typing the same four steps
     * for every joiner, and the due dates fall out of the hire date.
     */
    @Test
    void applyingTheJoiningTemplateCreatesTheWholeChecklistAnchoredToTheHireDate() throws Exception {
        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(4))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.anchorDate").value("2024-03-01"))
                // offsets of 0, 0, 3 and 7 days from the hire date
                .andExpect(jsonPath("$.tasks[0].dueDate").value("2024-03-01"))
                .andExpect(jsonPath("$.tasks[2].dueDate").value("2024-03-04"))
                .andExpect(jsonPath("$.tasks[3].dueDate").value("2024-03-08"))
                .andExpect(jsonPath("$.tasks[0].status").value("PENDING"));
    }

    /**
     * HR adds templates over time, so re-running has to fill the gaps rather than duplicate what is
     * already there.
     */
    @Test
    void reapplyingATemplateSkipsWhatAlreadyExists() throws Exception {
        applyTemplate("ONBOARDING");

        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(4));

        mockMvc.perform(get("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").with(jwtFor(hrAdmin)))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void checklistReportsProgress() throws Exception {
        applyTemplate("ONBOARDING");
        UUID first = firstTaskId("ONBOARDING");

        mockMvc.perform(patch("/api/hr/onboarding-tasks/{id}/complete", first).with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedBy").value(hrAdmin.getId().toString()));

        mockMvc.perform(get("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").with(jwtFor(hrAdmin)))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.completed").value(1))
                // the 2024 due dates are long past, so the three open items are overdue
                .andExpect(jsonPath("$.overdue").value(3))
                .andExpect(jsonPath("$.employeeName").value("Ayşe Yılmaz"));
    }

    // --- FR-HR-07: leaving ---

    /**
     * Offboarding is scheduled around the leaving date, so it cannot be laid out before there is
     * one.
     */
    @Test
    void leavingChecklistNeedsATerminationDateFirst() throws Exception {
        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "OFFBOARDING").with(jwtFor(hrAdmin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("ayrılış tarihi")));
    }

    /**
     * Negative offsets are the reason offboarding templates carry them: handover has to be finished
     * before the last day, not on it.
     */
    @Test
    void leavingChecklistSchedulesHandoverBeforeTheLastDay() throws Exception {
        employee.terminate(LocalDate.of(2026, 9, 30));
        employeeRepository.save(employee);

        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "OFFBOARDING").with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(4))
                .andExpect(jsonPath("$.anchorDate").value("2026-09-30"))
                .andExpect(jsonPath("$.tasks[0].taskName").value("Devir teslim tamamlanması"))
                .andExpect(jsonPath("$.tasks[0].dueDate").value("2026-09-23"))
                .andExpect(jsonPath("$.tasks[1].dueDate").value("2026-09-30"))
                .andExpect(jsonPath("$.tasks[3].dueDate").value("2026-09-29"));
    }

    // --- who may do what ---

    @Test
    void assigneeCanCompleteTheirOwnItemButNotSomeoneElses() throws Exception {
        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").param("assignTo", itUser.getId().toString())
                        .with(jwtFor(hrAdmin)))
                .andExpect(status().isOk());
        UUID taskId = firstTaskId("ONBOARDING");

        mockMvc.perform(patch("/api/hr/onboarding-tasks/{id}/complete", taskId).with(jwtFor(outsider)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/hr/onboarding-tasks/{id}/complete", taskId).with(jwtFor(itUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedBy").value(itUser.getId().toString()));
    }

    @Test
    void assigneeSeesOnlyTheirOwnItems() throws Exception {
        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").param("assignTo", itUser.getId().toString())
                        .with(jwtFor(hrAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/onboarding-tasks").with(jwtFor(itUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(get("/api/me/onboarding-tasks").with(jwtFor(outsider)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anAlreadyCompletedItemCannotBeCompletedTwiceButCanBeReopened() throws Exception {
        applyTemplate("ONBOARDING");
        UUID taskId = firstTaskId("ONBOARDING");

        mockMvc.perform(patch("/api/hr/onboarding-tasks/{id}/complete", taskId).with(jwtFor(hrAdmin)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/hr/onboarding-tasks/{id}/complete", taskId).with(jwtFor(hrAdmin)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/hr/onboarding-tasks/{id}/reopen", taskId).with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void adHocTaskCanBeAddedOutsideTheTemplate() throws Exception {
        String body = objectMapper.writeValueAsString(new TaskBody(
                employee.getId(), "Araç tahsisi", "ONBOARDING", LocalDate.of(2024, 3, 15),
                itUser.getId()));

        mockMvc.perform(post("/api/hr/onboarding-tasks").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskName").value("Araç tahsisi"))
                .andExpect(jsonPath("$.overdue").value(true));
    }

    @Test
    void duplicateTemplateNameWithinTheSameTypeIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(new TemplateBody(
                "ekipman teslimi (dizüstü, telefon)", "ONBOARDING", 9, 0));

        mockMvc.perform(post("/api/hr/onboarding-task-templates").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void checklistManagementIsClosedToOrdinaryEmployees() throws Exception {
        mockMvc.perform(get("/api/hr/onboarding-tasks").with(jwtFor(outsider)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", "ONBOARDING").with(jwtFor(outsider)))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private void applyTemplate(String type) throws Exception {
        mockMvc.perform(post("/api/hr/employees/{id}/checklist", employee.getId())
                        .param("type", type).with(jwtFor(hrAdmin)))
                .andExpect(status().isOk());
    }

    private UUID firstTaskId(String type) throws Exception {
        String response = mockMvc.perform(get("/api/hr/onboarding-tasks")
                        .param("employeeId", employee.getId().toString()).param("type", type)
                        .with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get(0).get("id").asString());
    }

    private record TaskBody(UUID employeeId, String taskName, String taskType, LocalDate dueDate,
            UUID assignedTo) {
    }

    private record TemplateBody(String name, String taskType, int displayOrder, int offsetDays) {
    }
}
