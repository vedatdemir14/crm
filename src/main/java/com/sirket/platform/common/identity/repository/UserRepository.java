package com.sirket.platform.common.identity.repository;

import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.domain.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN u.roles r
            WHERE (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR r.name = :role)
            """)
    Page<User> search(@Param("status") UserStatus status, @Param("role") String role, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
