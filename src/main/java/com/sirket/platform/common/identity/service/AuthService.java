package com.sirket.platform.common.identity.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.identity.domain.RefreshToken;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.domain.UserStatus;
import com.sirket.platform.common.identity.dto.AuthDtos;
import com.sirket.platform.common.identity.repository.RefreshTokenRepository;
import com.sirket.platform.common.identity.repository.UserRepository;
import com.sirket.platform.common.security.SecurityProperties;
import com.sirket.platform.common.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Kullanıcı adı veya şifre hatalı";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            SecurityProperties properties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsername(request.usernameOrEmail())
                .or(() -> userRepository.findByEmail(request.usernameOrEmail()))
                .orElseThrow(() -> new ApiExceptions.Unauthorized(INVALID_CREDENTIALS));

        if (user.isLocked()) {
            throw new ApiExceptions.Unauthorized("Hesap geçici olarak kilitlendi, lütfen daha sonra tekrar deneyin");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiExceptions.Unauthorized("Hesap aktif değil");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin(properties.getLockout().getMaxFailedAttempts(),
                    properties.getLockout().getDuration());
            userRepository.save(user);
            throw new ApiExceptions.Unauthorized(INVALID_CREDENTIALS);
        }

        user.registerSuccessfulLogin();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new ApiExceptions.Unauthorized("Refresh token geçersiz"));

        if (!stored.isUsable()) {
            // A replayed token means the value leaked; drop every live session for that user.
            revokeAllFor(stored.getUser());
            throw new ApiExceptions.Unauthorized("Refresh token geçersiz");
        }

        User user = stored.getUser();
        if (user.isLocked() || user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiExceptions.Unauthorized("Hesap aktif değil");
        }

        AuthDtos.TokenResponse response = issueTokens(user);
        RefreshToken replacement = refreshTokenRepository.findByTokenHash(hash(response.refreshToken()))
                .orElseThrow(() -> new IllegalStateException("Newly issued refresh token was not persisted"));
        stored.replaceWith(replacement);
        refreshTokenRepository.save(stored);
        return response;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    private AuthDtos.TokenResponse issueTokens(User user) {
        String accessToken = tokenService.createAccessToken(user);
        String rawRefreshToken = generateRefreshTokenValue();
        Instant expiresAt = Instant.now().plus(properties.getJwt().getRefreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), expiresAt));
        return AuthDtos.TokenResponse.bearer(accessToken, rawRefreshToken, tokenService.accessTokenTtlSeconds());
    }

    private void revokeAllFor(User user) {
        refreshTokenRepository.findByUserAndRevokedAtIsNull(user).forEach(RefreshToken::revoke);
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
