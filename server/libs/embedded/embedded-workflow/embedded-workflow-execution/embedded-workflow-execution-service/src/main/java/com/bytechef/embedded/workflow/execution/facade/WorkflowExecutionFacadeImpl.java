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

package com.bytechef.embedded.workflow.execution.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.workflow.execution.dto.WorkflowExecution;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.registry.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service("com.bytechef.embedded.execution.facade.WorkflowExecutionFacade")
public class WorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final InstanceJobService instanceJobService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationService integrationService;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        InstanceJobService instanceJobService, IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        JobService jobService, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        TriggerExecutionService triggerExecutionService, TriggerFileStorage triggerFileStorage,
        WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.instanceJobService = instanceJobService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.jobService = jobService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorage = triggerFileStorage;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowExecution getWorkflowExecution(long id) {
        Job job = jobService.getJob(id);

        JobDTO jobDTO = new JobDTO(
            job, taskFileStorage.readJobOutputs(job.getOutputs()), getJobTaskExecutions(id));
        Optional<Long> projectInstanceIdOptional = instanceJobService.fetchJobInstanceId(
            Validate.notNull(job.getId(), ""), Type.EMBEDDED);

        return new WorkflowExecution(
            Validate.notNull(jobDTO.id(), "id"),
            integrationService.getWorkflowIntegration(jobDTO.workflowId()),
            OptionalUtils.map(projectInstanceIdOptional, integrationInstanceService::getIntegrationInstance),
            jobDTO, workflowService.getWorkflow(jobDTO.workflowId()),
            getTriggerExecutionDTO(
                OptionalUtils.orElse(projectInstanceIdOptional, null),
                triggerExecutionService.getJobTriggerExecution(Validate.notNull(job.getId(), "id")), job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecution> getWorkflowExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long integrationId,
        Long integrationInstanceId, String workflowId, int pageNumber) {

        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            workflowIds.add(workflowId);
        } else if (integrationId != null) {
            Integration integration = integrationService.getIntegration(integrationId);

            workflowIds.addAll(integration.getWorkflowIds());
        } else {
            workflowIds.addAll(
                CollectionUtils.map(
                    workflowService.getWorkflows(Type.EMBEDDED.getId()), Workflow::getId));
        }

        if (workflowIds.isEmpty()) {
            return Page.empty();
        } else {
            Page<Job> jobIdsPage = instanceJobService
                .getJobIds(
                    jobStatus, jobStartDate, jobEndDate, integrationInstanceId, Type.EMBEDDED,
                    workflowIds, pageNumber)
                .map(jobService::getJob);

            List<Integration> integrations = new ArrayList<>();

            if (integrationId == null) {
                integrations.addAll(integrationService.getIntegrations());
            } else {
                integrations.add(integrationService.getIntegration(integrationId));
            }

            List<Workflow> workflows = workflowService.getWorkflows(
                CollectionUtils.map(jobIdsPage.toList(), Job::getWorkflowId));

            return jobIdsPage.map(job -> new WorkflowExecution(
                Validate.notNull(job.getId(), "id"),
                CollectionUtils.getFirst(
                    integrations,
                    integration -> CollectionUtils.contains(integration.getWorkflowIds(), job.getWorkflowId())),
                OptionalUtils.map(
                    instanceJobService.fetchJobInstanceId(job.getId(), Type.EMBEDDED),
                    integrationInstanceService::getIntegrationInstance),
                new JobDTO(job),
                CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())),
                getTriggerExecutionDTO(
                    integrationInstanceId,
                    triggerExecutionService.getJobTriggerExecution(Validate.notNull(job.getId(), "id")), job)));
        }
    }

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return CollectionUtils.map(
            taskExecutionService.getJobTaskExecutions(jobId),
            taskExecution -> new TaskExecutionDTO(
                taskExecutionService.getTaskExecution(taskExecution.getId()),
                getComponentDefinition(taskExecution.getType()),
                taskFileStorage.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)),
                taskExecution.getOutput() == null
                    ? null
                    : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput())));
    }

    private ComponentDefinition getComponentDefinition(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return componentDefinitionService.getComponentDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion());
    }

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number integrationInstanceId, TriggerExecution triggerExecution, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (integrationInstanceId != null) {
            IntegrationInstanceWorkflow integrationInstanceWorkflow =
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(
                    integrationInstanceId.longValue(), job.getWorkflowId());

            triggerExecutionDTO = new TriggerExecutionDTO(
                getComponentDefinition(triggerExecution.getType()), integrationInstanceWorkflow.getInputs(),
                triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()), triggerExecution);
        }

        return triggerExecutionDTO;
    }
}
