package com.sirket.platform.common.identity.controller;

import com.sirket.platform.common.identity.domain.UserStatus;
import com.sirket.platform.common.identity.dto.UserDtos;
import com.sirket.platform.common.identity.repository.RoleRepository;
import com.sirket.platform.common.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Kullanıcı Yönetimi")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/users")
    @Operation(summary = "Kullanıcıları listeler")
    public Page<UserDtos.UserResponse> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return userService.search(status, role, pageable);
    }

    @PostMapping("/users")
    @Operation(summary = "Yeni kullanıcı oluşturur ve rol atar")
    public ResponseEntity<UserDtos.UserResponse> create(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        UserDtos.UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Kullanıcı detayını getirir")
    public UserDtos.UserResponse get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Kullanıcı bilgilerini günceller")
    public UserDtos.UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Kullanıcıyı aktif/pasif/kilitli yapar")
    public UserDtos.UserResponse changeStatus(
            @PathVariable UUID id, @Valid @RequestBody UserDtos.UpdateStatusRequest request) {
        return userService.changeStatus(id, request.status());
    }

    @PostMapping("/users/{id}/roles")
    @Operation(summary = "Kullanıcının rollerini günceller")
    public UserDtos.UserResponse assignRoles(
            @PathVariable UUID id, @Valid @RequestBody UserDtos.AssignRolesRequest request) {
        return userService.assignRoles(id, request.roles());
    }

    @GetMapping("/roles")
    @Operation(summary = "Tanımlı rolleri listeler")
    public List<UserDtos.RoleResponse> roles() {
        return roleRepository.findAll().stream().map(UserDtos.RoleResponse::from).toList();
    }
}
