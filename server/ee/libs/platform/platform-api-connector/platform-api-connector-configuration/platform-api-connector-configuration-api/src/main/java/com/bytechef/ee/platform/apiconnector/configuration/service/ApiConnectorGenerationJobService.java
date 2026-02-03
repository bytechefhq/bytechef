/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import java.util.Optional;

/**
 * Service for managing API connector generation jobs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorGenerationJobService {

    /**
     * Creates a new generation job.
     *
     * @param documentationUrl the URL to the API documentation
     * @return the created job with a unique ID
     */
    ApiConnectorGenerationJob create(String documentationUrl);

    /**
     * Gets a job by ID.
     *
     * @param jobId the job ID
     * @return the job if found
     */
    Optional<ApiConnectorGenerationJob> get(String jobId);

    /**
     * Updates the job status to processing.
     *
     * @param jobId the job ID
     */
    void markAsProcessing(String jobId);

    /**
     * Marks the job as completed with the generated specification.
     *
     * @param jobId         the job ID
     * @param specification the generated OpenAPI specification
     */
    void markAsCompleted(String jobId, String specification);

    /**
     * Marks the job as failed with an error message.
     *
     * @param jobId        the job ID
     * @param errorMessage the error message
     */
    void markAsFailed(String jobId, String errorMessage);

    /**
     * Requests cancellation of the job.
     *
     * @param jobId the job ID
     * @return true if the job was found and cancellation was requested
     */
    boolean requestCancellation(String jobId);

    /**
     * Checks if cancellation was requested for the job.
     *
     * @param jobId the job ID
     * @return true if cancellation was requested
     */
    boolean isCancellationRequested(String jobId);
}
