package com.sirket.platform.hr.onboarding.repository;

import com.sirket.platform.hr.onboarding.domain.OnboardingTask;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskStatus;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, UUID> {

    /**
     * Undated items sort last: a checklist is read by what is due soonest, and something with no
     * date yet is not the most urgent thing on it.
     */
    @Query("""
            SELECT t FROM OnboardingTask t
            JOIN FETCH t.employee
            WHERE (:employeeId IS NULL OR t.employee.id = :employeeId)
              AND (:taskType IS NULL OR t.taskType = :taskType)
              AND (:status IS NULL OR t.status = :status)
              AND (:assignedTo IS NULL OR t.assignedTo = :assignedTo)
            ORDER BY CASE WHEN t.dueDate IS NULL THEN 1 ELSE 0 END, t.dueDate ASC, t.createdAt ASC
            """)
    List<OnboardingTask> search(@Param("employeeId") UUID employeeId,
            @Param("taskType") OnboardingTaskType taskType,
            @Param("status") OnboardingTaskStatus status,
            @Param("assignedTo") UUID assignedTo);

    List<OnboardingTask> findByEmployeeIdAndTaskType(UUID employeeId, OnboardingTaskType taskType);

    boolean existsByEmployeeIdAndTaskTypeAndTaskName(UUID employeeId, OnboardingTaskType taskType,
            String taskName);
}
