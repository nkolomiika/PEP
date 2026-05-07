package ru.pep.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "student_stream_module_schedule",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_stream_module_schedule",
                columnNames = {"stream_id", "module_id"}))
public class StudentStreamModuleSchedule {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stream_id", nullable = false)
    private StudentStream stream;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private LearningModule module;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "submission_deadline")
    private OffsetDateTime submissionDeadline;

    @Column(name = "black_box_starts_at")
    private OffsetDateTime blackBoxStartsAt;

    @Column(name = "black_box_deadline")
    private OffsetDateTime blackBoxDeadline;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StudentStreamModuleSchedule() {
    }

    public StudentStreamModuleSchedule(StudentStream stream, LearningModule module) {
        this.stream = stream;
        this.module = module;
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

    public void applySchedule(
            OffsetDateTime startsAt,
            OffsetDateTime submissionDeadline,
            OffsetDateTime blackBoxStartsAt,
            OffsetDateTime blackBoxDeadline) {
        this.startsAt = startsAt;
        this.submissionDeadline = submissionDeadline;
        this.blackBoxStartsAt = blackBoxStartsAt;
        this.blackBoxDeadline = blackBoxDeadline;
    }

    public UUID getId() {
        return id;
    }

    public StudentStream getStream() {
        return stream;
    }

    public LearningModule getModule() {
        return module;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getSubmissionDeadline() {
        return submissionDeadline;
    }

    public OffsetDateTime getBlackBoxStartsAt() {
        return blackBoxStartsAt;
    }

    public OffsetDateTime getBlackBoxDeadline() {
        return blackBoxDeadline;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
