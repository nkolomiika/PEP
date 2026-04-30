package ru.pep.platform.domain;

public enum ValidationJobStatus {
    QUEUED,
    PULLING_IMAGE,
    STARTING_CONTAINER,
    CHECKING_PORT,
    CHECKING_HEALTH,
    PASSED,
    FAILED
}
