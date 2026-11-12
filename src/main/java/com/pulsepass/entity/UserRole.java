package com.pulsepass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRole.UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    protected UserRole() {
    }

    public UserRole(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public static class UserRoleId implements Serializable {

        private Long userId;
        private String role;

        public UserRoleId() {
        }

        public UserRoleId(Long userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof UserRoleId that)) return false;
            return Objects.equals(userId, that.userId)
                    && Objects.equals(role, that.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, role);
        }
    }
}
