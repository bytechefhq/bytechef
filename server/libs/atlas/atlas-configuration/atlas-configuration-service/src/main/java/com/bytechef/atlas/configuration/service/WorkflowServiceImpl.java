/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.OptimisticLockingFailureException;
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
        @NonNull String definition, @NonNull Format format, @NonNull SourceType sourceType, int type) {

        Validate.notNull(definition, "'definition' must not be null");
        Validate.notNull(format, "'format' must not be null");
        Validate.notNull(sourceType, "'sourceType' must not be null");

        // TODO Validate definition considering format value

        final Workflow workflow = new Workflow(definition, format, type);

        workflow.setNew(true);
        workflow.setSourceType(sourceType);

        Workflow savedWorkflow = CollectionUtils.getFirstFilter(
            workflowCrudRepositories,
            workflowCrudRepository -> Objects.equals(workflowCrudRepository.getSourceType(), sourceType),
            workflowCrudRepository -> workflowCrudRepository.save(workflow));

        return getWorkflow(Validate.notNull(savedWorkflow.getId(), "id"));
    }

    @Override
    public void delete(@NonNull String id) {
        Validate.notNull(id, "'id' must not be null");

        workflowCrudRepositories
            .stream()
            .filter(workflowCrudRepository -> OptionalUtils.isPresent(workflowCrudRepository.findById(id)))
            .findFirst()
            .ifPresent(workflowCrudRepository -> workflowCrudRepository.deleteById(id));
    }

    @Override
    public Workflow duplicateWorkflow(@NonNull String id) {
        Workflow workflow = getWorkflow(id);

        return create(
            workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType(), workflow.getType());
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
                logger.error(e.getMessage(), e);
            }
        }

        throw new IllegalArgumentException("Workflow %s does not exist".formatted(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows(int type) {
        List<Workflow> workflows;
        List<SourceType> sourceTypes = Arrays.asList(SourceType.values());

        List<WorkflowRepository> filteredWorkflowRepositories = CollectionUtils.filter(
            workflowRepositories, curWorkflowRepository -> sourceTypes.contains(curWorkflowRepository.getSourceType()));

        Cache cacheAll = Validate.notNull(cacheManager.getCache(CACHE_ALL), "cacheAll");

        if (cacheAll.get(CACHE_ALL) == null) {
            workflows = filteredWorkflowRepositories.stream()
                .flatMap(workflowRepository -> stream(type, workflowRepository))
                .sorted(WorkflowServiceImpl::compare)
                .collect(Collectors.toList());

            cacheAll.put(CACHE_ALL, workflows);
        } else {
            Cache.ValueWrapper valueWrapper = Validate.notNull(cacheAll.get(CACHE_ALL), "valueWrapper");

            workflows = CollectionUtils.filter(
                (List<Workflow>) valueWrapper.get(), workflow -> workflow.getType() == type);
        }

        return workflows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows(@NonNull List<String> workflowIds) {
        List<Workflow> workflows = new ArrayList<>();

        for (String workflowId : workflowIds) {
            workflows.add(getWorkflow(workflowId));
        }

        return CollectionUtils.sort(workflows, (workflow1, workflow2) -> {
            String label1 = workflow1.getLabel();

            return label1.compareTo(workflow2.getLabel());
        });
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

                    workflow.setSourceType(workflowRepository.getSourceType());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
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
                CollectionUtils
                    .findFirst(workflows, curWorkflow -> Objects.equals(curWorkflow.getId(), id))
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
    public Workflow update(@NonNull String id, @NonNull String definition, int version) {
        Validate.notNull(id, "'id' must not be null");
        Validate.notNull(definition, "'definition' must not be null");

        final Workflow workflow = getWorkflow(id);

        return CollectionUtils.getFirstFilter(
            workflowCrudRepositories,
            workflowCrudRepository -> OptionalUtils.isPresent(workflowCrudRepository.findById(id)),
            workflowCrudRepository -> update(definition, version, workflow, workflowCrudRepository));
    }

    private static int compare(Workflow a, Workflow b) {
        if (a.getLabel() == null || b.getLabel() == null) {
            return -1;
        }

        String label = a.getLabel();

        return label.compareTo(b.getLabel());
    }

    private static Stream<Workflow> stream(int type, WorkflowRepository workflowRepository) {
        return workflowRepository.findAll(type)
            .stream()
            .filter(Objects::nonNull)
            .peek(workflow -> workflow.setSourceType(workflowRepository.getSourceType()));
    }

    private Workflow update(
        String definition, int version, Workflow workflow, WorkflowCrudRepository workflowCrudRepository) {

        workflow.setDefinition(definition);
        workflow.setVersion(version);

        try {
            workflowCrudRepository.save(workflow);
        } catch (OptimisticLockingFailureException e) {
            refreshCache(Validate.notNull(workflow.getId(), "id"));

            throw e;
        }

        return updateCache(
            workflowCrudRepository
                .findById(workflow.getId())
                .map(curWorkflow -> {
                    curWorkflow.setSourceType(workflowCrudRepository.getSourceType());

                    return curWorkflow;
                })
                .get());
    }

    private Workflow updateCache(Workflow workflow) {
        Cache cacheOne = Validate.notNull(cacheManager.getCache(CACHE_ONE), "cacheOne");

        String workflowId = Validate.notNull(workflow.getId(), "id");

        if (cacheOne.get(workflowId) != null) {
            cacheOne.put(workflowId, workflow);
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
