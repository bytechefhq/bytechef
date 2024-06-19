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
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.dto.WorkflowDTO;
import com.bytechef.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.embedded.workflow.execution.dto.WorkflowExecution;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
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
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class IntegrationWorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final InstanceJobService instanceJobService;
    private final IntegrationFacade integrationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        InstanceJobService instanceJobService, IntegrationFacade integrationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        JobService jobService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, TriggerExecutionService triggerExecutionService,
        TriggerFileStorage triggerFileStorage, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.instanceJobService = instanceJobService;
        this.integrationFacade = integrationFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.jobService = jobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
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
        long integrationInstanceId = instanceJobService.getJobInstanceId(
            Validate.notNull(job.getId(), ""), AppType.EMBEDDED);

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        return new WorkflowExecution(
            Validate.notNull(jobDTO.id(), "id"),
            integrationService.getWorkflowIntegration(jobDTO.workflowId()),
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstance.getIntegrationInstanceConfigurationId()),
            integrationInstance,
            jobDTO, workflowService.getWorkflow(jobDTO.workflowId()),
            getTriggerExecutionDTO(
                integrationInstanceId,
                OptionalUtils.orElse(
                    triggerExecutionService.fetchJobTriggerExecution(Validate.notNull(job.getId(), "id")), null),
                job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecution> getWorkflowExecutions(
        Environment environment, Status jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate,
        Long integrationId, Long integrationInstanceConfigurationId, String workflowId, int pageNumber) {

        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            workflowIds.add(workflowId);
        } else if (integrationId != null) {
            Integration integration = integrationService.getIntegration(integrationId);

            workflowIds.addAll(getWorkflowIds(integration));
        } else {
            workflowIds.addAll(
                CollectionUtils.map(integrationFacade.getIntegrationWorkflows(), WorkflowDTO::id));
        }

        if (workflowIds.isEmpty()) {
            return Page.empty();
        } else {
            List<Long> integrationInstanceConfigurationIds = new ArrayList<>();

            if (integrationInstanceConfigurationId != null) {
                integrationInstanceConfigurationIds.add(integrationInstanceConfigurationId);
            } else if (environment != null) {
                integrationInstanceConfigurationIds.addAll(
                    integrationInstanceConfigurationService
                        .getIntegrationInstanceConfigurations(environment, null, null)
                        .stream()
                        .map(IntegrationInstanceConfiguration::getId)
                        .toList());
            }

            Page<Job> jobIdsPage = instanceJobService
                .getJobIds(
                    jobStatus, jobStartDate, jobEndDate, integrationInstanceConfigurationIds, AppType.EMBEDDED,
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

            return jobIdsPage.map(job -> {
                IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                    instanceJobService.getJobInstanceId(Validate.notNull(job.getId(), ""), AppType.EMBEDDED));

                return new WorkflowExecution(
                    Validate.notNull(job.getId(), "id"),
                    CollectionUtils.getFirst(
                        integrations,
                        integration -> CollectionUtils.contains(getWorkflowIds(integration), job.getWorkflowId())),
                    integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                        integrationInstance.getIntegrationInstanceConfigurationId()),
                    integrationInstance,
                    new JobDTO(job),
                    CollectionUtils.getFirst(
                        workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())),
                    getTriggerExecutionDTO(
                        integrationInstanceConfigurationId,
                        OptionalUtils.orElse(
                            triggerExecutionService.fetchJobTriggerExecution(
                                Validate.notNull(job.getId(), "id")),
                            null),
                        job));
            });
        }
    }

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return CollectionUtils.map(
            taskExecutionService.getJobTaskExecutions(jobId),
            taskExecution -> {
                Map<String, ?> context = taskFileStorage.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION));

                WorkflowTask workflowTask = taskExecution.getWorkflowTask();

                return new TaskExecutionDTO(
                    taskExecutionService.getTaskExecution(Validate.notNull(taskExecution.getId(), "id")),
                    getComponentDefinition(taskExecution.getType()), workflowTask.evaluateParameters(context),
                    taskExecution.getOutput() == null
                        ? null
                        : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
            });
    }

    private ComponentDefinition getComponentDefinition(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return componentDefinitionService.getComponentDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion());
    }

    private List<String> getWorkflowIds(Integration integration) {
        return OptionalUtils.mapOrElse(
            integration.fetchLastVersion(),
            lastVersion -> integrationWorkflowService.getWorkflowIds(integration.getId(), lastVersion),
            List.of());
    }

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number integrationInstanceId, TriggerExecution triggerExecution, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (integrationInstanceId != null) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceId.longValue(), job.getWorkflowId());

            triggerExecutionDTO = new TriggerExecutionDTO(
                getComponentDefinition(triggerExecution.getType()),
                integrationInstanceConfigurationWorkflow.getInputs(),
                triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()), triggerExecution);
        }

        return triggerExecutionDTO;
    }
}
