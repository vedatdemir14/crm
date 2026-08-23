package com.sirket.platform.common.identity.controller;

import com.sirket.platform.common.identity.dto.UserDtos;
import com.sirket.platform.common.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Self-Service")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Giriş yapmış kullanıcının profil bilgisi")
    public UserDtos.UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.get(UUID.fromString(jwt.getSubject()));
    }
}
