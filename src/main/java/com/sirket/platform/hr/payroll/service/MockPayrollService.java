package com.sirket.platform.hr.payroll.service;

import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.payroll.domain.PayrollRecord;
import com.sirket.platform.hr.payroll.repository.PayrollRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stand-in payroll provider for the first release (FR-HR-04).
 * <p>
 * <strong>These figures are invented.</strong> The deduction model below is a flat percentage and
 * is deliberately not an attempt at Turkish payroll law — no income tax brackets, no cumulative
 * base, no SGK ceiling, no minimum-wage exemption. It exists so the screens that display payslips
 * have something plausible to render, and every row it writes is flagged {@code is_mock}.
 * <p>
 * Records are generated once and then persisted, so the same period always reports the same
 * amounts. A payslip whose numbers changed between two page loads would make the consuming screens
 * impossible to test.
 * <p>
 * The bean steps aside automatically once a real {@link PayrollService} is registered.
 */
@Service
@ConditionalOnMissingBean(ignored = MockPayrollService.class, value = PayrollService.class)
public class MockPayrollService implements PayrollService {

    /** Flat stand-in for tax and social security. Not a real payroll calculation. */
    private static final BigDecimal MOCK_DEDUCTION_RATE = new BigDecimal("0.32");

    private static final BigDecimal MIN_GROSS = new BigDecimal("35000");
    private static final BigDecimal GROSS_SPREAD = new BigDecimal("65000");

    private final PayrollRecordRepository payrollRecordRepository;
    private final String currency;

    public MockPayrollService(PayrollRecordRepository payrollRecordRepository,
            @Value("${hr.payroll.currency:TRY}") String currency) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.currency = currency;
    }

    @Override
    @Transactional
    public List<PayrollRecord> recordsFor(Employee employee, int monthsBack) {
        YearMonth current = YearMonth.now();
        YearMonth hired = YearMonth.from(employee.getHireDate());

        List<PayrollRecord> records = new ArrayList<>();
        for (int offset = 0; offset < monthsBack; offset++) {
            YearMonth period = current.minusMonths(offset);
            // Nobody is paid for months before they joined, and a terminated employee stops being
            // paid after they leave.
            if (period.isBefore(hired) || isAfterTermination(employee, period)) {
                continue;
            }
            records.add(findOrGenerate(employee, period));
        }
        return records;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PayrollRecord> findById(UUID id) {
        return payrollRecordRepository.findById(id);
    }

    @Override
    @Transactional
    public Optional<PayrollRecord> findFor(Employee employee, YearMonth period) {
        YearMonth hired = YearMonth.from(employee.getHireDate());
        if (period.isBefore(hired) || period.isAfter(YearMonth.now()) || isAfterTermination(employee, period)) {
            return Optional.empty();
        }
        return Optional.of(findOrGenerate(employee, period));
    }

    @Override
    public boolean isMockProvider() {
        return true;
    }

    private boolean isAfterTermination(Employee employee, YearMonth period) {
        return employee.getTerminationDate() != null
                && period.isAfter(YearMonth.from(employee.getTerminationDate()));
    }

    private PayrollRecord findOrGenerate(Employee employee, YearMonth period) {
        return payrollRecordRepository
                .findByEmployeeIdAndPeriod(employee.getId(), period.toString())
                .orElseGet(() -> payrollRecordRepository.save(generate(employee, period)));
    }

    /**
     * Gross is derived from the employee id so the same person always gets the same salary, and two
     * different people get different ones. It is a spread over a fixed range, not a real pay scale.
     */
    private PayrollRecord generate(Employee employee, YearMonth period) {
        int seed = Math.abs(employee.getId().hashCode() % 1000);
        BigDecimal gross = MIN_GROSS.add(
                GROSS_SPREAD.multiply(BigDecimal.valueOf(seed))
                        .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
        BigDecimal net = gross.multiply(BigDecimal.ONE.subtract(MOCK_DEDUCTION_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        return new PayrollRecord(employee, period, gross.setScale(2, RoundingMode.HALF_UP), net,
                currency, true);
    }
}
