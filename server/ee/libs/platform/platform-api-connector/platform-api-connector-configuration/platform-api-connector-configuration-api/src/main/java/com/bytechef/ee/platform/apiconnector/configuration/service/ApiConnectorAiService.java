/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

/**
 * Service for generating OpenAPI specifications from API documentation using AI.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorAiService {

    /**
     * Generates an OpenAPI specification from API documentation URL.
     *
     * @param documentationUrl the URL to the API documentation
     * @return the generated OpenAPI specification in YAML format
     */
    String generateOpenApiSpecification(String documentationUrl);

    /**
     * Asynchronously generates an OpenAPI specification from API documentation URL.
     *
     * @param jobId            the job ID to track progress
     * @param documentationUrl the URL to the API documentation
     * @param userPrompt       optional user instructions for endpoint selection
     */
    void generateOpenApiSpecificationAsync(String jobId, String documentationUrl, String userPrompt);
}
