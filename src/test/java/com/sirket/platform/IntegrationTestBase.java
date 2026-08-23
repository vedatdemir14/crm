package com.sirket.platform;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
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

    /**
     * Because the container is shared, each test starts from a clean slate. Plain deletes are not
     * enough: soft-deleted rows physically remain and keep their foreign keys onto common.users,
     * so the tables are truncated instead. common.roles is seeded by migration and must survive.
     */
    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    crm.contacts,
                    crm.companies,
                    common.refresh_tokens,
                    common.user_roles,
                    common.audit_logs,
                    common.users
                CASCADE
                """);
    }
}
