/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionService {

    ApiCollection create(ApiCollection apiCollection);

    void delete(long id);

    ApiCollection getApiCollection(long id);

    ApiCollection getApiCollection(String contextPath);

    List<Long> getApiCollectionProjectIds(long workspaceId);

    List<ApiCollection> getApiCollections(Long workspaceId, Environment environment, Long projectId, Long tagId);

    ApiCollection update(ApiCollection apiCollection);

    ApiCollection update(long id, List<Long> tagIds);
}
