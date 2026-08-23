package com.sirket.platform.common.security;

import com.sirket.platform.common.error.ApiExceptions;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated user out of the security context. Roles come from the JWT's
 * {@code roles} claim and already carry the {@code ROLE_} prefix.
 */
@Component
public class CurrentUser {

    public UUID id() {
        Authentication authentication = authentication();
        try {
            return UUID.fromString(authentication.getName());
        }
        catch (IllegalArgumentException ex) {
            throw new ApiExceptions.Unauthorized("Kimlik bilgisi çözümlenemedi");
        }
    }

    public boolean hasRole(String role) {
        return authentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiExceptions.Unauthorized("Kimlik doğrulanmamış");
        }
        return authentication;
    }
}
