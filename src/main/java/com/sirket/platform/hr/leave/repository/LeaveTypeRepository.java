package com.sirket.platform.hr.leave.repository;

import com.sirket.platform.hr.leave.domain.LeaveType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findAllByOrderByNameAsc();

    @Query("SELECT t FROM LeaveType t WHERE LOWER(t.name) = LOWER(CAST(:name AS String))")
    Optional<LeaveType> findByNameIgnoreCase(@Param("name") String name);
}
