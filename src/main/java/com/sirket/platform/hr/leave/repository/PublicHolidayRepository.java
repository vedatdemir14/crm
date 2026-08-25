package com.sirket.platform.hr.leave.repository;

import com.sirket.platform.hr.leave.domain.PublicHoliday;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    List<PublicHoliday> findAllByOrderByDateAsc();

    List<PublicHoliday> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    Optional<PublicHoliday> findByDate(LocalDate date);

    /** Only the dates are needed when counting working days. */
    @Query("SELECT h.date FROM PublicHoliday h WHERE h.date BETWEEN :from AND :to")
    List<LocalDate> findDatesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
