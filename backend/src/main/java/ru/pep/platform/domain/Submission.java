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
@Table(name = "submission")
public class Submission {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private LearningModule module;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(name = "image_reference", nullable = false, columnDefinition = "TEXT")
    private String imageReference;

    @Column(name = "application_port", nullable = false)
    private Integer applicationPort;

    @Column(name = "health_path")
    private String healthPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SubmissionStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    protected Submission() {
    }

    public Submission(LearningModule module, AppUser student, String imageReference, Integer applicationPort, String healthPath) {
        this.module = module;
        this.student = student;
        this.imageReference = imageReference;
        this.applicationPort = applicationPort;
        this.healthPath = healthPath;
        this.status = SubmissionStatus.VALIDATION_QUEUED;
        this.submittedAt = OffsetDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }

    public void markReadyForReview() {
        status = SubmissionStatus.READY_FOR_REVIEW;
    }

    public void markTechnicalValidationFailed() {
        status = SubmissionStatus.TECHNICAL_VALIDATION_FAILED;
    }

    public void approve() {
        status = SubmissionStatus.APPROVED;
        approvedAt = OffsetDateTime.now();
    }

    public void requestRevision() {
        status = SubmissionStatus.NEEDS_REVISION;
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

    public String getImageReference() {
        return imageReference;
    }

    public Integer getApplicationPort() {
        return applicationPort;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public SubmissionStatus getStatus() {
        return status;
    }
}
