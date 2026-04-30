package ru.pep.platform.domain;

public enum SubmissionStatus {
    DRAFT,
    SUBMITTED,
    BUILD_QUEUED,
    BUILDING,
    BUILD_FAILED,
    TECHNICAL_VALIDATION_FAILED,
    READY_FOR_REVIEW,
    APPROVED,
    REJECTED,
    NEEDS_REVISION,
    PUBLISHED_FOR_BLACK_BOX,
    ARCHIVED
}
