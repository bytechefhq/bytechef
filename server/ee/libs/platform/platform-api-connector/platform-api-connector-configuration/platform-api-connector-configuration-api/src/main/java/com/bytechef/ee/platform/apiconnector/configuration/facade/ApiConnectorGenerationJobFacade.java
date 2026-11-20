/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import org.jspecify.annotations.Nullable;

/**
 * Runs an API connector generation job: generates an OpenAPI specification from API documentation with AI and records
 * the progress of the job.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorGenerationJobFacade {

    /**
     * Asynchronously generates an OpenAPI specification from API documentation URL.
     *
     * @param jobId            the job ID to track progress
     * @param documentationUrl the URL to the API documentation
     * @param userInstructions optional user instructions for endpoint selection
     */
    void generateOpenApiSpecificationAsync(
        String jobId, String documentationUrl, @Nullable String userInstructions);
}
