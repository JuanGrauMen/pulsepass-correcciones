package com.pulsepass.service;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserRole;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.identifier(),
                                request.password()));

        String userId = authentication.getName();
        String token = jwtService.generateToken(userId);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getAccessTokenTtl().toSeconds());
    }

    /**
     * Identidad y roles vigentes del usuario autenticado. Un identificador
     * que ya no existe se trata como no autenticado (401).
     */
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        List<String> roles = userRoleRepository.findAllByUserId(userId)
                .stream()
                .map(UserRole::getRole)
                .toList();

        return new AuthenticatedUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles);
    }
}
