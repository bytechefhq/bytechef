/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.platform.constant.Environment;
import java.util.List;
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

        return apiCollectionRepository.save(apiCollection);
    }

    @Override
    public void delete(long id) {
        apiCollectionRepository.deleteById(id);
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
