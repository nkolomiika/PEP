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
@Table(name = "module")
public class LearningModule {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "vulnerability_topic", nullable = false, length = 120)
    private String vulnerabilityTopic;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "submission_deadline")
    private OffsetDateTime submissionDeadline;

    @Column(name = "black_box_starts_at")
    private OffsetDateTime blackBoxStartsAt;

    @Column(name = "black_box_deadline")
    private OffsetDateTime blackBoxDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModuleStatus status;

    protected LearningModule() {
    }

    public LearningModule(String title, String vulnerabilityTopic, ModuleStatus status) {
        this.title = title;
        this.vulnerabilityTopic = vulnerabilityTopic;
        this.status = status;
    }

    public LearningModule(Course course, String title, String vulnerabilityTopic, ModuleStatus status) {
        this(title, vulnerabilityTopic, status);
        this.course = course;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    void setCourse(Course course) {
        this.course = course;
    }

    public UUID getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public String getVulnerabilityTopic() {
        return vulnerabilityTopic;
    }

    public ModuleStatus getStatus() {
        return status;
    }
}
