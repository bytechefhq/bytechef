/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.workflow;

import com.integri.atlas.engine.coordinator.cache.Clearable;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Jun 2, 2017
 */
public class WorkflowRepositoryChain implements WorkflowRepository, Clearable {

    private final List<WorkflowRepository> repositories;

    private CacheManager cacheManager = new ConcurrentMapCacheManager();

    private static final String CACHE_ALL = WorkflowRepositoryChain.class.getName() + ".all";
    private static final String CACHE_ONE = WorkflowRepositoryChain.class.getName() + ".one";

    private Logger logger = LoggerFactory.getLogger(getClass());

    public WorkflowRepositoryChain(List<WorkflowRepository> aRepositories) {
        Assert.notNull(aRepositories, "'aRepositories' can not be null");
        repositories = aRepositories;
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
        for (WorkflowRepository repository : repositories) {
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

        List<Workflow> workflows = repositories
            .stream()
            .map(r -> r.findAll())
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
        cacheManager.getCacheNames().stream().forEach(c -> cacheManager.getCache(c).clear());
    }
}
