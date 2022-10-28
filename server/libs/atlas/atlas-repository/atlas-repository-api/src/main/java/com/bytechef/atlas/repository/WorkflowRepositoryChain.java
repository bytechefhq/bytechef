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

package com.bytechef.atlas.repository;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.commons.cache.Clearable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
    public void clear() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = Objects.requireNonNull(cacheManager.getCache(cacheName));

            cache.clear();
        });
    }

    @Override
    public void deleteById(String id) {
        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                workflowRepository.deleteById(id);

                Cache cacheOne = cacheManager.getCache(CACHE_ONE);

                if (cacheOne != null) {
                    cacheOne.evict(id);
                }

                Cache cacheAll = cacheManager.getCache(CACHE_ALL);

                if (cacheAll != null) {
                    cacheAll.clear();
                }
            } catch (UnsupportedOperationException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}", e.getMessage());
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Workflow> findById(String id) {
        Cache cacheOne = cacheManager.getCache(CACHE_ONE);

        if (Objects.requireNonNull(cacheOne).get(id) != null) {
            Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cacheOne.get(id));

            return Optional.of((Workflow) Objects.requireNonNull(valueWrapper.get()));
        }

        Cache cacheAll = cacheManager.getCache(CACHE_ALL);

        if (Objects.requireNonNull(cacheAll).get(CACHE_ALL) != null) {
            Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cacheAll.get(CACHE_ALL));

            List<Workflow> workflows = (List<Workflow>) Objects.requireNonNull(valueWrapper.get());

            for (Workflow workflow : workflows) {
                if (Objects.equals(workflow.getId(), id)) {
                    return Optional.of(workflow);
                }
            }
        }

        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                Workflow workflow = workflowRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + id));

                cacheOne.put(id, workflow);

                return Optional.of(workflow);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}", e.getMessage());
                }
            }
        }

        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<Workflow> findAll() {
        List<Workflow> workflows;

        Cache cacheAll = Objects.requireNonNull(cacheManager.getCache(CACHE_ALL));

        if (cacheAll.get(CACHE_ALL) == null) {
            workflows = workflowRepositories.stream()
                    .map(WorkflowRepository::findAll)
                    .flatMap(iterable -> StreamSupport.stream(iterable.spliterator(), false))
                    .sorted((a, b) -> {
                        if (a.getLabel() == null || b.getLabel() == null) {
                            return -1;
                        }
                        return a.getLabel().compareTo(b.getLabel());
                    })
                    .collect(Collectors.toList());

            cacheAll.put(CACHE_ALL, workflows);
        } else {
            Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cacheAll.get(CACHE_ALL));

            workflows = (List<Workflow>) valueWrapper.get();
        }

        return workflows;
    }

    @Override
    public Workflow save(Workflow workflow) {
        if (workflow.getId() == null) {
            return create(workflow);
        } else {
            return update(workflow);
        }
    }

    private Workflow create(Workflow workflow) {
        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                workflow = workflowRepository.save(workflow);

                Cache cacheOne = Objects.requireNonNull(cacheManager.getCache(CACHE_ONE));

                cacheOne.put(Objects.requireNonNull(workflow.getId()), workflow);

                return workflow;
            } catch (UnsupportedOperationException e) {
                logger.debug("Repository {} doesn't support create operation", workflowRepository);
            }
        }

        throw new RuntimeException(
                "Set atlas.workflow-repository.database.enabled=true property to create new workflow");
    }

    private Workflow update(Workflow workflow) {
        for (WorkflowRepository repository : workflowRepositories) {
            try {
                workflow = repository.save(workflow);

                Cache cacheOne = Objects.requireNonNull(cacheManager.getCache(CACHE_ONE));

                cacheOne.put(Objects.requireNonNull(workflow.getId()), workflow);

                return workflow;
            } catch (UnsupportedOperationException e) {
                logger.debug("Repository {} doesn't support update operation", repository);
            }
        }

        throw new RuntimeException("Set atlas.workflow-repository.database.enabled=true property to update a workflow");
    }
}
