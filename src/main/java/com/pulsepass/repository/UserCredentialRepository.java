package com.pulsepass.repository;

import com.pulsepass.entity.UserCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository
        extends JpaRepository<UserCredential, Long> {

    Optional<UserCredential> findByUserId(Long userId);
}
