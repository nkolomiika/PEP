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
@Table(name = "review")
public class Review {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curator_id", nullable = false)
    private AppUser curator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewDecision decision;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "comment_markdown", nullable = false, columnDefinition = "TEXT")
    private String commentMarkdown;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Review() {
    }

    public Review(Report report, AppUser curator, ReviewDecision decision, Integer score, String commentMarkdown) {
        this.report = report;
        this.curator = curator;
        this.decision = decision;
        this.score = score;
        this.commentMarkdown = commentMarkdown;
        report.applyReviewDecision(decision);
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

    public Report getReport() {
        return report;
    }

    public AppUser getCurator() {
        return curator;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public Integer getScore() {
        return score;
    }

    public String getCommentMarkdown() {
        return commentMarkdown;
    }
}
