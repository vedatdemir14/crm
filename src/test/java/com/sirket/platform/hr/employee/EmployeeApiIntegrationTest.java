package com.sirket.platform.hr.employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiIntegrationTest extends IntegrationTestBase {

    private static final String NATIONAL_ID = "12345678901";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    private User hrAdmin;
    private User salesRep;
    private UUID engineeringId;

    @BeforeEach
    void seedHrData() throws Exception {
        hrAdmin = createUser("hr-admin", "ROLE_HR_ADMIN");
        salesRep = createUser("hr-sales-rep", "ROLE_SALES_REP");
        engineeringId = createDepartment("Mühendislik", null);
    }

    // --- FR-HR-01: employee records ---

    @Test
    void employeeIsCreatedActiveWithItsDepartment() throws Exception {
        mockMvc.perform(post("/api/hr/employees").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.firstName").value("Ayşe"))
                .andExpect(jsonPath("$.departmentName").value("Mühendislik"));
    }

    /**
     * The point of the converter is that the column holds ciphertext. Reading the raw value through
     * plain JDBC is the only way to prove the encryption is not quietly a no-op.
     */
    @Test
    void nationalIdIsStoredAsCiphertextNotPlainText() throws Exception {
        UUID id = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);

        String stored = jdbc.queryForObject(
                "SELECT national_id FROM hr.employees WHERE id = ?", String.class, id);

        assertNotNull(stored);
        assertFalse(stored.contains(NATIONAL_ID), "kimlik numarası veritabanında düz metin durmamalı: " + stored);
        assertTrue(stored.length() > NATIONAL_ID.length(), "şifreli değer IV ve etiket nedeniyle daha uzun olmalı");

        // and it still round-trips back through the application
        mockMvc.perform(get("/api/hr/employees/{id}", id)
                        .param("includeNationalId", "true").with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nationalId").value(NATIONAL_ID));
    }

    /**
     * A random IV per value means the same input encrypts differently every time, which is what
     * stops two employees sharing a national id from being identifiable by matching ciphertext.
     */
    @Test
    void twoEmployeesWithTheSameNationalIdProduceDifferentCiphertext() throws Exception {
        UUID first = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);
        UUID second = createEmployee("Fatma", "Kaya", NATIONAL_ID, engineeringId, null);

        String a = jdbc.queryForObject("SELECT national_id FROM hr.employees WHERE id = ?", String.class, first);
        String b = jdbc.queryForObject("SELECT national_id FROM hr.employees WHERE id = ?", String.class, second);

        assertFalse(a.equals(b), "aynı girdi her seferinde farklı şifreli değer üretmeli");
    }

    @Test
    void nationalIdIsMaskedUnlessExplicitlyRequested() throws Exception {
        UUID id = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);

        mockMvc.perform(get("/api/hr/employees/{id}", id).with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nationalId").value("*******8901"));
    }

    @Test
    void listResponseNeverCarriesTheNationalIdAtAll() throws Exception {
        createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);

        mockMvc.perform(get("/api/hr/employees").with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nationalId").value("*******8901"));
    }

    @Test
    void employeesCanBeFilteredByNameAndDepartment() throws Exception {
        UUID sales = createDepartment("Satış", null);
        createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);
        createEmployee("Mehmet", "Demir", "98765432109", sales, null);

        mockMvc.perform(get("/api/hr/employees").param("name", "demir").with(jwtFor(hrAdmin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Demir"));

        mockMvc.perform(get("/api/hr/employees").param("department", sales.toString()).with(jwtFor(hrAdmin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Mehmet"));
    }

    @Test
    void oneUserAccountCannotBeLinkedToTwoEmployees() throws Exception {
        User linked = createUser("linked-user", "ROLE_EMPLOYEE");
        mockMvc.perform(post("/api/hr/employees").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithUser("Ayşe", "Yılmaz", linked.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/hr/employees").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithUser("Mehmet", "Demir", linked.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void hireDateInTheFutureIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(new EmployeeBody(
                null, "İleri", "Tarihli", "ileri@sirket.test", "+90555", NATIONAL_ID,
                LocalDate.of(1990, 1, 1), LocalDate.now().plusDays(30), "FULL_TIME",
                engineeringId, "Uzman", null));

        mockMvc.perform(post("/api/hr/employees").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    // --- reporting line integrity ---

    @Test
    void anEmployeeCannotBeTheirOwnManager() throws Exception {
        UUID id = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);

        mockMvc.perform(put("/api/hr/employees/{id}", id).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, id)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Two people managing each other makes any walk up the reporting line — an approval chain, an
     * org chart — loop forever.
     */
    @Test
    void managerAssignmentCannotCreateAReportingCycle() throws Exception {
        UUID boss = createEmployee("Zeynep", "Kaya", "11111111111", engineeringId, null);
        UUID report = createEmployee("Ali", "Veli", "22222222222", engineeringId, boss);

        mockMvc.perform(put("/api/hr/employees/{id}", boss).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody("Zeynep", "Kaya", "11111111111", engineeringId, report)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    // --- termination ---

    @Test
    void terminationDeactivatesTheRecordInsteadOfDeletingIt() throws Exception {
        UUID id = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);

        mockMvc.perform(patch("/api/hr/employees/{id}/terminate", id).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminationDate\":\"2026-08-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.terminationDate").value("2026-08-01"));

        // the record is still there
        mockMvc.perform(get("/api/hr/employees/{id}", id).with(jwtFor(hrAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void terminationDateBeforeHireDateIsRejected() throws Exception {
        UUID id = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);

        mockMvc.perform(patch("/api/hr/employees/{id}/terminate", id).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminationDate\":\"2000-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Terminating a manager while people still report to them would orphan their reports and leave
     * the chart pointing at someone who has left.
     */
    @Test
    void managerWithReportsCannotBeTerminated() throws Exception {
        UUID boss = createEmployee("Zeynep", "Kaya", "11111111111", engineeringId, null);
        createEmployee("Ali", "Veli", "22222222222", engineeringId, boss);

        mockMvc.perform(patch("/api/hr/employees/{id}/terminate", boss).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminationDate\":\"2026-08-01\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aTerminatedEmployeeCannotBeReactivated() throws Exception {
        UUID id = createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, engineeringId, null);
        mockMvc.perform(patch("/api/hr/employees/{id}/terminate", id).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminationDate\":\"2026-08-01\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/hr/employees/{id}/status", id).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());
    }

    // --- FR-HR-10: departments and the org chart ---

    @Test
    void departmentHierarchyIsReturnedAsATreeWithHeadcounts() throws Exception {
        UUID backend = createDepartment("Backend", engineeringId);
        createDepartment("Frontend", engineeringId);
        createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, backend, null);
        createEmployee("Mehmet", "Demir", "98765432109", backend, null);
        createEmployee("Zeynep", "Kaya", "11111111111", engineeringId, null);

        mockMvc.perform(get("/api/hr/departments/org-chart").with(jwtFor(hrAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mühendislik"))
                // counts are direct members only, not rolled up from children
                .andExpect(jsonPath("$[0].employeeCount").value(1))
                .andExpect(jsonPath("$[0].children.length()").value(2))
                .andExpect(jsonPath("$[0].children[0].name").value("Backend"))
                .andExpect(jsonPath("$[0].children[0].employeeCount").value(2))
                .andExpect(jsonPath("$[0].children[1].employeeCount").value(0));
    }

    @Test
    void departmentCannotBecomeItsOwnAncestor() throws Exception {
        UUID backend = createDepartment("Backend", engineeringId);

        mockMvc.perform(put("/api/hr/departments/{id}", engineeringId).with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentBody("Mühendislik", backend))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateDepartmentNameIsRejected() throws Exception {
        mockMvc.perform(post("/api/hr/departments").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentBody("mühendislik", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void departmentWithChildrenOrEmployeesCannotBeDeleted() throws Exception {
        UUID backend = createDepartment("Backend", engineeringId);

        mockMvc.perform(delete("/api/hr/departments/{id}", engineeringId).with(jwtFor(hrAdmin)))
                .andExpect(status().isConflict());

        createEmployee("Ayşe", "Yılmaz", NATIONAL_ID, backend, null);
        mockMvc.perform(delete("/api/hr/departments/{id}", backend).with(jwtFor(hrAdmin)))
                .andExpect(status().isConflict());
    }

    @Test
    void hrEndpointsAreClosedToSalesRoles() throws Exception {
        mockMvc.perform(get("/api/hr/employees").with(jwtFor(salesRep)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hr/departments").with(jwtFor(salesRep)))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private UUID createDepartment(String name, UUID parentId) throws Exception {
        String response = mockMvc.perform(post("/api/hr/departments").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentBody(name, parentId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private UUID createEmployee(String firstName, String lastName, String nationalId, UUID departmentId,
            UUID managerId) throws Exception {
        String response = mockMvc.perform(post("/api/hr/employees").with(jwtFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeBody(firstName, lastName, nationalId, departmentId, managerId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private String employeeBody(String firstName, String lastName, String nationalId, UUID departmentId,
            UUID managerId) {
        return objectMapper.writeValueAsString(new EmployeeBody(
                null, firstName, lastName,
                firstName.toLowerCase() + "@sirket.test", "+905551112233", nationalId,
                LocalDate.of(1990, 5, 12), LocalDate.of(2024, 3, 1), "FULL_TIME",
                departmentId, "Uzman", managerId));
    }

    private String bodyWithUser(String firstName, String lastName, UUID userId) {
        return objectMapper.writeValueAsString(new EmployeeBody(
                userId, firstName, lastName, firstName.toLowerCase() + "@sirket.test", "+905551112233",
                NATIONAL_ID, LocalDate.of(1990, 5, 12), LocalDate.of(2024, 3, 1), "FULL_TIME",
                engineeringId, "Uzman", null));
    }

    private record EmployeeBody(UUID userId, String firstName, String lastName, String email, String phone,
            String nationalId, LocalDate birthDate, LocalDate hireDate, String employmentType,
            UUID departmentId, String positionTitle, UUID managerId) {
    }

    private record DepartmentBody(String name, UUID parentDepartmentId) {
    }
}
