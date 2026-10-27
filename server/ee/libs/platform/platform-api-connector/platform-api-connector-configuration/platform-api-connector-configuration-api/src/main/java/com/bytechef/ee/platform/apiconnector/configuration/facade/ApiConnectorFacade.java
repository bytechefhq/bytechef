/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import com.bytechef.ee.platform.apiconnector.configuration.dto.GenerateSpecificationRequestDTO;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorFacade {

    boolean cancelGenerationJob(String jobId);

    void deleteApiConnector(long id);

    /**
     * Deletes stored specification and definition files that no API connector references.
     *
     * @return the number of deleted files
     */
    int deleteOrphanedFiles();

    void enableApiConnector(long id, boolean enable);

    Optional<ApiConnectorGenerationJob> fetchGenerationJob(String jobId);

    ApiConnector generateFromDocumentation(String componentName, String documentationUrl, String icon);

    String generateSpecification(GenerateSpecificationRequestDTO generateSpecificationRequestDTO);

    ApiConnectorDTO getApiConnector(long id);

    List<ApiConnectorDTO> getApiConnectors();

    ApiConnector importOpenApiSpecification(String componentName, String icon, String specification);

    /**
     * Starts an asynchronous OpenAPI specification generation from API documentation and returns the tracking job. A
     * null or non-positive {@code maxPages} scrapes a single page.
     */
    ApiConnectorGenerationJob startGenerateFromDocumentationPreview(
        String documentationUrl, String userPrompt, Integer maxPages);

    /**
     * Updates an existing API connector from a new specification and regenerates its definition. A non-null
     * {@code version} must match the stored version.
     */
    ApiConnector updateApiConnector(long id, String componentName, String icon, String specification, Integer version);

    /**
     * Updates the metadata of an existing API connector without touching its specification or definition. Null
     * arguments leave the corresponding field unchanged.
     */
    ApiConnector updateApiConnectorMetadata(
        long id, String name, String title, String description, String icon, Integer connectorVersion);
}
