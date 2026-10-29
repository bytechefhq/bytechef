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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ApiConnectorGenerationJobServiceImpl implements ApiConnectorGenerationJobService {

    public static final String JOBS_CACHE = ApiConnectorGenerationJobService.class.getName() + ".jobs";

    private static final String CANCELLATION_REQUESTED = "cancellationRequested";
    private static final String COMPLETED_DATE = "completedDate";
    private static final String CREATED_DATE = "createdDate";
    private static final String DOCUMENTATION_URL = "documentationUrl";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ID = "id";
    private static final String SPECIFICATION = "specification";
    private static final String STATUS = "status";

    private final CacheManager cacheManager;

    @SuppressFBWarnings("EI")
    public ApiConnectorGenerationJobServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public ApiConnectorGenerationJob create(String documentationUrl) {
        String jobId = UUID.randomUUID()
            .toString();

        ApiConnectorGenerationJob job = new ApiConnectorGenerationJob(jobId, documentationUrl);

        put(job);

        return job;
    }

    @Override
    public Optional<ApiConnectorGenerationJob> get(String jobId) {
        Cache.ValueWrapper valueWrapper = getCache().get(jobId);

        if (valueWrapper == null || !(valueWrapper.get() instanceof Map<?, ?> jobMap)) {
            return Optional.empty();
        }

        return Optional.of(toJob(jobMap));
    }

    @Override
    public void markAsProcessing(String jobId) {
        update(jobId, job -> job.setStatus(Status.PROCESSING));
    }

    @Override
    public void markAsCompleted(String jobId, String specification) {
        update(jobId, job -> {
            job.setStatus(Status.COMPLETED);
            job.setSpecification(specification);
        });
    }

    @Override
    public void markAsFailed(String jobId, String errorMessage) {
        update(jobId, job -> {
            job.setStatus(Status.FAILED);
            job.setErrorMessage(errorMessage);
        });
    }

    @Override
    public boolean requestCancellation(String jobId) {
        Optional<ApiConnectorGenerationJob> jobOptional = get(jobId);

        if (jobOptional.isEmpty()) {
            return false;
        }

        ApiConnectorGenerationJob job = jobOptional.get();

        job.setCancellationRequested(true);
        job.setStatus(Status.CANCELLED);

        put(job);

        return true;
    }

    @Override
    public boolean isCancellationRequested(String jobId) {
        return get(jobId)
            .map(ApiConnectorGenerationJob::isCancellationRequested)
            .orElse(false);
    }

    private Cache getCache() {
        return Objects.requireNonNull(cacheManager.getCache(JOBS_CACHE));
    }

    private void put(ApiConnectorGenerationJob job) {
        Cache cache = getCache();

        cache.put(job.getId(), toMap(job));
    }

    private void update(String jobId, Consumer<ApiConnectorGenerationJob> jobConsumer) {
        get(jobId).ifPresent(job -> {
            if (job.isCancellationRequested()) {
                return;
            }

            jobConsumer.accept(job);

            put(job);
        });
    }

    private static Map<String, Object> toMap(ApiConnectorGenerationJob job) {
        Map<String, Object> jobMap = new HashMap<>();

        jobMap.put(CANCELLATION_REQUESTED, job.isCancellationRequested());
        jobMap.put(ID, job.getId());
        jobMap.put(STATUS, job.getStatus()
            .name());

        putIfNotNull(jobMap, COMPLETED_DATE, job.getCompletedDate());
        putIfNotNull(jobMap, CREATED_DATE, job.getCreatedDate());
        putIfNotNull(jobMap, DOCUMENTATION_URL, job.getDocumentationUrl());
        putIfNotNull(jobMap, ERROR_MESSAGE, job.getErrorMessage());
        putIfNotNull(jobMap, SPECIFICATION, job.getSpecification());

        return jobMap;
    }

    private static ApiConnectorGenerationJob toJob(Map<?, ?> jobMap) {
        ApiConnectorGenerationJob job = new ApiConnectorGenerationJob();

        job.setId((String) jobMap.get(ID));
        job.setStatus(Status.valueOf((String) jobMap.get(STATUS)));
        job.setCancellationRequested(Boolean.TRUE.equals(jobMap.get(CANCELLATION_REQUESTED)));
        job.setCompletedDate(toLocalDateTime(jobMap.get(COMPLETED_DATE)));
        job.setCreatedDate(toLocalDateTime(jobMap.get(CREATED_DATE)));
        job.setDocumentationUrl((String) jobMap.get(DOCUMENTATION_URL));
        job.setErrorMessage((String) jobMap.get(ERROR_MESSAGE));
        job.setSpecification((String) jobMap.get(SPECIFICATION));

        return job;
    }

    private static void putIfNotNull(Map<String, Object> jobMap, String key, Object value) {
        if (value != null) {
            jobMap.put(key, value.toString());
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }

        return LocalDateTime.parse((String) value);
    }
}
