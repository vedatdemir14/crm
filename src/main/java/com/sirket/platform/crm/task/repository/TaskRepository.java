package com.sirket.platform.crm.task.repository;

import com.sirket.platform.crm.task.domain.Task;
import com.sirket.platform.crm.task.domain.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * {@code assigneeRestriction} is null for roles allowed to see the whole team's tasks.
     * {@code dueBefore} is widened to a far date by the service rather than left null, since
     * PostgreSQL cannot type a null date parameter.
     */
    @Query(value = """
            SELECT t FROM Task t
            LEFT JOIN FETCH t.relatedContact
            LEFT JOIN FETCH t.relatedOpportunity
            WHERE (:assignedTo IS NULL OR t.assignedTo = :assignedTo)
              AND (:assigneeRestriction IS NULL OR t.assignedTo = :assigneeRestriction)
              AND (:status IS NULL OR t.status = :status)
              AND t.dueDate <= :dueBefore
            ORDER BY t.dueDate ASC
            """,
            countQuery = """
            SELECT COUNT(t) FROM Task t
            WHERE (:assignedTo IS NULL OR t.assignedTo = :assignedTo)
              AND (:assigneeRestriction IS NULL OR t.assignedTo = :assigneeRestriction)
              AND (:status IS NULL OR t.status = :status)
              AND t.dueDate <= :dueBefore
            """)
    Page<Task> search(@Param("assignedTo") UUID assignedTo,
            @Param("assigneeRestriction") UUID assigneeRestriction,
            @Param("status") TaskStatus status,
            @Param("dueBefore") LocalDate dueBefore,
            Pageable pageable);

    /**
     * Open tasks due on or before the given date — the input to the reminder job (FR-CRM-07).
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.status = com.sirket.platform.crm.task.domain.TaskStatus.OPEN
              AND t.dueDate <= :dueBefore
            """)
    List<Task> findOpenTasksDueOnOrBefore(@Param("dueBefore") LocalDate dueBefore);
}
