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
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_lesson_progress_student_lesson", columnNames = {"student_id", "lesson_id"}))
public class LessonProgress {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LessonProgress() {
    }

    public LessonProgress(AppUser student, Lesson lesson) {
        this.student = student;
        this.lesson = lesson;
        this.completedAt = OffsetDateTime.now();
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

    public AppUser getStudent() {
        return student;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
