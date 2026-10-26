/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ApiCollectionService {

    ApiCollection create(ApiCollection apiCollection);

    void delete(long id);

    boolean existsByNameAndEnvironment(
        String name, long workspaceId, Environment environment, @Nullable Long excludeId);

    /**
     * Retrieves an API collection by id without throwing when it does not exist.
     *
     * @param id the API collection id
     * @return the API collection, if one exists
     */
    Optional<ApiCollection> fetchApiCollection(long id);

    /**
     * Retrieves an API collection by its cross-environment lineage identifier and environment.
     *
     * @param uuid        the cross-environment lineage identifier
     * @param environment the environment to look the collection up in
     * @return the API collection matching the given uuid and environment, if one exists
     */
    Optional<ApiCollection> fetchApiCollection(UUID uuid, Environment environment);

    ApiCollection getApiCollection(long id);

    ApiCollection getApiCollection(String contextPath);

    List<Long> getApiCollectionProjectIds(long workspaceId);

    List<ApiCollection> getApiCollections(Long workspaceId, Environment environment, Long projectId, Long tagId);

    ApiCollection update(ApiCollection apiCollection);

    ApiCollection update(long id, List<Long> tagIds);
}
