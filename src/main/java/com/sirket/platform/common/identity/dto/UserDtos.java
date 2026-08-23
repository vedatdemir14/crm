package com.sirket.platform.common.identity.dto;

import com.sirket.platform.common.identity.domain.Role;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Email @Size(max = 255) String email,
            // Length over composition rules, per Kriptografi ve Güvenlik Standartları §2 (NIST 800-63B).
            @NotBlank @Size(min = 10, message = "Şifre en az 10 karakter olmalıdır") String password,
            @NotEmpty(message = "En az bir rol atanmalıdır") Set<String> roles) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Email @Size(max = 255) String email) {
    }

    public record UpdateStatusRequest(@NotNull UserStatus status) {
    }

    public record AssignRolesRequest(@NotEmpty Set<String> roles) {
    }

    public record UserResponse(
            UUID id,
            String username,
            String email,
            UserStatus status,
            Set<String> roles,
            Instant lastLoginAt,
            Instant createdAt) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getStatus(),
                    user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()),
                    user.getLastLoginAt(),
                    user.getCreatedAt());
        }
    }

    public record RoleResponse(UUID id, String name, String description) {
        public static RoleResponse from(Role role) {
            return new RoleResponse(role.getId(), role.getName(), role.getDescription());
        }
    }
}
