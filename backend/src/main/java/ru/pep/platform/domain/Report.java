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
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "report")
public class Report {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private LearningModule module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportStatus status;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Report() {
    }

    public Report(AppUser author, LearningModule module, Submission submission, ReportType type, String title, String contentMarkdown) {
        this.author = author;
        this.module = module;
        this.submission = submission;
        this.type = type;
        this.title = title;
        this.contentMarkdown = contentMarkdown;
        this.status = ReportStatus.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void applyReviewDecision(ReviewDecision decision) {
        status = switch (decision) {
            case APPROVED -> ReportStatus.APPROVED;
            case NEEDS_REVISION -> ReportStatus.NEEDS_REVISION;
            case REJECTED -> ReportStatus.REJECTED;
        };
        if (submission != null && type == ReportType.WHITE_BOX) {
            if (decision == ReviewDecision.APPROVED) {
                submission.approve();
            } else if (decision == ReviewDecision.NEEDS_REVISION) {
                submission.requestRevision();
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public AppUser getAuthor() {
        return author;
    }

    public LearningModule getModule() {
        return module;
    }

    public Submission getSubmission() {
        return submission;
    }

    public ReportType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public ReportStatus getStatus() {
        return status;
    }
}
