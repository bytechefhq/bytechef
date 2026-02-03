/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob.Status;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Implementation of ApiConnectorGenerationJobService using in-memory storage.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ApiConnectorGenerationJobServiceImpl implements ApiConnectorGenerationJobService {

    private static final int JOB_RETENTION_MINUTES = 5;

    private final Map<String, ApiConnectorGenerationJob> jobs = new ConcurrentHashMap<>();

    @Override
    public ApiConnectorGenerationJob create(String documentationUrl) {
        String jobId = UUID.randomUUID()
            .toString();

        ApiConnectorGenerationJob job = new ApiConnectorGenerationJob(jobId, documentationUrl);

        jobs.put(jobId, job);

        return job;
    }

    @Override
    public Optional<ApiConnectorGenerationJob> get(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public void markAsProcessing(String jobId) {
        ApiConnectorGenerationJob job = jobs.get(jobId);

        if (job != null) {
            job.setStatus(Status.PROCESSING);
        }
    }

    @Override
    public void markAsCompleted(String jobId, String specification) {
        ApiConnectorGenerationJob job = jobs.get(jobId);

        if (job != null) {
            job.setStatus(Status.COMPLETED);
            job.setSpecification(specification);
        }
    }

    @Override
    public void markAsFailed(String jobId, String errorMessage) {
        ApiConnectorGenerationJob job = jobs.get(jobId);

        if (job != null) {
            job.setStatus(Status.FAILED);
            job.setErrorMessage(errorMessage);
        }
    }

    @Override
    public boolean requestCancellation(String jobId) {
        ApiConnectorGenerationJob job = jobs.get(jobId);

        if (job != null) {
            job.setCancellationRequested(true);
            job.setStatus(Status.CANCELLED);

            return true;
        }

        return false;
    }

    @Override
    public boolean isCancellationRequested(String jobId) {
        ApiConnectorGenerationJob job = jobs.get(jobId);

        return job != null && job.isCancellationRequested();
    }

    @Scheduled(fixedRate = 60000)
    void cleanupOldJobs() {
        LocalDateTime cutoffTime = LocalDateTime.now()
            .minusMinutes(JOB_RETENTION_MINUTES);

        jobs.entrySet()
            .removeIf(entry -> {
                ApiConnectorGenerationJob job = entry.getValue();
                LocalDateTime completedDate = job.getCompletedDate();

                return completedDate != null && completedDate.isBefore(cutoffTime);
            });
    }
}
