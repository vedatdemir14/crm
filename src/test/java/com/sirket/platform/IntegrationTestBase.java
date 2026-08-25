package com.sirket.platform;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.sirket.platform.common.identity.domain.Role;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.repository.RoleRepository;
import com.sirket.platform.common.identity.repository.UserRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton container shared by every integration test. JUnit's {@code @Container} lifecycle would
 * stop the container once the first test class finished, leaving later classes with a refused
 * connection, so the container is started once here and left to Testcontainers' reaper to remove
 * when the JVM exits.
 */
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Because the container is shared, each test starts from a clean slate. Plain deletes are not
     * enough: soft-deleted rows physically remain and keep their foreign keys onto common.users,
     * so the tables are truncated instead. common.roles and crm.pipeline_stages are seeded by
     * migration and must survive.
     */
    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    hr.payroll_records,
                    hr.leave_requests,
                    hr.leave_balances,
                    hr.public_holidays,
                    hr.employees,
                    hr.departments,
                    crm.activities,
                    crm.tasks,
                    common.notifications,
                    crm.opportunities,
                    crm.contacts,
                    crm.companies,
                    common.refresh_tokens,
                    common.user_roles,
                    common.audit_logs,
                    common.users
                CASCADE
                """);
    }

    protected User createUser(String username, String role) {
        Role assigned = roleRepository.findByName(role).orElseThrow();
        User user = new User(username, username + "@example.com", passwordEncoder.encode("test-password-1234"));
        user.replaceRoles(Set.of(assigned));
        return userRepository.save(user);
    }

    /**
     * Builds the same JWT shape the auth endpoint issues: subject is the user id and roles live in
     * a {@code roles} claim.
     */
    protected RequestPostProcessor jwtFor(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .toList();
        return jwt()
                .jwt(token -> token.subject(user.getId().toString()).claim("roles", roles))
                .authorities(authorities);
    }
}
