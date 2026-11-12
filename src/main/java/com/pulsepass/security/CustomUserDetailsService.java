package com.pulsepass.security;

import com.pulsepass.entity.User;
import com.pulsepass.entity.UserCredential;
import com.pulsepass.entity.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final UserRoleRepository roles;

    public CustomUserDetailsService(UserRepository users,
            UserCredentialRepository credentials, UserRoleRepository roles) {
        this.users = users;
        this.credentials = credentials;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        User user = (value.contains("@")
                ? users.findByEmailIgnoreCase(value)
                : users.findByUsername(value)).orElseThrow(this::invalidCredentials);
        return buildDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = users.findById(id).orElseThrow(this::invalidCredentials);
        return buildDetails(user);
    }

    private UserDetails buildDetails(User user) {
        if (!Boolean.TRUE.equals(user.getActive())) throw invalidCredentials();
        UserCredential credential = credentials.findByUserId(user.getId())
                .orElseThrow(this::invalidCredentials);
        List<SimpleGrantedAuthority> authorities = roles.findAllByUserId(user.getId())
                .stream().map(UserRole::getRole)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        if (authorities.isEmpty()) throw invalidCredentials();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getId().toString())
                .password(credential.getPasswordHash())
                .authorities(authorities).disabled(false).build();
    }

    private UsernameNotFoundException invalidCredentials() {
        return new UsernameNotFoundException("Invalid credentials");
    }
}
