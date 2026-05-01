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

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private SubmissionSourceType sourceType;

    @Column(name = "archive_filename")
    private String archiveFilename;

    @Column(name = "archive_storage_path", columnDefinition = "TEXT")
    private String archiveStoragePath;

    @Column(name = "compose_service", length = 120)
    private String composeService;

    @Column(name = "build_context")
    private String buildContext;

    @Column(name = "runtime_image_reference", columnDefinition = "TEXT")
    private String runtimeImageReference;

    @Column(name = "public_url", columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "local_host_url", columnDefinition = "TEXT")
    private String localHostUrl;

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
        this.sourceType = SubmissionSourceType.IMAGE_REFERENCE;
        this.runtimeImageReference = imageReference;
        this.applicationPort = applicationPort;
        this.healthPath = healthPath;
        this.status = SubmissionStatus.VALIDATION_QUEUED;
        this.submittedAt = OffsetDateTime.now();
    }

    public static Submission archive(
            LearningModule module,
            AppUser student,
            String archiveFilename,
            String archiveStoragePath,
            String composeService,
            Integer applicationPort,
            String healthPath) {
        Submission submission = new Submission();
        submission.module = module;
        submission.student = student;
        submission.imageReference = "archive://" + archiveFilename;
        submission.sourceType = SubmissionSourceType.ARCHIVE;
        submission.archiveFilename = archiveFilename;
        submission.archiveStoragePath = archiveStoragePath;
        submission.composeService = composeService;
        submission.buildContext = ".";
        submission.applicationPort = applicationPort;
        submission.healthPath = healthPath;
        submission.status = SubmissionStatus.VALIDATION_QUEUED;
        submission.submittedAt = OffsetDateTime.now();
        return submission;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (sourceType == null) {
            sourceType = SubmissionSourceType.IMAGE_REFERENCE;
        }
        if (runtimeImageReference == null) {
            runtimeImageReference = imageReference;
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

    public SubmissionSourceType getSourceType() {
        return sourceType == null ? SubmissionSourceType.IMAGE_REFERENCE : sourceType;
    }

    public String getArchiveFilename() {
        return archiveFilename;
    }

    public String getArchiveStoragePath() {
        return archiveStoragePath;
    }

    public String getComposeService() {
        return composeService;
    }

    public String getBuildContext() {
        return buildContext;
    }

    public String getRuntimeImageReference() {
        return runtimeImageReference == null ? imageReference : runtimeImageReference;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public String getLocalHostUrl() {
        return localHostUrl;
    }

    public void markArchiveBuilt(String runtimeImageReference) {
        this.runtimeImageReference = runtimeImageReference;
        this.imageReference = runtimeImageReference;
    }

    public void setLabUrls(String publicUrl, String localHostUrl) {
        this.publicUrl = publicUrl;
        this.localHostUrl = localHostUrl;
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
