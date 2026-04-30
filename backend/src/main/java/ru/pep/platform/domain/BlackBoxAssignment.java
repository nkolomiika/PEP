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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "black_box_assignment")
public class BlackBoxAssignment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private LearningModule module;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_lab_instance_id", nullable = false)
    private LabInstance targetLabInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BlackBoxAssignmentStatus status;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected BlackBoxAssignment() {
    }

    public BlackBoxAssignment(LearningModule module, AppUser student, LabInstance targetLabInstance) {
        this.module = module;
        this.student = student;
        this.targetLabInstance = targetLabInstance;
        this.status = BlackBoxAssignmentStatus.ASSIGNED;
        this.assignedAt = OffsetDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public void markSubmitted() {
        this.status = BlackBoxAssignmentStatus.SUBMITTED;
        this.completedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public LearningModule getModule() {
        return module;
    }

    public AppUser getStudent() {
        return student;
    }

    public LabInstance getTargetLabInstance() {
        return targetLabInstance;
    }

    public BlackBoxAssignmentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }
}
