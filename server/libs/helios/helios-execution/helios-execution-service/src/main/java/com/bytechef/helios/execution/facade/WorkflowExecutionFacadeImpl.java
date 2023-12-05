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

package com.bytechef.helios.execution.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.helios.execution.dto.TestConnectionDTO;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.helios.execution.dto.WorkflowExecution;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.dto.JobDTO;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import com.bytechef.hermes.execution.dto.TriggerExecutionDTO;
import com.bytechef.hermes.execution.service.InstanceJobService;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import com.bytechef.hermes.file.storage.TriggerFileStorage;
import com.bytechef.hermes.test.executor.JobTestExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final JobService jobService;
    private final JobTestExecutor jobTestExecutor;
    private final InstanceJobService instanceJobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectService projectService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        InstanceAccessorRegistry instanceAccessorRegistry, JobService jobService, JobTestExecutor jobTestExecutor,
        InstanceJobService instanceJobService, ProjectInstanceService projectInstanceService,
        ProjectService projectService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, TriggerExecutionService triggerExecutionService,
        TriggerFileStorage triggerFileStorage, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.jobService = jobService;
        this.jobTestExecutor = jobTestExecutor;
        this.instanceJobService = instanceJobService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
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

        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(ProjectConstants.PROJECT_TYPE);
        JobDTO jobDTO = new JobDTO(
            job, taskFileStorage.readJobOutputs(job.getOutputs()), getJobTaskExecutions(id));
        Optional<Long> projectInstanceIdOptional = instanceJobService.fetchJobInstanceId(
            Validate.notNull(job.getId(), ""), ProjectConstants.PROJECT_TYPE);

        return new WorkflowExecution(
            Validate.notNull(jobDTO.id(), "id"),
            projectService.getWorkflowProject(jobDTO.workflowId()),
            OptionalUtils.mapOrElse(projectInstanceIdOptional, projectInstanceService::getProjectInstance, null),
            jobDTO, workflowService.getWorkflow(jobDTO.workflowId()),
            getTriggerExecutionDTO(
                OptionalUtils.orElse(projectInstanceIdOptional, null),
                triggerExecutionService.getJobTriggerExecution(
                    Validate.notNull(job.getId(), "id")),
                instanceAccessor, job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecution> getWorkflowExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, int pageNumber) {

        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            workflowIds.add(workflowId);
        } else if (projectId != null) {
            Project project = projectService.getProject(projectId);

            workflowIds.addAll(project.getWorkflowIds());
        } else {
            workflowIds.addAll(
                CollectionUtils.map(workflowService.getWorkflows(ProjectConstants.PROJECT_TYPE), Workflow::getId));
        }

        Page<Job> jobsPage;

        if (workflowIds.isEmpty()) {
            jobsPage = Page.empty();
        } else {
            jobsPage = instanceJobService
                .getJobIds(
                    jobStatus, jobStartDate, jobEndDate, projectInstanceId, ProjectConstants.PROJECT_TYPE, workflowIds,
                    pageNumber)
                .map(jobService::getJob);
        }

        List<Project> projects = new ArrayList<>();

        if (projectId == null) {
            projects.addAll(projectService.getProjects());
        } else {
            projects.add(projectService.getProject(projectId));
        }

        List<Workflow> workflows = workflowService.getWorkflows(
            CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));
        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(ProjectConstants.PROJECT_TYPE);

        return jobsPage.map(job -> new WorkflowExecution(
            Validate.notNull(job.getId(), "id"),
            CollectionUtils.findFirstOrElse(
                projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId()), null),
            OptionalUtils.mapOrElse(
                instanceJobService.fetchJobInstanceId(job.getId(), ProjectConstants.PROJECT_TYPE),
                projectInstanceService::getProjectInstance, null),
            new JobDTO(job, Map.of(), List.of()),
            CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())),
            getTriggerExecutionDTO(
                projectInstanceId,
                triggerExecutionService.getJobTriggerExecution(Validate.notNull(job.getId(), "id")),
                instanceAccessor, job)));
    }

    @Override
    public WorkflowExecution testWorkflow(JobParameters jobParameters) {

//        TODO triggerTestExecutor

        JobDTO job = jobTestExecutor.execute(new JobParameters(workflowId, inputs));

        return new WorkflowExecution(
            job.id(), null, null, job, workflowService.getWorkflow(jobParameters.getWorkflowId()), null);
    }

    private ComponentDefinition getComponentDefinition(String type) {
        OperationType operationType = OperationType.ofType(type);

        return componentDefinitionService.getComponentDefinition(
            operationType.componentName(), operationType.componentVersion());
    }

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .map(taskExecution -> new TaskExecutionDTO(
                getComponentDefinition(taskExecution.getType()),
                taskFileStorage.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)),
                taskExecution.getOutput() == null
                    ? null
                    : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()),
                taskExecutionService.getTaskExecution(taskExecution.getId())))
            .toList();
    }

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number projectInstanceId, TriggerExecution triggerExecution, InstanceAccessor instanceAccessor, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (projectInstanceId != null) {
            triggerExecutionDTO = new TriggerExecutionDTO(
                getComponentDefinition(triggerExecution.getType()), instanceAccessor.getInputMap(
                    projectInstanceId.longValue(), job.getWorkflowId()),
                triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()), triggerExecution);
        }
        return triggerExecutionDTO;
    }
}
