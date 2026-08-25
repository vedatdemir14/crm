package com.sirket.platform.hr.employee.repository;

import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.domain.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * Nullable string parameters are cast explicitly: PostgreSQL cannot infer the type of a null
     * parameter and falls back to bytea, which makes lower() fail at runtime.
     */
    @Query(value = """
            SELECT e FROM Employee e
            LEFT JOIN FETCH e.department
            LEFT JOIN FETCH e.manager
            WHERE (CAST(:name AS String) IS NULL
                   OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%')))
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
              AND (:managerId IS NULL OR e.manager.id = :managerId)
              AND (:status IS NULL OR e.status = :status)
            """,
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            WHERE (CAST(:name AS String) IS NULL
                   OR LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%')))
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
              AND (:managerId IS NULL OR e.manager.id = :managerId)
              AND (:status IS NULL OR e.status = :status)
            """)
    Page<Employee> search(@Param("name") String name,
            @Param("departmentId") UUID departmentId,
            @Param("managerId") UUID managerId,
            @Param("status") EmployeeStatus status,
            Pageable pageable);

    /** Direct headcount per department; the org chart rolls these up through the tree. */
    @Query("""
            SELECT new com.sirket.platform.hr.employee.repository.DepartmentHeadcount(e.department.id, COUNT(e))
            FROM Employee e
            WHERE e.department IS NOT NULL AND e.status <> com.sirket.platform.hr.employee.domain.EmployeeStatus.TERMINATED
            GROUP BY e.department.id
            """)
    List<DepartmentHeadcount> headcountByDepartment();

    Optional<Employee> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByDepartmentId(UUID departmentId);

    boolean existsByManagerId(UUID managerId);
}
