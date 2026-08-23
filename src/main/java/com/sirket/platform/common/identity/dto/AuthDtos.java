package com.sirket.platform.common.identity.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Kullanıcı adı veya e-posta zorunludur") String usernameOrEmail,
            @NotBlank(message = "Şifre zorunludur") String password) {
    }

    public record RefreshRequest(
            @NotBlank(message = "Refresh token zorunludur") String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn) {

        public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
            return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
        }
    }
}
