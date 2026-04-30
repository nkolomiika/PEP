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
@Table(name = "lab_instance")
public class LabInstance {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(nullable = false, length = 120)
    private String namespace;

    @Column(name = "deployment_name", nullable = false, length = 120)
    private String deploymentName;

    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    @Column(name = "route_url", columnDefinition = "TEXT")
    private String routeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LabStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LabInstance() {
    }

    public LabInstance(Submission submission, String namespace, String deploymentName, String serviceName, String routeUrl) {
        this.submission = submission;
        this.namespace = namespace;
        this.deploymentName = deploymentName;
        this.serviceName = serviceName;
        this.routeUrl = routeUrl;
        this.status = LabStatus.RUNNING;
        this.expiresAt = OffsetDateTime.now().plusDays(7);
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

    public Submission getSubmission() {
        return submission;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getRouteUrl() {
        return routeUrl;
    }

    public LabStatus getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
