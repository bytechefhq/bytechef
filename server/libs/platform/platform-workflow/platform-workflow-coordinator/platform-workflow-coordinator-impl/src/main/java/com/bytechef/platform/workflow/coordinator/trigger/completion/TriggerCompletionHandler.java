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

package com.bytechef.platform.workflow.coordinator.trigger.completion;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.JobInputConstants;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.JobMetadataKeys;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.TriggerJobParameterContributor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution.Status;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerCompletionHandler {

    private static final Logger log = LoggerFactory.getLogger(TriggerCompletionHandler.class);

    /**
     * Phase 17b: trigger extension key carrying static {@code JobParameter} overrides declared in the workflow JSON by
     * {@code KnowledgeBaseSourceWorkflowGenerator} / {@code ContextStoreWorkflowGenerator}. {@code WorkflowTrigger}
     * stuffs unknown top-level trigger keys into its {@code extensions} bag, so we read the block out of there.
     */
    private static final String TRIGGER_EXTENSION_JOB_PARAMETERS = "jobParameters";

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobFacade principalJobFacade;
    private final List<TriggerJobParameterContributor> triggerJobParameterContributors;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerCompletionHandler(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        List<TriggerJobParameterContributor> triggerJobParameterContributors,
        TriggerExecutionService triggerExecutionService, TriggerFileStorage triggerFileStorage,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobFacade = principalJobFacade;
        this.triggerJobParameterContributors = triggerJobParameterContributors;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    @SuppressWarnings("unchecked")
    public void handle(TriggerExecution triggerExecution) {
        Assert.notNull(triggerExecution, "'triggerExecution' must not be null");
        Assert.notNull(triggerExecution.getId(), "'triggerExecution.id' must not be null");

        if (log.isDebugEnabled()) {
            log.debug("handle: triggerExecution={}", triggerExecution);
        }

        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        triggerExecution.setStatus(Status.COMPLETED);

        if (triggerExecution.getId() == null) {
            triggerExecutionService.create(triggerExecution);
        } else {
            triggerExecutionService.update(triggerExecution);
        }

        if (triggerExecution.getState() != null) {
            triggerStateService.save(workflowExecutionId, triggerExecution.getState());
        }

        Map<String, Object> inputMap = (Map<String, Object>) getInputMap(workflowExecutionId);
        String workflowId = getWorkflowId(workflowExecutionId);
        Map<String, ?> metadataMap = enrichMetadataWithJobParameters(
            getMetadataMap(workflowExecutionId), workflowId, triggerExecution.getName(), workflowExecutionId);

        jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType())
            .validateConnectionsForJob(
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

        Object output = triggerExecution.getOutput() == null
            ? null
            : triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput());

        if (output == null) {
            triggerExecution.addJobId(
                createJob(
                    workflowId, withTriggerInputs(inputMap, triggerExecution.getName(), Map.of()),
                    workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
        } else if (!triggerExecution.isBatch() && output instanceof Collection<?> triggerOutputValues) {
            for (Object triggerOutputValue : triggerOutputValues) {
                triggerExecution.addJobId(
                    createJob(
                        workflowId, withTriggerInputs(inputMap, triggerExecution.getName(), triggerOutputValue),
                        workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
            }
        } else if (triggerExecution.isBatch()) {
            if (output instanceof Collection<?> collection && !collection.isEmpty()) {
                triggerExecution.addJobId(
                    createJob(
                        workflowId, withTriggerInputs(inputMap, triggerExecution.getName(), output),
                        workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
            }
        } else {
            triggerExecution.addJobId(
                createJob(
                    workflowId, withTriggerInputs(inputMap, triggerExecution.getName(), output),
                    workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
        }

        triggerExecutionService.update(triggerExecution);

        if (log.isDebugEnabled()) {
            log.debug(
                "Trigger id={}, type='{}', name='{}' completed",
                triggerExecution.getId(), triggerExecution.getType(), triggerExecution.getName());
        }
    }

    private long createJob(
        String workflowId, Map<String, ?> inputMap, long jobPrincipalId, Map<String, ?> metadataMap,
        PlatformType type) {

        return principalJobFacade.createJob(
            new JobParametersDTO(workflowId, inputMap, metadataMap), jobPrincipalId, type);
    }

    /**
     * Merges the firing trigger's output into {@code inputMap} under its own name, alongside the reserved
     * {@link JobInputConstants#TRIGGER_NAME_INPUT} key so downstream expressions can ask "which trigger fired" without
     * knowing the trigger's node name.
     */
    private static Map<String, ?> withTriggerInputs(
        Map<String, Object> inputMap, String triggerName, Object triggerOutputValue) {

        return MapUtils.concat(
            inputMap,
            Map.of(triggerName, triggerOutputValue, JobInputConstants.TRIGGER_NAME_INPUT, triggerName));
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getInputMap(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private Map<String, ?> getMetadataMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        return jobPrincipalAccessor.getMetadataMap(workflowExecutionId.getJobPrincipalId());
    }

    private Map<String, ?> enrichMetadataWithJobParameters(
        Map<String, ?> metadataMap, String workflowId, String triggerName, WorkflowExecutionId workflowExecutionId) {

        Workflow workflow;

        try {
            workflow = workflowService.getWorkflow(workflowId);
        } catch (RuntimeException ex) {
            // Workflow lookup failed — fall back to "no enrichment" so the trigger dispatch path doesn't get
            // blocked by a metadata-side hiccup. The dataStream reader interprets a missing __jobParameters as
            // pre-17b behavior (no since override, no mode override).
            log.warn(
                "Failed to load workflow {} for trigger {} jobParameter enrichment: {}",
                workflowId, triggerName, ex.getMessage());

            return metadataMap;
        }

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(triggerName, workflow);

        Map<String, Object> jobParameters = assembleJobParameters(
            workflowTrigger, workflow.getMetadata(), workflowExecutionId, triggerJobParameterContributors);

        if (jobParameters.isEmpty()) {
            return metadataMap;
        }

        Map<String, Object> enriched = new LinkedHashMap<>(metadataMap);

        enriched.put(JobMetadataKeys.JOB_PARAMETERS, jobParameters);

        return enriched;
    }

    /**
     * Phase 17b: assembles the {@code __jobParameters} sub-map for the firing trigger. Returns an empty map (signalling
     * "no enrichment") when neither the trigger's static {@code jobParameters} block nor any registered
     * {@link TriggerJobParameterContributor} produces an entry — that's the pre-17b single-trigger path and the spawned
     * job carries no out-of-band JobParameter hints.
     *
     * <p>
     * Merge order: trigger-declared entries first (the static {@code datastream.mode = PARTIAL | FULL_REPLACE} hint
     * emitted by the workflow generators), then each contributor's response layered on top. Contributors typically
     * write disjoint key namespaces (e.g. {@code datastream.since} from the source's {@code lastSyncRunAt}), so
     * collisions are not expected; if they happen, later contributors win.
     * </p>
     *
     * <p>
     * Package-private + static for direct unit-testability — the merge logic is a pure function of its inputs. The
     * caller resolves the {@code WorkflowTrigger} and {@code workflowMetadata} from a {@code Workflow} object (which
     * requires the platform's workflow mapper to be registered), then hands both to this helper so tests can construct
     * the trigger directly via the {@code Map}-source ctor without spinning up the full workflow-loading stack.
     * </p>
     *
     * @param workflowTrigger     the firing trigger as resolved from the workflow JSON, or {@code null} when the
     *                            trigger name didn't match any entry (benign — only contributors run).
     * @param workflowMetadata    the workflow's static metadata block — the discriminator key for contributors (e.g.
     *                            {@code contextStoreSourceId}, {@code knowledgeBaseSourceId}).
     * @param workflowExecutionId threaded through to contributors for subsystems keying on principal type.
     * @param contributors        registered {@link TriggerJobParameterContributor}s in Spring discovery order.
     */
    static Map<String, Object> assembleJobParameters(
        WorkflowTrigger workflowTrigger, Map<String, Object> workflowMetadata,
        WorkflowExecutionId workflowExecutionId, List<TriggerJobParameterContributor> contributors) {

        Map<String, Object> jobParameters = new LinkedHashMap<>();

        if (workflowTrigger != null) {
            // WorkflowTrigger stuffs unknown top-level trigger JSON keys into its extensions bag. The
            // jobParameters block emitted by the workflow generators (Phase 17b commit 4) lives there.
            Map<?, ?> triggerJobParameters = workflowTrigger.getExtension(
                TRIGGER_EXTENSION_JOB_PARAMETERS, Map.class, Map.of());

            triggerJobParameters.forEach((key, value) -> jobParameters.put(String.valueOf(key), value));
        }

        for (TriggerJobParameterContributor contributor : contributors) {
            Map<String, ?> contribution;

            try {
                contribution = contributor.contribute(workflowMetadata, workflowExecutionId);
            } catch (RuntimeException ex) {
                // The SPI contract forbids throwing, but defend against a misbehaving impl regardless — one bad
                // contributor must not block the dispatch path for the rest of the system.
                log.warn(
                    "TriggerJobParameterContributor {} threw while contributing: {}",
                    contributor.getClass()
                        .getSimpleName(),
                    ex.getMessage());

                continue;
            }

            if (contribution != null && !contribution.isEmpty()) {
                jobParameters.putAll(contribution);
            }
        }

        return jobParameters;
    }
}
