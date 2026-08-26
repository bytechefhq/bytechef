/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiCollectionServiceImpl implements ApiCollectionService {

    private final ApiCollectionRepository apiCollectionRepository;

    public ApiCollectionServiceImpl(ApiCollectionRepository apiCollectionRepository) {
        this.apiCollectionRepository = apiCollectionRepository;
    }

    @Override
    public ApiCollection create(ApiCollection apiCollection) {
        Assert.notNull(apiCollection, "'openApiCollection' must not be null");
        Assert.notNull(apiCollection.getCollectionVersion(), "'collectionVersion' must not be null");
        Assert.isTrue(apiCollection.getId() == null, "'id' must be null");
        Assert.notNull(apiCollection.getName(), "'name' must not be null");
        Assert.notNull(apiCollection.getProjectDeploymentId(), "'projectDeploymentId' must not be null");

        if (apiCollection.getUuid() == null) {
            apiCollection.setUuid(UUID.randomUUID());
        }

        return apiCollectionRepository.save(apiCollection);
    }

    @Override
    public void delete(long id) {
        apiCollectionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameAndEnvironment(
        String name, long workspaceId, Environment environment, @Nullable Long excludeId) {

        Assert.notNull(name, "'name' must not be null");

        return apiCollectionRepository.existsByNameAndWorkspaceIdAndEnvironment(
            name, workspaceId, environment.ordinal(), excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiCollection> fetchApiCollection(long id) {
        return apiCollectionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiCollection> fetchApiCollection(UUID uuid, Environment environment) {
        return apiCollectionRepository.findByUuidAndEnvironment(uuid, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public ApiCollection getApiCollection(long id) {
        return OptionalUtils.get(apiCollectionRepository.findById(id));
    }

    @Override
    public ApiCollection getApiCollection(String contextPath) {
        return OptionalUtils.get(apiCollectionRepository.findByContextPath(contextPath));
    }

    @Override
    public List<Long> getApiCollectionProjectIds(long workspaceId) {
        return apiCollectionRepository.findAllApiCollectionProjectIds(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiCollection> getApiCollections(
        Long workspaceId, Environment environment, Long projectId, Long tagId) {

        return apiCollectionRepository.findAllApiCollections(
            workspaceId, environment == null ? null : environment.ordinal(), projectId, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiCollection> getProjectDeploymentApiCollections(long projectDeploymentId) {
        return apiCollectionRepository.findAllByProjectDeploymentId(projectDeploymentId);
    }

    @Override
    public ApiCollection update(ApiCollection apiCollection) {
        Assert.notNull(apiCollection, "'openApiCollection' must not be null");
        Assert.notNull(apiCollection.getCollectionVersion(), "'collectionVersion' must not be null");
        Assert.notNull(apiCollection.getId(), "id");
        Assert.notNull(apiCollection.getName(), "name");

        ApiCollection curApiCollection = getApiCollection(apiCollection.getId());

        curApiCollection.setCollectionVersion(apiCollection.getCollectionVersion());
        curApiCollection.setContextPath(apiCollection.getContextPath());
        curApiCollection.setDescription(apiCollection.getDescription());
        curApiCollection.setName(apiCollection.getName());
        curApiCollection.setTagIds(apiCollection.getTagIds());

        return apiCollectionRepository.save(curApiCollection);
    }

    @Override
    public ApiCollection update(long id, List<Long> tagIds) {
        ApiCollection connection = getApiCollection(id);

        connection.setTagIds(tagIds);

        return apiCollectionRepository.save(connection);
    }
}
