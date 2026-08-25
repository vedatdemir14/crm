package com.sirket.platform.hr.payroll.repository;

import com.sirket.platform.hr.payroll.domain.PayrollRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, UUID> {

    Optional<PayrollRecord> findByEmployeeIdAndPeriod(UUID employeeId, String period);
}
