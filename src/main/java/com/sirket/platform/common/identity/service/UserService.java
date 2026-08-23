package com.sirket.platform.common.identity.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.identity.domain.Role;
import com.sirket.platform.common.identity.domain.User;
import com.sirket.platform.common.identity.domain.UserStatus;
import com.sirket.platform.common.identity.dto.UserDtos;
import com.sirket.platform.common.identity.repository.RoleRepository;
import com.sirket.platform.common.identity.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserDtos.UserResponse> search(UserStatus status, String role, Pageable pageable) {
        return userRepository.search(status, role, pageable).map(UserDtos.UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse get(UUID id) {
        return UserDtos.UserResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiExceptions.NotFound("Kullanıcı bulunamadı: " + username));
    }

    @Transactional
    public UserDtos.UserResponse create(UserDtos.CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiExceptions.Conflict("Bu kullanıcı adı zaten kullanımda");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiExceptions.Conflict("Bu e-posta adresi zaten kullanımda");
        }
        User user = new User(request.username(), request.email(), passwordEncoder.encode(request.password()));
        user.replaceRoles(resolveRoles(request.roles()));
        return UserDtos.UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse update(UUID id, UserDtos.UpdateUserRequest request) {
        User user = require(id);
        userRepository.findByUsername(request.username())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiExceptions.Conflict("Bu kullanıcı adı zaten kullanımda");
                });
        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiExceptions.Conflict("Bu e-posta adresi zaten kullanımda");
                });
        user.updateProfile(request.username(), request.email());
        return UserDtos.UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse changeStatus(UUID id, UserStatus status) {
        User user = require(id);
        user.changeStatus(status);
        return UserDtos.UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse assignRoles(UUID id, Set<String> roleNames) {
        User user = require(id);
        user.replaceRoles(resolveRoles(roleNames));
        return UserDtos.UserResponse.from(userRepository.save(user));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new ApiExceptions.NotFound("Tanımsız rol adı gönderildi");
        }
        return roles;
    }

    private User require(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Kullanıcı bulunamadı: " + id));
    }
}
