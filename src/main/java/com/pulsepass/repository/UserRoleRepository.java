package com.pulsepass.repository;

import com.pulsepass.entity.UserRole;
import com.pulsepass.entity.UserRole.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByUserId(Long userId);

    boolean existsByUserIdAndRole(Long userId, String role);
}
