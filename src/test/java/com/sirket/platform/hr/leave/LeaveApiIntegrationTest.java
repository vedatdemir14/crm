package com.sirket.platform.hr.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.hr.employee.domain.Department;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.domain.EmploymentType;
import com.sirket.platform.hr.employee.repository.DepartmentRepository;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import com.sirket.platform.hr.leave.domain.PublicHoliday;
import com.sirket.platform.hr.leave.repository.LeaveTypeRepository;
import com.sirket.platform.hr.leave.repository.PublicHolidayRepository;
import com.sirket.platform.hr.leave.service.WorkingDayCalculator;
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
class LeaveApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private PublicHolidayRepository holidayRepository;

    @Autowired
    private WorkingDayCalculator workingDayCalculator;

    @Autowired
    private ObjectMapper objectMapper;

    private User hrAdminUser;
    private User managerUser;
    private User employeeUser;
    private User otherManagerUser;
    private Employee employee;
    private UUID annualLeaveId;

    @BeforeEach
    void seedLeaveData() {
        hrAdminUser = createUser("leave-hr", "ROLE_HR_ADMIN");
        managerUser = createUser("leave-manager", "ROLE_EMPLOYEE");
        employeeUser = createUser("leave-employee", "ROLE_EMPLOYEE");
        otherManagerUser = createUser("leave-other-manager", "ROLE_EMPLOYEE");

        Department dept = departmentRepository.save(new Department("Mühendislik", null));
        Employee manager = employeeRepository.save(new Employee(managerUser.getId(), "Zeynep", "Kaya",
                "zeynep@sirket.test", null, null, LocalDate.of(1985, 1, 1), LocalDate.of(2020, 1, 1),
                EmploymentType.FULL_TIME, dept, "Müdür", null));
        employeeRepository.save(new Employee(otherManagerUser.getId(), "Baska", "Yonetici",
                "baska@sirket.test", null, null, LocalDate.of(1985, 1, 1), LocalDate.of(2020, 1, 1),
                EmploymentType.FULL_TIME, dept, "Müdür", null));
        employee = employeeRepository.save(new Employee(employeeUser.getId(), "Ayşe", "Yılmaz",
                "ayse@sirket.test", null, null, LocalDate.of(1990, 5, 12), LocalDate.of(2024, 3, 1),
                EmploymentType.FULL_TIME, dept, "Uzman", manager));

        annualLeaveId = leaveTypeRepository.findByNameIgnoreCase("Yıllık İzin").orElseThrow().getId();
    }

    // --- FR-HR-03: working-day arithmetic ---

    /**
     * 2026-08-17 is a Monday. Counting calendar days would charge the employee 7 days for a range
     * that only contains 5 working days.
     */
    @Test
    void weekendsAreNotCountedAsLeave() {
        int days = workingDayCalculator.countWorkingDays(
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23));
        assertEquals(5, days, "Pazartesi-Pazar aralığı 5 iş günü olmalı");
    }

    @Test
    void publicHolidaysAreNotCountedAsLeave() {
        holidayRepository.save(new PublicHoliday(LocalDate.of(2026, 8, 19), "Test Tatili"));

        int days = workingDayCalculator.countWorkingDays(
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 21));
        assertEquals(4, days, "hafta içi bir resmi tatil düşülmeli");
    }

    @Test
    void aSingleWorkingDayCountsAsOne() {
        assertEquals(1, workingDayCalculator.countWorkingDays(
                LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 18)));
    }

    @Test
    void rangeMadeUpEntirelyOfNonWorkingDaysCountsAsZero() {
        // 2026-08-22 and 23 are Saturday and Sunday
        assertEquals(0, workingDayCalculator.countWorkingDays(
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 23)));
    }

    @Test
    void requestCoveringOnlyWeekendIsRejected() throws Exception {
        mockMvc.perform(post("/api/me/leave-requests").with(jwtFor(employeeUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2026-08-22", "2026-08-23")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void workingDaysEndpointReportsTheCostBeforeRequesting() throws Exception {
        holidayRepository.save(new PublicHoliday(LocalDate.of(2026, 8, 19), "Test Tatili"));

        mockMvc.perform(get("/api/hr/working-days")
                        .param("from", "2026-08-17").param("to", "2026-08-21")
                        .with(jwtFor(employeeUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workingDays").value(4));
    }

    // --- FR-HR-02: request and approval flow ---

    @Test
    void requestIsCreatedPendingWithWorkingDaysOnly() throws Exception {
        mockMvc.perform(post("/api/me/leave-requests").with(jwtFor(employeeUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2026-08-17", "2026-08-23")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.daysCount").value(5))
                .andExpect(jsonPath("$.employeeName").value("Ayşe Yılmaz"));
    }

    @Test
    void approvalByTheEmployeesManagerDeductsFromTheBalance() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");

        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId).with(jwtFor(managerUser))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Onaylandı\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approverId").value(managerUser.getId().toString()));

        mockMvc.perform(get("/api/me/leave-balance").param("year", "2026").with(jwtFor(employeeUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leaveTypeName=='Yıllık İzin')].usedDays").value(5))
                .andExpect(jsonPath("$[?(@.leaveTypeName=='Yıllık İzin')].remainingDays").value(9));
    }

    @Test
    void rejectionLeavesTheBalanceUntouched() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");

        mockMvc.perform(patch("/api/hr/leave-requests/{id}/reject", requestId).with(jwtFor(managerUser))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Yoğunluk var\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/me/leave-balance").param("year", "2026").with(jwtFor(employeeUser)))
                .andExpect(jsonPath("$[?(@.leaveTypeName=='Yıllık İzin')].usedDays").value(0));
    }

    /**
     * Approving another team's leave is not a manager's call, so only the employee's own manager or
     * HR may decide.
     */
    @Test
    void anUnrelatedManagerCannotDecideTheRequest() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");

        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId)
                        .with(jwtFor(otherManagerUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void hrAdminCanAlsoDecide() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");

        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId).with(jwtFor(hrAdminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void aDecidedRequestCannotBeDecidedAgain() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");
        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId).with(jwtFor(managerUser)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/hr/leave-requests/{id}/reject", requestId).with(jwtFor(managerUser)))
                .andExpect(status().isConflict());
    }

    /**
     * Letting an approval overdraw the entitlement is the kind of error nobody notices until
     * payroll, so it is refused outright.
     */
    @Test
    void approvalIsRefusedWhenTheBalanceIsInsufficient() throws Exception {
        // Annual leave defaults to 14 days; ask for more in one go.
        UUID requestId = createRequest("2026-09-01", "2026-09-30");

        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId).with(jwtFor(managerUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("bakiye")));

        // and it stays pending rather than half-applying
        mockMvc.perform(get("/api/me/leave-requests").with(jwtFor(employeeUser)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void overlappingRequestIsRejected() throws Exception {
        createRequest("2026-08-17", "2026-08-21");

        mockMvc.perform(post("/api/me/leave-requests").with(jwtFor(employeeUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("2026-08-19", "2026-08-25")))
                .andExpect(status().isConflict());
    }

    @Test
    void employeeCanCancelTheirOwnPendingRequest() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");

        mockMvc.perform(delete("/api/me/leave-requests/{id}", requestId).with(jwtFor(employeeUser)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me/leave-requests").with(jwtFor(employeeUser)))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
    }

    @Test
    void anApprovedRequestCanNoLongerBeCancelled() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");
        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId).with(jwtFor(managerUser)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/me/leave-requests/{id}", requestId).with(jwtFor(employeeUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void userWithoutAnEmployeeRecordGetsAClearAnswer() throws Exception {
        User orphan = createUser("no-employee", "ROLE_EMPLOYEE");

        mockMvc.perform(get("/api/me/leave-balance").with(jwtFor(orphan)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("çalışan kaydı")));
    }

    @Test
    void balanceIsReportedFromTheDefaultEntitlementBeforeAnyLeaveIsTaken() throws Exception {
        mockMvc.perform(get("/api/me/leave-balance").param("year", "2026").with(jwtFor(employeeUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.leaveTypeName=='Yıllık İzin')].totalDays").value(14))
                .andExpect(jsonPath("$[?(@.leaveTypeName=='Yıllık İzin')].remainingDays").value(14));
    }

    @Test
    void hrCanCorrectTheEntitlementButNotBelowWhatIsAlreadyUsed() throws Exception {
        UUID requestId = createRequest("2026-08-17", "2026-08-21");
        mockMvc.perform(patch("/api/hr/leave-requests/{id}/approve", requestId).with(jwtFor(managerUser)))
                .andExpect(status().isOk());

        String body = objectMapper.writeValueAsString(
                new AdjustBody(annualLeaveId, 2026, 20));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/hr/employees/{id}/leave-balance", employee.getId())
                        .with(jwtFor(hrAdminUser))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDays").value(20))
                .andExpect(jsonPath("$.remainingDays").value(15));

        String tooLow = objectMapper.writeValueAsString(new AdjustBody(annualLeaveId, 2026, 3));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/hr/employees/{id}/leave-balance", employee.getId())
                        .with(jwtFor(hrAdminUser))
                        .contentType(MediaType.APPLICATION_JSON).content(tooLow))
                .andExpect(status().isBadRequest());
    }

    @Test
    void seededLeaveTypesAreAvailableAndDuplicateNamesAreRejected() throws Exception {
        mockMvc.perform(get("/api/hr/leave-types").with(jwtFor(employeeUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mockMvc.perform(post("/api/hr/leave-types").with(jwtFor(hrAdminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"yıllık izin\",\"paid\":true,\"defaultAnnualDays\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicatePublicHolidayDateIsRejected() throws Exception {
        mockMvc.perform(post("/api/hr/public-holidays").with(jwtFor(hrAdminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-29\",\"name\":\"Cumhuriyet Bayramı\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/hr/public-holidays").with(jwtFor(hrAdminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-29\",\"name\":\"Tekrar\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void employeesCannotManageTheHolidayCalendar() throws Exception {
        mockMvc.perform(post("/api/hr/public-holidays").with(jwtFor(employeeUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-29\",\"name\":\"Cumhuriyet Bayramı\"}"))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private UUID createRequest(String start, String end) throws Exception {
        String response = mockMvc.perform(post("/api/me/leave-requests").with(jwtFor(employeeUser))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(start, end)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private String requestBody(String start, String end) {
        return objectMapper.writeValueAsString(
                new RequestBody(annualLeaveId, start, end, "gerekçe"));
    }

    private record RequestBody(UUID leaveTypeId, String startDate, String endDate, String reason) {
    }

    private record AdjustBody(UUID leaveTypeId, int year, int totalDays) {
    }
}
