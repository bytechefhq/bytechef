/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionFacade {

    ApiCollectionDTO createApiCollection(@NonNull ApiCollectionDTO apiCollectionDTO);

    ApiCollectionEndpointDTO createApiCollectionEndpoint(@NonNull ApiCollectionEndpointDTO apiCollectionEndpoint);

    void deleteApiCollection(long id);

    ApiCollectionDTO getApiCollection(long id);

    List<ApiCollectionDTO> getApiCollections(long workspaceId, Environment environment, Long projectId, Long tagId);

    List<Tag> getApiCollectionTags();

    ApiCollectionDTO updateApiCollection(@NonNull ApiCollectionDTO apiCollectionDTO);

    void updateApiCollectionTags(long id, List<Tag> tags);

    ApiCollectionEndpointDTO updateApiCollectionEndpoint(ApiCollectionEndpointDTO apiCollectionEndpointDTO);
}
