package com.sirket.platform.hr.payroll.service;

import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.payroll.domain.PayrollRecord;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The seam the SRS asks for around payroll (FR-HR-04, Mimari §8). The provider is undecided — Logo,
 * Netsis, SAP HR or another — so everything above this interface is written against it and the
 * first release ships a mock behind it.
 * <p>
 * Swapping in a real provider means writing another implementation. The self-service portal and
 * anything else reading payslips go through these methods and will not need to change.
 */
public interface PayrollService {

    /**
     * Payslips for an employee, newest period first.
     *
     * @param monthsBack how far back to report
     */
    List<PayrollRecord> recordsFor(Employee employee, int monthsBack);

    Optional<PayrollRecord> findById(UUID id);

    Optional<PayrollRecord> findFor(Employee employee, YearMonth period);

    /**
     * Whether these figures are generated rather than supplied by a payroll provider. Callers use
     * it to label the data, so nobody mistakes a mock payslip for the real thing.
     */
    boolean isMockProvider();
}
