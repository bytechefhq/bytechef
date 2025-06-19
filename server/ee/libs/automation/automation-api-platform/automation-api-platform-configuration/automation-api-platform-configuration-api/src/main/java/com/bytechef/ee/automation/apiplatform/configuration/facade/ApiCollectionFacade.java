/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionFacade {

    ApiCollectionDTO createApiCollection(ApiCollectionDTO apiCollectionDTO);

    ApiCollectionEndpointDTO createApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpoint);

    void deleteApiCollection(long id);

    ApiCollectionDTO getApiCollection(long id);

    List<ApiCollectionDTO> getApiCollections(long workspaceId, Environment environment, Long projectId, Long tagId);

    List<Tag> getApiCollectionTags();

    String getOpenApiSpecification(long id);

    List<Project> getWorkspaceProjects(long workspaceId);

    ApiCollectionDTO updateApiCollection(ApiCollectionDTO apiCollectionDTO);

    void updateApiCollectionTags(long id, List<Tag> tags);

    ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO);

}
