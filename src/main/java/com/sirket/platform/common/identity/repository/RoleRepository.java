package com.sirket.platform.common.identity.repository;

import com.sirket.platform.common.identity.domain.Role;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    Set<Role> findByNameIn(Set<String> names);
}
