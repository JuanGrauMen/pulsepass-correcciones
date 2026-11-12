package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserCredential;
import com.pulsepass.entity.UserProfile;
import com.pulsepass.entity.UserRole;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserProfileRepository userProfileRepository;

    @Mock
    UserCredentialRepository userCredentialRepository;

    @Mock
    UserRoleRepository userRoleRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void register_shouldCreateUserProfileCredentialsAndUserRole() {
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea",
                "andrea@email.com",
                "Andrea",
                "Diaz",
                "300",
                "Santa Marta",
                LocalDate.now().minusYears(25),
                "Password1234"
        );

        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        when(passwordEncoder.encode("Password1234"))
                .thenReturn("bcrypt-hash");

        when(userMapper.toResponse(any(User.class)))
                .thenReturn(mock(UserResponse.class));

        userService.register(request);

        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(passwordEncoder).encode("Password1234");
        verify(userCredentialRepository).save(any(UserCredential.class));

        verify(userRoleRepository).save(argThat(role ->
                role.getUserId().equals(1L)
                        && role.getRole().equals("USER")));
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        RegisterUserRequest request = new RegisterUserRequest(
                "andrea",
                "a@x.com",
                "Andrea",
                "Diaz",
                null,
                null,
                LocalDate.now().minusYears(25),
                "Password1234"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(userCredentialRepository, never())
                .save(any(UserCredential.class));
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void register_shouldRejectFutureBirthDate() {
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("a@x.com"))
                .thenReturn(false);

        RegisterUserRequest request = new RegisterUserRequest(
                "andrea",
                "a@x.com",
                "Andrea",
                "Diaz",
                null,
                null,
                LocalDate.now().plusDays(1),
                "Password1234"
        );

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldRejectDuplicateEmailIgnoringCase() {
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("ANDREA@EMAIL.COM")).thenReturn(true);

        RegisterUserRequest request = new RegisterUserRequest(
                "andrea", "ANDREA@EMAIL.COM", "Andrea", "Diaz",
                null, null, LocalDate.now().minusYears(25), "Password1234");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

}
