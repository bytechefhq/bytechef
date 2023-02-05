
/*
 * Copyright 2021 <your company/name>.
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
 */

package com.bytechef.atlas.service.impl;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowCrudRepository;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Ivica Cardic
 */
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private static final String CACHE_ALL = WorkflowService.class.getName() + ".all";
    private static final String CACHE_ONE = WorkflowService.class.getName() + ".one";

    private final CacheManager cacheManager;
    private final List<WorkflowCrudRepository> workflowCrudRepositories;
    private final List<WorkflowRepository> workflowRepositories;

    @SuppressFBWarnings("EI2")
    public WorkflowServiceImpl(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        this.cacheManager = cacheManager;
        this.workflowCrudRepositories = workflowCrudRepositories;
        this.workflowRepositories = workflowRepositories;
    }

    @Override
    public Workflow create(
        String definition, @NonNull Workflow.Format format, @NonNull Workflow.SourceType sourceType) {
        Assert.notNull(format, "'format' must not be null");
        Assert.notNull(sourceType, "'sourceType' must not be null");

        if (ObjectUtils.isEmpty(definition)) {
            definition = "{\"label\": \"New Workflow\", \"tasks\": []}";
        }

        Workflow workflow = new Workflow(definition, format);

        workflow.setNew(true);

        return workflowCrudRepositories.stream()
            .filter(workflowCrudRepository -> Objects.equals(workflowCrudRepository.getSourceType(), sourceType))
            .findFirst()
            .map(workflowCrudRepository -> save(workflow, workflowCrudRepository))
            .orElseThrow();
    }

    @Override
    public void delete(@NonNull String id) {
        Assert.notNull(id, "'id' must not be null");

        workflowCrudRepositories.stream()
            .filter(workflowCrudRepository -> workflowCrudRepository.findById(id)
                .isPresent())
            .findFirst()
            .ifPresent(workflowCrudRepository -> workflowCrudRepository.deleteById(id));
    }

    @Override
    @SuppressFBWarnings("NP")
    @Transactional(readOnly = true)
    public Workflow getWorkflow(@NonNull String id) {
        Assert.notNull(id, "'id' must not be null");

        Cache cacheOne = Objects.requireNonNull(cacheManager.getCache(CACHE_ONE));

        if (cacheOne.get(id) != null) {
            Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cacheOne.get(id));

            return (Workflow) valueWrapper.get();
        }

        Cache cacheAll = Objects.requireNonNull(cacheManager.getCache(CACHE_ALL));

        if (cacheAll.get(CACHE_ALL) != null) {
            Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cacheAll.get(CACHE_ALL));

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) valueWrapper.get();

            for (Workflow workflow : Objects.requireNonNull(workflows)) {
                if (Objects.equals(workflow.getId(), id)) {
                    return workflow;
                }
            }
        }

        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                Optional<Workflow> workflowOptional = workflowRepository.findById(id);

                if (workflowOptional.isPresent()) {
                    Workflow workflow = workflowOptional.get();

                    workflow.setSourceType(workflowRepository.getSourceType());

                    cacheOne.put(id, workflow);

                    return workflow;
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}", e.getMessage());
                }
            }
        }

        throw new IllegalArgumentException("Workflow %s does not exist".formatted(id));
    }

    @Override
    public List<Workflow> getWorkflows(List<String> workflowIds) {
        List<Workflow> workflows = new ArrayList<>();

        for (String workflowId : workflowIds) {
            workflows.add(getWorkflow(workflowId));
        }

        return workflows;
    }

    @Override
    @SuppressFBWarnings("NP")
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows() {
        List<Workflow> workflows;

        Cache cacheAll = Objects.requireNonNull(cacheManager.getCache(CACHE_ALL));

        if (cacheAll.get(CACHE_ALL) == null) {
            workflows = workflowRepositories.stream()
                .flatMap(workflowRepository -> {
                    Iterable<Workflow> iterable = workflowRepository.findAll();

                    return StreamSupport.stream(iterable.spliterator(), false)
                        .peek(workflow -> workflow.setSourceType(workflowRepository.getSourceType()));
                })
                .sorted((a, b) -> {
                    if (a.getLabel() == null || b.getLabel() == null) {
                        return -1;
                    }

                    String label = a.getLabel();

                    return label.compareTo(b.getLabel());
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
    public Workflow update(@NonNull String id, @NonNull String definition) {
        Assert.notNull(id, "'id' must not be null");
        Assert.notNull(definition, "'definition' must not be null");

        Workflow workflow = getWorkflow(id);

        workflow.setDefinition(definition);

        return workflowCrudRepositories.stream()
            .filter(workflowCrudRepository -> workflowCrudRepository.findById(workflow.getId())
                .isPresent())
            .findFirst()
            .map(workflowCrudRepository -> save(workflow, workflowCrudRepository))
            .orElseThrow();
    }

    @SuppressFBWarnings("NP")
    private Workflow save(Workflow workflow, WorkflowCrudRepository workflowCrudRepository) {
        if (workflow.isNew()) {
            workflow = workflowCrudRepository.save(workflow);
        } else {
            Workflow curWorkflow = workflowCrudRepository.findById(workflow.getId())
                .orElseThrow();

            curWorkflow.setDefinition(workflow.getDefinition());
            curWorkflow.setVersion(workflow.getVersion());

            workflow = workflowCrudRepository.save(curWorkflow);
        }

        Cache cacheOne = Objects.requireNonNull(cacheManager.getCache(CACHE_ONE));

        if (cacheOne.get(Objects.requireNonNull(workflow.getId())) != null) {
            cacheOne.put(workflow.getId(), workflow);
        }

        Cache cacheAll = Objects.requireNonNull(cacheManager.getCache(CACHE_ALL));

        if (cacheAll.get(CACHE_ALL) != null) {
            Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cacheAll.get(CACHE_ALL));

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) Objects.requireNonNull(valueWrapper.get());

            int index = workflows.indexOf(workflow);

            if (index != -1) {
                workflows.remove(index);

                workflows.add(index, workflow);

                cacheAll.put(CACHE_ALL, workflows);
            }

            Iterator<Workflow> iterator = workflows.iterator();

            while (iterator.hasNext()) {
                Workflow curWorkflow = iterator.next();

                if (Objects.equals(curWorkflow, workflow)) {
                    iterator.remove();

                    break;
                }
            }
        }

        return workflow;
    }
}
