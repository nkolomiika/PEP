package ru.pep.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "student_stream_course",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_stream_course",
                columnNames = {"stream_id", "course_id"}))
public class StudentStreamCourse {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stream_id", nullable = false)
    private StudentStream stream;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StudentStreamCourse() {
    }

    public StudentStreamCourse(StudentStream stream, Course course, int position) {
        this.stream = stream;
        this.course = course;
        this.position = position;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (position == null) {
            position = 0;
        }
        createdAt = OffsetDateTime.now();
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public StudentStream getStream() {
        return stream;
    }

    public Course getCourse() {
        return course;
    }

    public Integer getPosition() {
        return position;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
