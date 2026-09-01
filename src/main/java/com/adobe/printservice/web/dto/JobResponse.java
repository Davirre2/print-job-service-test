package com.adobe.printservice.web.dto;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;

import java.time.Instant;
import java.util.Map;

/**
 * API representation of a job. Kept separate from the {@link Job} entity so the persistence
 * model can grow (retry scheduling, backoff bookkeeping, ...) without changing the public
 * contract, and so submit/get/list can all return the same shape.
 */
public record JobResponse(
        String id,
        String templateId,
        Map<String, Object> parameters,
        JobStatus status,
        int attempts,
        String errorMessage,
        boolean resultAvailable,
        Instant createdAt,
        Instant updatedAt
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTemplateId(),
                job.getParameters(),
                job.getStatus(),
                job.getAttempts(),
                job.getErrorMessage(),
                job.getResultContent() != null,
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
