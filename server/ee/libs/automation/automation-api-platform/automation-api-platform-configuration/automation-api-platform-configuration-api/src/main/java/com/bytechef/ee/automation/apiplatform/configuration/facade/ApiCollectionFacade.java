/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionFacade {

    ApiCollectionDTO createApiCollection(ApiCollectionDTO apiCollectionDTO);

    ApiCollectionEndpointDTO createApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpoint);

    void deleteApiCollection(long id);

    void deleteApiCollectionEndpoint(long id);

    ApiCollectionEndpointDTO enableApiCollectionEndpoint(long id, boolean enable);

    ApiCollectionDTO getApiCollection(long id);

    ApiCollectionEndpointDTO getApiCollectionEndpoint(long id);

    List<ApiCollectionEndpointDTO> getApiCollectionEndpoints(long apiCollectionId);

    List<ApiCollectionDTO> getApiCollections(long workspaceId, Long environmentId, Long projectId, Long tagId);

    List<Tag> getApiCollectionTags(long workspaceId);

    String getOpenApiSpecification(long id);

    List<Project> getWorkspaceProjects(long workspaceId);

    ApiCollectionDTO updateApiCollection(ApiCollectionDTO apiCollectionDTO);

    /**
     * Moves the collection's backing deployment to a newer published project version in place, keeping its
     * {@code collectionVersion}. Endpoints whose workflow the new version lacks are refused unless
     * {@code removeMissingEndpoints} is set, in which case they are deleted.
     */
    ApiCollectionVersionUpdateDTO updateApiCollectionProjectVersion(
        long id, int projectVersion, boolean removeMissingEndpoints);

    void updateApiCollectionTags(long id, List<Tag> tags);

    ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO);

    ApiCollectionEndpointDTO updateApiCollectionEndpointWorkflow(
        long id, List<ProjectDeploymentWorkflowConnection> connections, Map<String, ?> inputs);

}
