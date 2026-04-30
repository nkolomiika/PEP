package ru.pep.platform.domain;

public enum LabStatus {
    PENDING,
    DEPLOYING,
    RUNNING,
    UNHEALTHY,
    FAILED,
    STOPPING,
    STOPPED,
    ARCHIVED
}
