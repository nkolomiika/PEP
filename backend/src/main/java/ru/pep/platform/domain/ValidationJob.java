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
@Table(name = "validation_job")
public class ValidationJob {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "image_reference", nullable = false, columnDefinition = "TEXT")
    private String imageReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ValidationJobStatus status;

    @Column(name = "logs_uri", columnDefinition = "TEXT")
    private String logsUri;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    protected ValidationJob() {
    }

    public ValidationJob(Submission submission) {
        this.submission = submission;
        this.imageReference = submission.getImageReference();
        this.status = ValidationJobStatus.QUEUED;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
    }

    public void pass(String logsUri) {
        this.status = ValidationJobStatus.PASSED;
        this.logsUri = logsUri;
        this.finishedAt = OffsetDateTime.now();
        submission.markReadyForReview();
    }

    public void fail(String errorMessage) {
        this.status = ValidationJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = OffsetDateTime.now();
        submission.markTechnicalValidationFailed();
    }

    public UUID getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public String getImageReference() {
        return imageReference;
    }

    public ValidationJobStatus getStatus() {
        return status;
    }

    public String getLogsUri() {
        return logsUri;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
