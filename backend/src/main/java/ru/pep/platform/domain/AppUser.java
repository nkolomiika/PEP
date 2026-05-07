package ru.pep.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "avatar_storage_key", length = 120)
    private String avatarStorageKey;

    protected AppUser() {
    }

    public AppUser(String email, String passwordHash, String displayName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(String displayName, Role role, UserStatus status) {
        this.displayName = displayName;
        this.role = role;
        this.status = status;
    }

    public void updateAdminProfile(String email, String displayName, Role role, UserStatus status) {
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    public String getAvatarStorageKey() {
        return avatarStorageKey;
    }

    public void setAvatarStorageKey(String avatarStorageKey) {
        this.avatarStorageKey = avatarStorageKey;
    }

    public void applyDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void applyEmail(String email) {
        this.email = email;
    }

    public void applyPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
