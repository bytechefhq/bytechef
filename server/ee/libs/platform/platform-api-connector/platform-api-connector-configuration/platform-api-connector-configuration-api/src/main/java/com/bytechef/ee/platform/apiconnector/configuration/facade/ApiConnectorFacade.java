/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.facade;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiConnectorFacade {

    void deleteApiConnector(long id);

    /**
     * Deletes stored specification/definition blobs that no connector row references (left behind by re-imports that
     * predate replaced-blob cleanup). Deliberately manual: an import in flight stores its blobs before its row commits,
     * so an automatic sweep could race it.
     *
     * @return the number of orphaned files that were deleted
     */
    int deleteOrphanedFiles();

    ApiConnector generateFromDocumentation(String componentName, String documentationUrl, String icon);

    ApiConnectorDTO getApiConnector(long id);

    List<ApiConnectorDTO> getApiConnectors();

    ApiConnector importOpenApiSpecification(String componentName, String icon, String specification);

    /**
     * Updates an existing connector by id from a new specification, regenerating its definition. Unlike
     * {@link #importOpenApiSpecification}, which upserts by name, this is rename-safe: a name change is rejected when
     * another connector already holds the name. A non-null {@code version} must match the stored row's version,
     * rejecting concurrent edits.
     */
    ApiConnector updateApiConnector(long id, String componentName, String icon, String specification, Integer version);
}
