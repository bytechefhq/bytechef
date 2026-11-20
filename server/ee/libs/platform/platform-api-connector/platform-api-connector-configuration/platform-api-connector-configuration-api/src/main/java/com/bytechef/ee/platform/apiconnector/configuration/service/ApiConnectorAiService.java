/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import org.jspecify.annotations.Nullable;

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
     * Fetches the API documentation page and returns its readable text content.
     *
     * @param documentationUrl the URL to the API documentation
     * @return the text content of the documentation page
     */
    String fetchDocumentation(String documentationUrl);

    /**
     * Generates an OpenAPI specification from already fetched API documentation content.
     *
     * @param documentationContent the text content of the API documentation
     * @param userInstructions     optional user instructions for endpoint selection
     * @return the generated OpenAPI specification in YAML format
     */
    String generateOpenApiSpecificationFromContent(String documentationContent, @Nullable String userInstructions);
}
