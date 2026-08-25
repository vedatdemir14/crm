package com.sirket.platform.crm.contact.repository;

import com.sirket.platform.crm.contact.domain.Contact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    /**
     * Optional string parameters are cast explicitly: when one is null PostgreSQL cannot infer its
     * type and falls back to bytea, which makes {@code lower()} fail at runtime.
     * <p>
     * {@code ownerFilter} is null for roles allowed to see every record, leaving the ownership
     * predicate satisfied for all rows (FR-CRM-10).
     */
    @Query(value = """
            SELECT c FROM Contact c LEFT JOIN FETCH c.company comp
            WHERE (CAST(:name AS String) IS NULL
                   OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%')))
              AND (:companyId IS NULL OR comp.id = :companyId)
              AND (CAST(:source AS String) IS NULL OR LOWER(c.source) = LOWER(CAST(:source AS String)))
              AND (:owner IS NULL OR c.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR c.ownerUserId = :ownerFilter)
            """,
            countQuery = """
            SELECT COUNT(c) FROM Contact c LEFT JOIN c.company comp
            WHERE (CAST(:name AS String) IS NULL
                   OR LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%')))
              AND (:companyId IS NULL OR comp.id = :companyId)
              AND (CAST(:source AS String) IS NULL OR LOWER(c.source) = LOWER(CAST(:source AS String)))
              AND (:owner IS NULL OR c.ownerUserId = :owner)
              AND (:ownerFilter IS NULL OR c.ownerUserId = :ownerFilter)
            """)
    Page<Contact> search(@Param("name") String name,
            @Param("companyId") UUID companyId,
            @Param("source") String source,
            @Param("owner") UUID owner,
            @Param("ownerFilter") UUID ownerFilter,
            Pageable pageable);

    /**
     * FR-CRM-02: finds records that would be duplicates. Matching is case-insensitive on e-mail
     * and exact on phone; either field alone is enough to flag a match.
     */
    @Query("""
            SELECT c FROM Contact c
            WHERE (CAST(:email AS String) IS NOT NULL AND LOWER(c.email) = LOWER(CAST(:email AS String)))
               OR (CAST(:phone AS String) IS NOT NULL AND c.phone = CAST(:phone AS String))
            """)
    List<Contact> findPotentialDuplicates(@Param("email") String email, @Param("phone") String phone);

    /**
     * FR-CRM-12: resolves message participants to contacts in a single query. Addresses are
     * compared case-insensitively, so the caller passes them already lower-cased.
     */
    @Query("SELECT c FROM Contact c WHERE LOWER(c.email) IN :emails")
    List<Contact> findByEmailInIgnoreCase(@Param("emails") List<String> emails);

    boolean existsByCompanyId(UUID companyId);
}
