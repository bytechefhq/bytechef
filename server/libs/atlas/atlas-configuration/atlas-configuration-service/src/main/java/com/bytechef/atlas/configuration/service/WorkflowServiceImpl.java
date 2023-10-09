
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

package com.bytechef.atlas.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

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
        String definition, @NonNull Workflow.Format format, @NonNull SourceType sourceType, int type) {

        Validate.notNull(format, "'format' must not be null");
        Validate.notNull(sourceType, "'sourceType' must not be null");

        if (ObjectUtils.isEmpty(definition)) {
            definition = "{\"label\": \"New Workflow\", \"tasks\": []}";
        }

        final Workflow workflow = new Workflow(definition, format, type);

        workflow.setNew(true);

        Workflow savedWorkflow = CollectionUtils.getFirst(
            workflowCrudRepositories,
            workflowCrudRepository -> Objects.equals(workflowCrudRepository.getSourceType(), sourceType),
            workflowCrudRepository -> workflowCrudRepository.save(workflow));


        return getWorkflow(Validate.notNull(savedWorkflow.getId(), "id"));
    }

    @Override
    public void delete(@NonNull String id) {
        Validate.notNull(id, "'id' must not be null");

        workflowCrudRepositories.stream()
            .filter(workflowCrudRepository -> OptionalUtils.isPresent(workflowCrudRepository.findById(id)))
            .findFirst()
            .ifPresent(workflowCrudRepository -> workflowCrudRepository.deleteById(id));
    }

    @Override
    public Workflow duplicateWorkflow(@NonNull String id) {
        Workflow workflow = getWorkflow(id);

        return create(workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType(), workflow.getType());
    }

    @Override
    public List<Workflow> getFilesystemWorkflows(int type) {
        WorkflowRepository workflowRepository = CollectionUtils.getFirst(
            workflowRepositories,
            curWorkflowRepository -> curWorkflowRepository.getSourceType() == SourceType.FILESYSTEM);

        return workflowRepository.findAll(type)
            .stream()
            .peek(workflow -> workflow.setSourceType(workflowRepository.getSourceType()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Workflow getWorkflow(@NonNull String id) {
        Validate.notNull(id, "'id' must not be null");

        Cache cacheOne = Validate.notNull(cacheManager.getCache(CACHE_ONE), "cacheOne");

        if (cacheOne.get(id) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(cacheOne.get(id), "valueWrapper");

            return (Workflow) valueWrapper.get();
        }

        Cache cacheAll = Validate.notNull(cacheManager.getCache(CACHE_ALL), "cacheAll");

        if (cacheAll.get(CACHE_ALL) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(cacheAll.get(CACHE_ALL), "valueWrapper");

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) valueWrapper.get();

            for (Workflow workflow : Validate.notNull(workflows, "workflows")) {
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
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows(@NonNull List<String> workflowIds) {
        List<Workflow> workflows = new ArrayList<>();

        for (String workflowId : workflowIds) {
            workflows.add(getWorkflow(workflowId));
        }

        return workflows;
    }

    @Override
    public void refreshCache(@NonNull String id) {
        Validate.notNull(id, "'id' must not be null");

        Workflow workflow = null;

        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                Optional<Workflow> workflowOptional = workflowRepository.findById(id);

                if (workflowOptional.isPresent()) {
                    workflow = workflowOptional.get();
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}", e.getMessage());
                }
            }
        }

        Cache cacheOne = Validate.notNull(cacheManager.getCache(CACHE_ONE), "cacheOne");

        if (cacheOne.get(id) != null) {
            if (workflow == null) {
                cacheOne.evictIfPresent(id);
            } else {
                cacheOne.put(id, workflow);
            }
        }

        Cache cacheAll = Validate.notNull(cacheManager.getCache(CACHE_ALL), "cacheAll");

        if (cacheAll.get(CACHE_ALL) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(cacheAll.get(CACHE_ALL), "valueWrapper");

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) Validate.notNull(valueWrapper.get(), "workflows");

            if (workflow == null) {
                CollectionUtils.findFirst(workflows, curWorkflow -> Objects.equals(curWorkflow.getId(), id))
                    .ifPresent(workflows::remove);
            } else {
                int index = workflows.indexOf(workflow);

                if (index == -1) {
                    workflows.add(workflow);
                } else {
                    workflows.remove(index);

                    workflows.add(index, workflow);
                }
            }

            cacheAll.put(CACHE_ALL, workflows);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows(int type) {
        List<Workflow> workflows;

        Cache cacheAll = Validate.notNull(cacheManager.getCache(CACHE_ALL), "cacheAll");

        if (cacheAll.get(CACHE_ALL) == null) {
            workflows = workflowRepositories.stream()
                .flatMap(workflowRepository -> {
                    List<Workflow> curWorkflows = workflowRepository.findAll(type);

                    return curWorkflows
                        .stream()
                        .filter(Objects::nonNull)
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
            Cache.ValueWrapper valueWrapper = Validate.notNull(cacheAll.get(CACHE_ALL), "valueWrapper");

            workflows = (List<Workflow>) valueWrapper.get();
        }

        return workflows;
    }

    @Override
    public Workflow update(@NonNull String id, @NonNull String definition) {
        Validate.notNull(id, "'id' must not be null");
        Validate.notNull(definition, "'definition' must not be null");

        Workflow curWorkflow = getWorkflow(id);

        return CollectionUtils.getFirst(
            workflowCrudRepositories,
            workflowCrudRepository -> OptionalUtils.isPresent(workflowCrudRepository.findById(curWorkflow.getId())),
            workflowCrudRepository -> {
                Workflow workflow  = new Workflow(
                    curWorkflow.getId(), curWorkflow.getDefinition(), curWorkflow.getFormat(), curWorkflow.getType());

                workflow.setVersion(curWorkflow.getVersion());

                return workflowCrudRepository.save(workflow);
            });
    }

    private Workflow updateCache(Workflow workflow, WorkflowCrudRepository workflowCrudRepository) {
        // Load definition into Workflow instance

        workflow = OptionalUtils.get(workflowCrudRepository.findById(workflow.getId()));

        Cache cacheOne = Validate.notNull(cacheManager.getCache(CACHE_ONE), "cacheOne");

        if (cacheOne.get(Validate.notNull(workflow.getId(), "id")) != null) {
            cacheOne.put(Validate.notNull(workflow.getId(), "id"), workflow);
        }

        Cache cacheAll = Validate.notNull(cacheManager.getCache(CACHE_ALL), "cacheAll");

        if (cacheAll.get(CACHE_ALL) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(cacheAll.get(CACHE_ALL), "valueWrapper");

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) Validate.notNull(valueWrapper.get(), "workflows");

            int index = workflows.indexOf(workflow);

            if (index == -1) {
                workflows.add(workflow);
            } else {
                workflows.remove(index);

                workflows.add(index, workflow);
            }

            cacheAll.put(CACHE_ALL, workflows);
        }

        return workflow;
    }
}
