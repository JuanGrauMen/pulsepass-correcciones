package com.pulsepass.service;

import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserRole;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserRepository userRepository;
    @Mock UserRoleRepository userRoleRepository;

    @InjectMocks AuthService authService;

    @Test
    void currentUser_returnsIdentityAndRoles() {
        User user = new User();
        user.setId(7L);
        user.setUsername("andrea");
        user.setEmail("andrea@email.com");
        user.setActive(true);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRoleRepository.findAllByUserId(7L))
                .thenReturn(List.of(new UserRole(7L, "USER"), new UserRole(7L, "ADMIN")));

        AuthenticatedUserResponse response = authService.currentUser(7L);

        assertThat(response.username()).isEqualTo("andrea");
        assertThat(response.email()).isEqualTo("andrea@email.com");
        assertThat(response.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void currentUser_ofAMissingUser_isRejectedAsUnauthenticated() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
