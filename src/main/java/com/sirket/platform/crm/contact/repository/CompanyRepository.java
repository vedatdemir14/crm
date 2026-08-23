package com.sirket.platform.crm.contact.repository;

import com.sirket.platform.crm.contact.domain.Company;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /**
     * Optional string parameters are cast explicitly: when one is null PostgreSQL cannot infer its
     * type and falls back to bytea, which makes {@code lower()} fail at runtime.
     * <p>
     * {@code ownerFilter} is null for roles that may see every record, which leaves the
     * ownership predicate satisfied for all rows (FR-CRM-10).
     */
    @Query("""
            SELECT c FROM Company c
            WHERE (CAST(:name AS String) IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%')))
              AND (CAST(:industry AS String) IS NULL OR LOWER(c.industry) = LOWER(CAST(:industry AS String)))
              AND (:owner IS NULL OR c.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR c.ownerUserId = :ownerFilter)
            """)
    Page<Company> search(@Param("name") String name,
            @Param("industry") String industry,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter,
            Pageable pageable);
}
