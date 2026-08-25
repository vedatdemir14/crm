package com.sirket.platform.hr.payroll.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.security.CurrentUser;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import com.sirket.platform.hr.payroll.domain.PayrollRecord;
import com.sirket.platform.hr.payroll.dto.PayrollDtos;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access rules around payroll. Kept out of {@link PayrollService} on purpose: who is allowed to see
 * a payslip is the platform's business, not the payroll provider's, so a real provider
 * implementation will not have to reimplement any of this.
 */
@Service
public class PayrollQueryService {

    private static final String ROLE_HR_ADMIN = "ROLE_HR_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PayrollService payrollService;
    private final EmployeeRepository employeeRepository;
    private final CurrentUser currentUser;

    public PayrollQueryService(PayrollService payrollService, EmployeeRepository employeeRepository,
            CurrentUser currentUser) {
        this.payrollService = payrollService;
        this.employeeRepository = employeeRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public List<PayrollDtos.PayrollRecordResponse> recordsOfEmployee(UUID employeeId, int monthsBack) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiExceptions.NotFound("Çalışan bulunamadı: " + employeeId));
        return toResponses(payrollService.recordsFor(employee, monthsBack));
    }

    @Transactional
    public List<PayrollDtos.PayrollRecordResponse> myRecords(int monthsBack) {
        return toResponses(payrollService.recordsFor(currentEmployee(), monthsBack));
    }

    /**
     * A payslip is readable by HR or by the person it belongs to. Anyone else is told it does not
     * exist rather than that it is forbidden, so the API does not confirm which employees have
     * payroll records.
     */
    @Transactional(readOnly = true)
    public PayrollDtos.PayrollRecordResponse get(UUID id) {
        PayrollRecord record = payrollService.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Bordro kaydı bulunamadı: " + id));

        if (!currentUser.hasAnyRole(ROLE_HR_ADMIN, ROLE_ADMIN)) {
            UUID ownerUserId = record.getEmployee().getUserId();
            if (ownerUserId == null || !ownerUserId.equals(currentUser.id())) {
                throw new ApiExceptions.NotFound("Bordro kaydı bulunamadı: " + id);
            }
        }
        return PayrollDtos.PayrollRecordResponse.from(record);
    }

    private List<PayrollDtos.PayrollRecordResponse> toResponses(List<PayrollRecord> records) {
        return records.stream().map(PayrollDtos.PayrollRecordResponse::from).toList();
    }

    private Employee currentEmployee() {
        return employeeRepository.findByUserId(currentUser.id())
                .orElseThrow(() -> new ApiExceptions.NotFound(
                        "Kullanıcı hesabınıza bağlı bir çalışan kaydı yok, İK ile iletişime geçin"));
    }
}
