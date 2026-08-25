package com.sirket.platform.hr.leave.repository;

import com.sirket.platform.hr.leave.domain.LeaveRequest;
import com.sirket.platform.hr.leave.domain.LeaveStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    @Query(value = """
            SELECT r FROM LeaveRequest r
            JOIN FETCH r.employee e
            JOIN FETCH r.leaveType
            WHERE (:employeeId IS NULL OR e.id = :employeeId)
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.requestedAt DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM LeaveRequest r
            WHERE (:employeeId IS NULL OR r.employee.id = :employeeId)
              AND (:departmentId IS NULL OR r.employee.department.id = :departmentId)
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<LeaveRequest> search(@Param("employeeId") UUID employeeId,
            @Param("departmentId") UUID departmentId,
            @Param("status") LeaveStatus status,
            Pageable pageable);

    Page<LeaveRequest> findByEmployeeIdOrderByRequestedAtDesc(UUID employeeId, Pageable pageable);

    /**
     * Requests that still hold the calendar: pending ones are not yet decided and approved ones are
     * booked, so either blocks an overlapping request.
     */
    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.employee.id = :employeeId
              AND r.status IN (com.sirket.platform.hr.leave.domain.LeaveStatus.PENDING,
                               com.sirket.platform.hr.leave.domain.LeaveStatus.APPROVED)
              AND r.startDate <= :endDate
              AND r.endDate >= :startDate
            """)
    List<LeaveRequest> findBlockingOverlaps(@Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
