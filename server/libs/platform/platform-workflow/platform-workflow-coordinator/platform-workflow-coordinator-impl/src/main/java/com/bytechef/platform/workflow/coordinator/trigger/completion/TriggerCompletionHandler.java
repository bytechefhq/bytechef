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
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.JobMetadataKeys;
import com.bytechef.platform.workflow.coordinator.trigger.jobparameter.TriggerJobParameterContributor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution.Status;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerStateFacade;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
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

    private static final String TRIGGER_EXTENSION_JOB_PARAMETERS = "jobParameters";

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobFacade principalJobFacade;
    private final List<TriggerJobParameterContributor> triggerJobParameterContributors;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerStateFacade triggerStateFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerCompletionHandler(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        List<TriggerJobParameterContributor> triggerJobParameterContributors,
        TriggerExecutionService triggerExecutionService, TriggerFileStorage triggerFileStorage,
        TriggerStateFacade triggerStateFacade, WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobFacade = principalJobFacade;
        this.triggerJobParameterContributors = triggerJobParameterContributors;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerStateFacade = triggerStateFacade;
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
            triggerStateFacade.save(workflowExecutionId, triggerExecution.getState());
        }

        Map<String, Object> inputMap = (Map<String, Object>) getInputMap(workflowExecutionId);
        String workflowId = getWorkflowId(workflowExecutionId);
        Map<String, ?> metadataMap = enrichMetadataWithJobParameters(
            getMetadataMap(workflowExecutionId), workflowId, triggerExecution.getName(), workflowExecutionId);

        Object output = triggerExecution.getOutput() == null
            ? null
            : triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput());

        if (output == null) {
            triggerExecution.addJobId(
                createJob(
                    workflowId,
                    MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), Map.of())),
                    workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
        } else if (!triggerExecution.isBatch() && output instanceof Collection<?> triggerOutputValues) {
            for (Object triggerOutputValue : triggerOutputValues) {
                triggerExecution.addJobId(
                    createJob(
                        workflowId,
                        MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), triggerOutputValue)),
                        workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
            }
        } else if (triggerExecution.isBatch()) {
            if (output instanceof Collection<?> collection && !collection.isEmpty()) {
                triggerExecution.addJobId(
                    createJob(
                        workflowId,
                        MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), output)),
                        workflowExecutionId.getJobPrincipalId(), metadataMap, workflowExecutionId.getType()));
            }
        } else {
            triggerExecution.addJobId(
                createJob(
                    workflowId,
                    MapUtils.concat(inputMap, Map.of(triggerExecution.getName(), output)),
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
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to load workflow {} for trigger {} jobParameter enrichment: {}",
                workflowId, triggerName, exception.getMessage());

            return metadataMap;
        }

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(triggerName, workflow);

        Map<String, Object> jobParameters = assembleJobParameters(
            workflowTrigger, workflow.getMetadata(), workflowExecutionId, triggerJobParameterContributors);

        if (jobParameters.isEmpty()) {
            return metadataMap;
        }

        Map<String, Object> enrichedMetadataMap = new LinkedHashMap<>(metadataMap);

        enrichedMetadataMap.put(JobMetadataKeys.JOB_PARAMETERS, jobParameters);

        return enrichedMetadataMap;
    }

    static Map<String, Object> assembleJobParameters(
        WorkflowTrigger workflowTrigger, Map<String, Object> workflowMetadata,
        WorkflowExecutionId workflowExecutionId, List<TriggerJobParameterContributor> contributors) {

        Map<String, Object> jobParameters = new LinkedHashMap<>();

        if (workflowTrigger != null) {
            Map<?, ?> triggerJobParameters = workflowTrigger.getExtension(
                TRIGGER_EXTENSION_JOB_PARAMETERS, Map.class, Map.of());

            triggerJobParameters.forEach((key, value) -> jobParameters.put(String.valueOf(key), value));
        }

        for (TriggerJobParameterContributor contributor : contributors) {
            Map<String, ?> contribution;

            try {
                contribution = contributor.contribute(workflowMetadata, workflowExecutionId);
            } catch (RuntimeException exception) {
                log.warn(
                    "TriggerJobParameterContributor {} threw while contributing: {}",
                    contributor.getClass()
                        .getSimpleName(),
                    exception.getMessage());

                continue;
            }

            if (contribution != null && !contribution.isEmpty()) {
                jobParameters.putAll(contribution);
            }
        }

        return jobParameters;
    }
}
