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

    @Enumerated(EnumType.STRING)
    @Column(name = "image_scan_status", length = 32)
    private ImageScanStatus imageScanStatus;

    @Column(name = "image_scan_summary", columnDefinition = "TEXT")
    private String imageScanSummary;

    @Column(name = "image_scan_report", columnDefinition = "TEXT")
    private String imageScanReport;

    @Column(name = "image_scan_finished_at")
    private OffsetDateTime imageScanFinishedAt;

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

    public void markPullingImage() {
        this.status = ValidationJobStatus.PULLING_IMAGE;
        this.startedAt = OffsetDateTime.now();
    }

    public void markStartingContainer() {
        this.status = ValidationJobStatus.STARTING_CONTAINER;
    }

    public void markCheckingPort() {
        this.status = ValidationJobStatus.CHECKING_PORT;
    }

    public void markCheckingHealth() {
        this.status = ValidationJobStatus.CHECKING_HEALTH;
    }

    public void markImageScanPassed(String summary, String report) {
        this.imageScanStatus = ImageScanStatus.PASSED;
        this.imageScanSummary = summary;
        this.imageScanReport = report;
        this.imageScanFinishedAt = OffsetDateTime.now();
    }

    public void markImageScanWarnings(String summary, String report) {
        this.imageScanStatus = ImageScanStatus.WARNINGS;
        this.imageScanSummary = summary;
        this.imageScanReport = report;
        this.imageScanFinishedAt = OffsetDateTime.now();
    }

    public void markImageScanFailed(String summary, String report) {
        this.imageScanStatus = ImageScanStatus.FAILED;
        this.imageScanSummary = summary;
        this.imageScanReport = report;
        this.imageScanFinishedAt = OffsetDateTime.now();
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

    public ImageScanStatus getImageScanStatus() {
        return imageScanStatus;
    }

    public String getImageScanSummary() {
        return imageScanSummary;
    }

    public String getImageScanReport() {
        return imageScanReport;
    }
}
