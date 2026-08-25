package com.sirket.platform.hr.leave.repository;

import com.sirket.platform.hr.leave.domain.LeaveBalance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(UUID employeeId, UUID leaveTypeId, int year);

    List<LeaveBalance> findByEmployeeIdAndYearOrderByLeaveTypeNameAsc(UUID employeeId, int year);
}
