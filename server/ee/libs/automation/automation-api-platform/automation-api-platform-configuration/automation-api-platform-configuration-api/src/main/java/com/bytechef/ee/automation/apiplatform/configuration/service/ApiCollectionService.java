/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionService {

    ApiCollection create(@NonNull ApiCollection apiCollection);

    void delete(long id);

    ApiCollection getApiCollection(long id);

    List<Long> getApiCollectionProjectIds();

    List<ApiCollection> getApiCollections(Long workspaceId, Environment environment, Long projectId, Long tagId);

    ApiCollection update(@NonNull ApiCollection apiCollection);

    ApiCollection update(long id, @NonNull List<Long> tagIds);
}
