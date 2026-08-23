package com.sirket.platform.config;

import com.sirket.platform.common.identity.domain.Role;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.repository.RoleRepository;
import com.sirket.platform.common.identity.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a first admin so a fresh development database is usable. Never active outside the dev profile —
 * production accounts are provisioned deliberately.
 */
@Configuration
@Profile("dev")
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    ApplicationRunner seedAdmin(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing — did migrations run?"));

            String configured = environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD");
            String password = (configured != null && !configured.isBlank()) ? configured : randomPassword();

            User admin = new User("admin", "admin@localhost", passwordEncoder.encode(password));
            admin.replaceRoles(Set.of(adminRole));
            userRepository.save(admin);

            if (configured == null || configured.isBlank()) {
                log.warn("Seeded dev admin user 'admin' with generated password: {}", password);
                log.warn("This is printed once. Set ADMIN_BOOTSTRAP_PASSWORD to choose your own.");
            }
            else {
                log.info("Seeded dev admin user 'admin' with the configured bootstrap password.");
            }
        };
    }

    private String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
