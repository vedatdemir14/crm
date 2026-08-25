package com.sirket.platform.hr.employee.repository;

import com.sirket.platform.hr.employee.domain.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findAllByOrderByNameAsc();

    @Query("SELECT d FROM Department d WHERE LOWER(d.name) = LOWER(CAST(:name AS String))")
    Optional<Department> findByNameIgnoreCase(@Param("name") String name);

    boolean existsByParentId(UUID parentId);
}
