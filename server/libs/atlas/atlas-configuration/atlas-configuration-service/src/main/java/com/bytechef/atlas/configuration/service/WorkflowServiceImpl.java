/*
 * Copyright 2025 ByteChef
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
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.exception.ConfigurationException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private static final String WORKFLOW_CACHE = WorkflowService.class.getName() + ".workflow";
    private static final String WORKFLOWS_CACHE = WorkflowService.class.getName() + ".workflows";

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
    public Workflow create(String definition, Format format, SourceType sourceType) {
        Assert.notNull(definition, "'definition' must not be null");
        Assert.notNull(format, "'format' must not be null");
        Assert.notNull(sourceType, "'sourceType' must not be null");

        // TODO Validate definition considering format value

        final Workflow workflow = new Workflow(definition, format);

        workflow.setNew(true);
        workflow.setSourceType(sourceType);

        Workflow savedWorkflow = CollectionUtils.getFirstFilter(
            workflowCrudRepositories,
            workflowCrudRepository -> Objects.equals(workflowCrudRepository.getSourceType(), sourceType),
            workflowCrudRepository -> workflowCrudRepository.save(workflow));

        return getWorkflow(Validate.notNull(savedWorkflow.getId(), "id"));
    }

    @Override
    public void delete(String id) {
        Assert.notNull(id, "'id' must not be null");

        workflowCrudRepositories
            .stream()
            .filter(workflowCrudRepository -> OptionalUtils.isPresent(workflowCrudRepository.findById(id)))
            .findFirst()
            .ifPresent(workflowCrudRepository -> workflowCrudRepository.deleteById(id));
    }

    @Override
    public void delete(List<String> ids) {
        for (String id : ids) {
            delete(id);
        }
    }

    @Override
    public Workflow duplicateWorkflow(String id) {
        Workflow workflow = getWorkflow(id);

        return create(workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workflow> fetchWorkflow(String id) {
        Assert.notNull(id, "'id' must not be null");

        Cache workflowCache = Validate.notNull(cacheManager.getCache(WORKFLOW_CACHE), WORKFLOW_CACHE);

        if (workflowCache.get(id) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(workflowCache.get(id), "valueWrapper");

            return Optional.ofNullable((Workflow) valueWrapper.get());
        }

        Cache workflowsCache = Validate.notNull(cacheManager.getCache(WORKFLOWS_CACHE), WORKFLOWS_CACHE);

        if (workflowsCache.get(WORKFLOWS_CACHE) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(workflowsCache.get(WORKFLOWS_CACHE), "valueWrapper");

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) valueWrapper.get();

            for (Workflow workflow : Validate.notNull(workflows, "workflows")) {
                if (Objects.equals(workflow.getId(), id)) {
                    return Optional.of(workflow);
                }
            }
        }

        for (WorkflowRepository workflowRepository : workflowRepositories) {
            try {
                Optional<Workflow> workflowOptional = workflowRepository.findById(id);

                if (workflowOptional.isPresent()) {
                    Workflow workflow = workflowOptional.get();

                    workflow.setSourceType(workflowRepository.getSourceType());

                    workflowCache.put(id, workflow);

                    return Optional.of(workflow);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Workflow getWorkflow(String id) {
        return fetchWorkflow(id)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with id: %s does not exist".formatted(id), WorkflowErrorType.WORKFLOW_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Workflow> getWorkflows() {
        List<Workflow> workflows;
        List<SourceType> sourceTypes = Arrays.asList(SourceType.values());

        List<WorkflowRepository> filteredWorkflowRepositories = CollectionUtils.filter(
            workflowRepositories, curWorkflowRepository -> sourceTypes.contains(curWorkflowRepository.getSourceType()));

        Cache workflowsCache = Validate.notNull(cacheManager.getCache(WORKFLOWS_CACHE), WORKFLOWS_CACHE);

        if (workflowsCache.get(WORKFLOWS_CACHE) == null) {
            workflows = filteredWorkflowRepositories.stream()
                .flatMap(WorkflowServiceImpl::stream)
                .sorted(WorkflowServiceImpl::compare)
                .collect(Collectors.toList());

            workflowsCache.put(WORKFLOWS_CACHE, workflows);
        } else {
            Cache.ValueWrapper valueWrapper = Validate.notNull(workflowsCache.get(WORKFLOWS_CACHE), WORKFLOWS_CACHE);

            workflows = (List<Workflow>) valueWrapper.get();
        }

        return workflows;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows(List<String> workflowIds) {
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
    public void refreshCache(String id) {
        Assert.notNull(id, "'id' must not be null");

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

        Cache workflowCache = Validate.notNull(cacheManager.getCache(WORKFLOW_CACHE), WORKFLOW_CACHE);

        if (workflowCache.get(id) != null) {
            if (workflow == null) {
                workflowCache.evictIfPresent(id);
            } else {
                workflowCache.put(id, workflow);
            }
        }

        Cache workflowsCache = Validate.notNull(cacheManager.getCache(WORKFLOWS_CACHE), WORKFLOWS_CACHE);

        if (workflowsCache.get(WORKFLOWS_CACHE) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(workflowsCache.get(WORKFLOWS_CACHE), WORKFLOW_CACHE);

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

            workflowsCache.put(WORKFLOWS_CACHE, workflows);
        }
    }

    @Override
    public Workflow update(String id, String definition, int version) {
        Assert.notNull(id, "'id' must not be null");
        Assert.notNull(definition, "'definition' must not be null");

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

    private static Stream<Workflow> stream(WorkflowRepository workflowRepository) {
        return workflowRepository.findAll()
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
                .orElseThrow(() -> new ConfigurationException(
                    "Workflow with id: %s does not exist".formatted(workflow.getId()),
                    WorkflowErrorType.WORKFLOW_NOT_FOUND)));
    }

    private Workflow updateCache(Workflow workflow) {
        Cache workflowCache = Validate.notNull(cacheManager.getCache(WORKFLOW_CACHE), WORKFLOW_CACHE);

        String workflowId = Validate.notNull(workflow.getId(), "id");

        if (workflowCache.get(workflowId) != null) {
            workflowCache.put(workflowId, workflow);
        }

        Cache workflowsCache = Validate.notNull(cacheManager.getCache(WORKFLOWS_CACHE), WORKFLOWS_CACHE);

        if (workflowsCache.get(WORKFLOWS_CACHE) != null) {
            Cache.ValueWrapper valueWrapper = Validate.notNull(workflowsCache.get(WORKFLOWS_CACHE), WORKFLOWS_CACHE);

            @SuppressWarnings("unchecked")
            List<Workflow> workflows = (List<Workflow>) Validate.notNull(valueWrapper.get(), "workflows");

            int index = workflows.indexOf(workflow);

            if (index == -1) {
                workflows.add(workflow);
            } else {
                workflows.remove(index);

                workflows.add(index, workflow);
            }

            workflowsCache.put(WORKFLOWS_CACHE, workflows);
        }

        return workflow;
    }
}
