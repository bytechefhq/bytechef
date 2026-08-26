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

package com.bytechef.platform.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.annotation.WorkflowCacheEvict;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkflowNodeTestOutputServiceImpl implements WorkflowNodeTestOutputService {

    private static final String WORKFLOW_TEST_NODE_OUTPUT_CACHE = "workflowTestNodeOutput";

    private final CacheManager cacheManager;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputServiceImpl(
        CacheManager cacheManager, WorkflowCacheManager workflowCacheManager,
        WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository) {

        this.cacheManager = cacheManager;
        this.workflowCacheManager = workflowCacheManager;
        this.workflowNodeTestOutputRepository = workflowNodeTestOutputRepository;
    }

    @Override
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public boolean checkWorkflowNodeTestOutputExists(
        String workflowId, String workflowNodeName, @Nullable Instant createdDate, long environmentId) {

        // hasPermission(#workflowId, 'Workflow', ...) above is environment-agnostic, so the caller-supplied
        // environmentId is never checked -- and until this fix, the repository queries below did not even use it,
        // making this an existence oracle across every environment regardless: a confined principal could learn
        // whether a workflow node has been tested in another environment it cannot itself read. No @Cacheable /
        // @WorkflowCacheEvict on this method, so resolving here (rather than at a caller) is safe. See
        // PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        if (createdDate == null) {
            return workflowNodeTestOutputRepository.existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
                workflowId, workflowNodeName, effectiveEnvironmentId);
        } else {
            return workflowNodeTestOutputRepository
                .existsByWorkflowIdAndWorkflowNodeNameAndEnvironmentIdAndLastModifiedDateAfter(
                    workflowId, workflowNodeName, effectiveEnvironmentId, createdDate);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')")
    @CacheEvict(value = WORKFLOW_TEST_NODE_OUTPUT_CACHE)
    @WorkflowCacheEvict(cacheNames = {
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE,
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE,
    })
    public void deleteWorkflowNodeTestOutput(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId, String workflowNodeName,
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {
        workflowNodeTestOutputRepository
            .findByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(workflowId, workflowNodeName, environmentId)
            .ifPresent(workflowNodeTestOutputRepository::delete);
    }

    /**
     * Evicts the same three caches the single node variant evicts through {@code @WorkflowCacheEvict}, which cannot be
     * used here because that annotation demands an environment id parameter and this method spans every environment.
     * The workflow-scoped evictions resolve the workflow's node names, so this must run while the workflow row still
     * exists - which is what the pre-delete (rather than post-delete) hook guarantees.
     */
    @Override
    public void deleteWorkflowNodeTestOutputs(String workflowId) {
        List<WorkflowNodeTestOutput> workflowNodeTestOutputs =
            workflowNodeTestOutputRepository.findByWorkflowId(workflowId);

        workflowNodeTestOutputRepository.deleteAll(workflowNodeTestOutputs);

        Set<Long> environmentIds = new HashSet<>();

        for (WorkflowNodeTestOutput workflowNodeTestOutput : workflowNodeTestOutputs) {
            Long environmentId = workflowNodeTestOutput.getEnvironmentId();

            environmentIds.add(environmentId);

            clearWorkflowTestNodeOutputCache(
                workflowId, workflowNodeTestOutput.getWorkflowNodeName(), environmentId);
        }

        for (Long environmentId : environmentIds) {
            workflowCacheManager.clearCacheForWorkflow(
                workflowId, WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE, environmentId);
            workflowCacheManager.clearCacheForWorkflow(
                workflowId, WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE, environmentId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WORKFLOW_TEST_NODE_OUTPUT_CACHE)
    public Optional<WorkflowNodeTestOutput> fetchWorkflowTestNodeOutput(
        String workflowId, String workflowNodeName, long environmentId) {

        return workflowNodeTestOutputRepository.findByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(
            workflowId, workflowNodeName, environmentId);
    }

    @Override
    public void removeUnusedNodeTestOutputs(Workflow workflow) {
        List<String> workflowTaskNames = workflow.getTasks(true)
            .stream()
            .map(WorkflowTask::getName)
            .toList();

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow)
            .stream()
            .toList();

        List<String> workflowTriggerNames = WorkflowTrigger.of(workflow)
            .stream()
            .map(WorkflowTrigger::getName)
            .toList();

        List<WorkflowNodeTestOutput> workflowNodeTestOutputs = workflowNodeTestOutputRepository
            .findByWorkflowId(Validate.notNull(workflow.getId(), "id"));

        for (WorkflowNodeTestOutput workflowNodeTestOutput : workflowNodeTestOutputs) {
            if (!workflowTaskNames.contains(workflowNodeTestOutput.getWorkflowNodeName()) &&
                !workflowTriggerNames.contains(workflowNodeTestOutput.getWorkflowNodeName())) {

                workflowNodeTestOutputRepository.delete(workflowNodeTestOutput);
            }

            if (workflowTriggerNames.contains(workflowNodeTestOutput.getWorkflowNodeName())) {
                workflowTriggers.stream()
                    .filter(workflowTrigger -> {
                        String name = workflowTrigger.getName();

                        return name.equals(workflowNodeTestOutput.getWorkflowNodeName());
                    })
                    .findFirst()
                    .ifPresent(workflowTrigger -> {
                        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                        if (!Objects.equals(
                            workflowNodeType.operation(),
                            workflowNodeTestOutput.getTypeOperationName())) {

                            workflowNodeTestOutputRepository.delete(workflowNodeTestOutput);
                        }
                    });
            }
        }
    }

    @Override
    @WorkflowCacheEvict(cacheNames = {
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE,
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE,
    })
    public WorkflowNodeTestOutput save(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId, String workflowNodeName,
        WorkflowNodeType workflowNodeType, OutputResponse outputResponse,
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {

        try {
            return save(
                workflowId, workflowNodeName, workflowNodeType, outputResponse.outputSchema(),
                outputResponse.sampleOutput(), environmentId);
        } finally {
            clearWorkflowTestNodeOutputCache(workflowId, workflowNodeName, environmentId);
        }
    }

    @Override
    public void updateWorkflowId(String oldWorkflowId, String newWorkflowId) {
        workflowNodeTestOutputRepository.updateWorkflowId(oldWorkflowId, newWorkflowId);
    }

    private WorkflowNodeTestOutput save(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType, BaseProperty outputSchema,
        Object sampleOutput, long environmentId) {

        WorkflowNodeTestOutput workflowNodeTestOutput = workflowNodeTestOutputRepository
            .findByWorkflowIdAndWorkflowNodeNameAndEnvironmentId(workflowId, workflowNodeName, environmentId)
            .orElse(new WorkflowNodeTestOutput());

        workflowNodeTestOutput.setEnvironmentId(environmentId);
        workflowNodeTestOutput.setTypeName(workflowNodeType.name());
        workflowNodeTestOutput.setTypeOperationName(workflowNodeType.operation());
        workflowNodeTestOutput.setTypeVersion(workflowNodeType.version());
        workflowNodeTestOutput.setOutputSchema(outputSchema);
        workflowNodeTestOutput.setSampleOutput(sampleOutput);
        workflowNodeTestOutput.setWorkflowId(workflowId);
        workflowNodeTestOutput.setWorkflowNodeName(workflowNodeName);

        return workflowNodeTestOutputRepository.save(workflowNodeTestOutput);
    }

    private void clearWorkflowTestNodeOutputCache(String workflowId, String workflowNodeName, long environmentId) {
        Objects.requireNonNull(cacheManager.getCache(WORKFLOW_TEST_NODE_OUTPUT_CACHE))
            .evict(TenantCacheKeyUtils.getKey(workflowId, workflowNodeName, environmentId));
    }
}
