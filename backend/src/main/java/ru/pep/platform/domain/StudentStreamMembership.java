package ru.pep.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "student_stream_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_stream_member",
                columnNames = {"stream_id", "user_id"}))
public class StudentStreamMembership {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stream_id", nullable = false)
    private StudentStream stream;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StudentStreamMembershipStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StudentStreamMembership() {
    }

    public StudentStreamMembership(StudentStream stream, AppUser user) {
        this.stream = stream;
        this.user = user;
        this.status = StudentStreamMembershipStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = StudentStreamMembershipStatus.ACTIVE;
        }
        enrolledAt = OffsetDateTime.now();
        updatedAt = enrolledAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.status = StudentStreamMembershipStatus.ACTIVE;
    }

    public void remove() {
        this.status = StudentStreamMembershipStatus.REMOVED;
    }

    public UUID getId() {
        return id;
    }

    public StudentStream getStream() {
        return stream;
    }

    public AppUser getUser() {
        return user;
    }

    public StudentStreamMembershipStatus getStatus() {
        return status;
    }

    public OffsetDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
