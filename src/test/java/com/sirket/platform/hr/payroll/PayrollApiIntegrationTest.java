package com.sirket.platform.hr.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirket.platform.IntegrationTestBase;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.domain.EmploymentType;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import com.sirket.platform.hr.payroll.domain.PayrollRecord;
import com.sirket.platform.hr.payroll.service.PayrollService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PayrollApiIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private JdbcTemplate jdbc;

    private User hrAdminUser;
    private User employeeUser;
    private User otherUser;
    private Employee employee;

    @BeforeEach
    void seedPayrollData() {
        hrAdminUser = createUser("pay-hr", "ROLE_HR_ADMIN");
        employeeUser = createUser("pay-employee", "ROLE_EMPLOYEE");
        otherUser = createUser("pay-other", "ROLE_EMPLOYEE");

        employee = employeeRepository.save(new Employee(employeeUser.getId(), "Ayşe", "Yılmaz",
                "ayse@sirket.test", null, null, LocalDate.of(1990, 5, 12), LocalDate.of(2024, 3, 1),
                EmploymentType.FULL_TIME, null, "Uzman", null));
        employeeRepository.save(new Employee(otherUser.getId(), "Mehmet", "Demir",
                "mehmet@sirket.test", null, null, LocalDate.of(1990, 5, 12), LocalDate.of(2024, 3, 1),
                EmploymentType.FULL_TIME, null, "Uzman", null));
    }

    @Test
    void payslipsAreReportedAndFlaggedAsMock() throws Exception {
        mockMvc.perform(get("/api/hr/employees/{id}/payroll-records", employee.getId())
                        .param("months", "3").with(jwtFor(hrAdminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].mock").value(true))
                .andExpect(jsonPath("$[0].currency").value("TRY"))
                .andExpect(jsonPath("$[0].employeeName").value("Ayşe Yılmaz"));
    }

    /**
     * The whole reason the mock persists its output: a payslip whose amounts moved between two
     * requests would make every screen that shows them untestable.
     */
    @Test
    void repeatedCallsReturnTheSameAmounts() {
        List<PayrollRecord> first = payrollService.recordsFor(employee, 3);
        List<PayrollRecord> second = payrollService.recordsFor(employee, 3);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getId(), second.get(i).getId(), "aynı dönem aynı kaydı döndürmeli");
            assertEquals(0, first.get(i).getGrossAmount().compareTo(second.get(i).getGrossAmount()));
            assertEquals(0, first.get(i).getNetAmount().compareTo(second.get(i).getNetAmount()));
        }
    }

    @Test
    void netIsLowerThanGrossAndDeductionsAddUp() {
        PayrollRecord record = payrollService.recordsFor(employee, 1).getFirst();

        assertTrue(record.getNetAmount().compareTo(record.getGrossAmount()) < 0,
                "net tutar brütten küçük olmalı");
        assertEquals(0, record.getGrossAmount().subtract(record.getNetAmount())
                .compareTo(record.getTotalDeductions()), "kesinti = brüt - net olmalı");
        assertTrue(record.getNetAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Amounts are personal data; the converter has to be doing real work, not passing the value
     * through. Reading the raw column is the only way to be sure.
     */
    @Test
    void amountsAreStoredEncryptedNotAsReadableNumbers() {
        PayrollRecord record = payrollService.recordsFor(employee, 1).getFirst();
        String plainGross = record.getGrossAmount().toPlainString();

        String stored = jdbc.queryForObject(
                "SELECT gross_amount FROM hr.payroll_records WHERE id = ?", String.class, record.getId());

        assertNotNull(stored);
        assertFalse(stored.contains(plainGross), "brüt tutar veritabanında okunabilir durmamalı: " + stored);

        Long readableRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM hr.payroll_records WHERE gross_amount LIKE ?", Long.class,
                "%" + plainGross + "%");
        assertEquals(0L, readableRows);
    }

    @Test
    void noPayslipIsProducedForMonthsBeforeTheEmployeeWasHired() {
        Employee recentHire = employeeRepository.save(new Employee(null, "Yeni", "Başlayan",
                "yeni@sirket.test", null, null, LocalDate.of(1995, 1, 1), LocalDate.now().minusMonths(1),
                EmploymentType.FULL_TIME, null, "Uzman", null));

        List<PayrollRecord> records = payrollService.recordsFor(recentHire, 12);

        assertEquals(2, records.size(), "işe girişten önceki aylar için bordro üretilmemeli");
        YearMonth hired = YearMonth.from(recentHire.getHireDate());
        records.forEach(record ->
                assertFalse(record.getPeriod().isBefore(hired), "dönem işe giriş ayından önce olamaz"));
    }

    @Test
    void noPayslipIsProducedAfterTermination() {
        employee.terminate(LocalDate.now().minusMonths(2));
        employeeRepository.save(employee);

        List<PayrollRecord> records = payrollService.recordsFor(employee, 6);

        YearMonth terminated = YearMonth.from(employee.getTerminationDate());
        records.forEach(record ->
                assertFalse(record.getPeriod().isAfter(terminated), "ayrılıştan sonra bordro üretilmemeli"));
    }

    @Test
    void twoEmployeesGetDifferentSalaries() {
        Employee other = employeeRepository.findByUserId(otherUser.getId()).orElseThrow();

        BigDecimal mine = payrollService.recordsFor(employee, 1).getFirst().getGrossAmount();
        BigDecimal theirs = payrollService.recordsFor(other, 1).getFirst().getGrossAmount();

        assertFalse(mine.compareTo(theirs) == 0, "farklı çalışanlar aynı maaşı almamalı");
    }

    // --- access ---

    @Test
    void employeeSeesOwnPayslipsThroughSelfService() throws Exception {
        mockMvc.perform(get("/api/me/payroll-records").param("months", "2").with(jwtFor(employeeUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$[0].mock").value(true));
    }

    @Test
    void employeeCannotReadSomeoneElsesPayslip() throws Exception {
        UUID recordId = payrollService.recordsFor(employee, 1).getFirst().getId();

        mockMvc.perform(get("/api/hr/payroll-records/{id}", recordId).with(jwtFor(otherUser)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/hr/payroll-records/{id}", recordId).with(jwtFor(employeeUser)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hr/payroll-records/{id}", recordId).with(jwtFor(hrAdminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void anEmployeeCannotBrowseAnotherEmployeesPayrollList() throws Exception {
        mockMvc.perform(get("/api/hr/employees/{id}/payroll-records", employee.getId())
                        .with(jwtFor(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userWithoutAnEmployeeRecordGetsAClearAnswer() throws Exception {
        User orphan = createUser("pay-orphan", "ROLE_EMPLOYEE");

        mockMvc.perform(get("/api/me/payroll-records").with(jwtFor(orphan)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("çalışan kaydı")));
    }

    @Test
    void theProviderReportsItselfAsAMock() {
        assertTrue(payrollService.isMockProvider(),
                "ilk sürümde bordro sağlayıcısı kendini mock olarak bildirmeli");
    }
}
