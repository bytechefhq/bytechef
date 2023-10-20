/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.workflow.repository;

import com.integri.atlas.engine.cache.Clearable;
import com.integri.atlas.engine.workflow.Workflow;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Jun 2, 2017
 */
public class WorkflowRepositoryChain implements WorkflowRepository, Clearable {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRepositoryChain.class);

    private static final String CACHE_ALL = WorkflowRepositoryChain.class.getName() + ".all";
    private static final String CACHE_ONE = WorkflowRepositoryChain.class.getName() + ".one";

    private final CacheManager cacheManager;
    private final List<WorkflowRepository> workflowRepositories;

    public WorkflowRepositoryChain(CacheManager cacheManager, List<WorkflowRepository> workflowRepositories) {
        Assert.notNull(cacheManager, "'cacheManager' can not be null");
        Assert.notNull(workflowRepositories, "'workflowRepositories' can not be null");

        this.cacheManager = cacheManager;
        this.workflowRepositories = workflowRepositories;
    }

    @Override
    public Workflow create(String content, String format) {
        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                Workflow workflow = workflowRepository.create(content, format);

                Cache cache = cacheManager.getCache(CACHE_ONE);

                cache.put(workflow.getId(), workflow);

                return workflow;
            } catch (UnsupportedOperationException e) {
                logger.debug("Repository {} doesn't support create operation", workflowRepository);
            }
        }

        throw new RuntimeException(
            "Set atlas.workflow-repository.database.enabled=true property to create new workflow"
        );
    }

    @Override
    public Workflow update(String id, String content, String format) {
        for (WorkflowRepository repository : workflowRepositories) {
            try {
                Workflow workflow = repository.update(id, content, format);

                Cache cache = cacheManager.getCache(CACHE_ONE);

                cache.put(workflow.getId(), workflow);

                return workflow;
            } catch (UnsupportedOperationException e) {
                logger.debug("Repository {} doesn't support update operation", repository);
            }
        }
        throw new RuntimeException("Set atlas.workflow-repository.database.enabled=true property to update a workflow");
    }

    @Override
    public Workflow findOne(String aId) {
        Cache oneCache = cacheManager.getCache(CACHE_ONE);

        if (oneCache.get(aId) != null) {
            return (Workflow) oneCache.get(aId).get();
        }

        Cache allCache = cacheManager.getCache(CACHE_ALL);

        if (allCache.get(CACHE_ALL) != null) {
            List<Workflow> workflows = (List<Workflow>) allCache.get(CACHE_ALL).get();

            for (Workflow p : workflows) {
                if (p.getId().equals(aId)) {
                    return p;
                }
            }
        }

        for (WorkflowRepository repository : workflowRepositories) {
            try {
                Workflow workflow = repository.findOne(aId);

                oneCache.put(aId, workflow);

                return workflow;
            } catch (Exception e) {
                logger.debug("{}", e.getMessage());
            }
        }

        throw new IllegalArgumentException("Unknown workflow: " + aId);
    }

    @Override
    public List<Workflow> findAll() {
        Cache cache = cacheManager.getCache(CACHE_ALL);

        if (cache.get(CACHE_ALL) != null) {
            return (List<Workflow>) cache.get(CACHE_ALL).get();
        }

        List<Workflow> workflows = workflowRepositories
            .stream()
            .map(WorkflowRepository::findAll)
            .flatMap(List::stream)
            .sorted((a, b) -> {
                if (a.getLabel() == null || b.getLabel() == null) {
                    return -1;
                }
                return a.getLabel().compareTo(b.getLabel());
            })
            .collect(Collectors.toList());
        cache.put(CACHE_ALL, workflows);

        return workflows;
    }

    @Override
    public void clear() {
        cacheManager.getCacheNames().forEach(c -> cacheManager.getCache(c).clear());
    }
}
