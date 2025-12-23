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

package com.bytechef.ee.embedded.workflow.execution.facade;

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
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
@ConditionalOnEEVersion
public class IntegrationWorkflowExecutionFacadeImpl implements IntegrationWorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final EnvironmentService environmentService;
    private final Evaluator evaluator;
    private final PrincipalJobService principalJobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowFacade integrationWorkflowFacade;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final JobService jobService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        EnvironmentService environmentService, Evaluator evaluator, PrincipalJobService principalJobService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, IntegrationWorkflowFacade integrationWorkflowFacade,
        IntegrationWorkflowService integrationWorkflowService, JobService jobService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, TriggerExecutionService triggerExecutionService,
        TriggerFileStorage triggerFileStorage, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.environmentService = environmentService;
        this.evaluator = evaluator;
        this.principalJobService = principalJobService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowFacade = integrationWorkflowFacade;
        this.integrationWorkflowService = integrationWorkflowService;
        this.jobService = jobService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerFileStorage = triggerFileStorage;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowExecutionDTO getWorkflowExecution(long id) {
        Job job = jobService.getJob(id);

        JobDTO jobDTO = new JobDTO(
            job, taskFileStorage.readJobOutputs(job.getOutputs()), getJobTaskExecutions(id));
        long integrationInstanceId = principalJobService.getJobPrincipalId(
            Validate.notNull(job.getId(), ""), PlatformType.EMBEDDED);

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        return new WorkflowExecutionDTO(
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
    public Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate,
        Long integrationId, Long integrationInstanceConfigurationId, String workflowId, int pageNumber) {

        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            workflowIds.add(workflowId);
        } else if (integrationId != null) {
            Integration integration = integrationService.getIntegration(integrationId);

            workflowIds.addAll(getWorkflowIds(integration));
        } else {
            workflowIds.addAll(
                CollectionUtils.map(
                    integrationWorkflowFacade.getIntegrationWorkflows(), IntegrationWorkflowDTO::getId));
        }

        if (workflowIds.isEmpty()) {
            return Page.empty();
        } else {
            List<Long> integrationInstanceConfigurationIds = new ArrayList<>();

            if (integrationInstanceConfigurationId != null) {
                integrationInstanceConfigurationIds.add(integrationInstanceConfigurationId);
            } else {
                Environment environment =
                    environmentId == null ? null : environmentService.getEnvironment(environmentId);

                integrationInstanceConfigurationIds.addAll(
                    integrationInstanceConfigurationService
                        .getIntegrationInstanceConfigurations(environment, null, null)
                        .stream()
                        .map(IntegrationInstanceConfiguration::getId)
                        .toList());
            }

            if (integrationInstanceConfigurationIds.isEmpty()) {
                return Page.empty();
            } else {
                Page<Job> jobsPage = principalJobService
                    .getJobIds(
                        jobStatus, jobStartDate, jobEndDate, integrationInstanceConfigurationIds, PlatformType.EMBEDDED,
                        workflowIds, pageNumber)
                    .map(jobService::getJob);

                List<Integration> integrations = new ArrayList<>();

                if (integrationId == null) {
                    integrations.addAll(integrationService.getIntegrations());
                } else {
                    integrations.add(integrationService.getIntegration(integrationId));
                }

                List<Workflow> workflows = workflowService.getWorkflows(
                    CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

                return jobsPage.map(job -> {
                    IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                        principalJobService.getJobPrincipalId(Validate.notNull(job.getId(), ""),
                            PlatformType.EMBEDDED));

                    return new WorkflowExecutionDTO(
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
    }

    private DefinitionResult getDefinition(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        if (componentDefinitionService.hasComponentDefinition(
            workflowNodeType.name(), workflowNodeType.version())) {

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            return new DefinitionResult(componentDefinition.getTitle(), componentDefinition.getIcon());
        }

        TaskDispatcherDefinition taskDispatcherDefinition = taskDispatcherDefinitionService.getTaskDispatcherDefinition(
            workflowNodeType.name(), workflowNodeType.version());

        return new DefinitionResult(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getIcon());
    }

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return CollectionUtils.map(
            taskExecutionService.getJobTaskExecutions(jobId),
            taskExecution -> {
                Map<String, ?> context = taskFileStorage.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION));

                DefinitionResult definitionResult = getDefinition(taskExecution.getType());
                WorkflowTask workflowTask = taskExecution.getWorkflowTask();
                Object output = taskExecution.getOutput() == null
                    ? null
                    : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput());

                return new TaskExecutionDTO(
                    taskExecutionService.getTaskExecution(Validate.notNull(taskExecution.getId(), "id")),
                    definitionResult.title(), definitionResult.icon(),
                    workflowTask.evaluateParameters(context, evaluator), output);
            });
    }

    private List<String> getWorkflowIds(Integration integration) {
        return integrationWorkflowService.getWorkflowIds(integration.getId(), integration.getLastIntegrationVersion());
    }

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number integrationInstanceId, TriggerExecution triggerExecution, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (integrationInstanceId != null) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceId.longValue(), job.getWorkflowId());

            DefinitionResult definitionResult = getDefinition(triggerExecution.getType());

            triggerExecutionDTO = new TriggerExecutionDTO(
                triggerExecution, definitionResult.title(), definitionResult.icon(),
                integrationInstanceConfigurationWorkflow.getInputs(),
                triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()));
        }

        return triggerExecutionDTO;
    }

    record DefinitionResult(String title, String icon) {
    }
}
