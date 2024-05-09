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

package com.bytechef.automation.workflow.execution.facade;

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
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.dto.WorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecution;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.constant.Type;
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
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class ProjectWorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobService jobService;
    private final InstanceJobService instanceJobService;
    private final ProjectFacade projectFacade;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobService jobService, InstanceJobService instanceJobService, ProjectFacade projectFacade,
        ProjectInstanceService projectInstanceService, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        TriggerExecutionService triggerExecutionService, TriggerFileStorage triggerFileStorage,
        WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobService = jobService;
        this.instanceJobService = instanceJobService;
        this.projectFacade = projectFacade;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
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
            job,
            job.getOutputs() == null
                ? null
                : taskFileStorage.readJobOutputs(job.getOutputs()),
            getJobTaskExecutions(id));
        Optional<Long> projectInstanceIdOptional = instanceJobService.fetchJobInstanceId(
            Validate.notNull(job.getId(), ""), Type.AUTOMATION);

        return new WorkflowExecution(
            jobDTO.id(), projectService.getWorkflowProject(jobDTO.workflowId()),
            OptionalUtils.map(projectInstanceIdOptional, projectInstanceService::getProjectInstance),
            jobDTO, workflowService.getWorkflow(jobDTO.workflowId()),
            getTriggerExecutionDTO(
                OptionalUtils.orElse(projectInstanceIdOptional, null),
                OptionalUtils.orElse(
                    triggerExecutionService.fetchJobTriggerExecution(Validate.notNull(job.getId(), "id")), null),
                job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecution> getWorkflowExecutions(
        Status jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, int pageNumber) {

        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            workflowIds.add(workflowId);
        } else if (projectId != null) {
            workflowIds.addAll(projectWorkflowService.getWorkflowIds(projectId));
        } else {
            workflowIds.addAll(
                CollectionUtils.map(projectFacade.getProjectWorkflows(), WorkflowDTO::id));
        }

        Page<WorkflowExecution> workflowExecutionPage;

        if (workflowIds.isEmpty()) {
            workflowExecutionPage = Page.empty();
        } else {
            Page<Job> jobsPage = instanceJobService
                .getJobIds(
                    jobStatus, jobStartDate, jobEndDate, projectInstanceId, Type.AUTOMATION, workflowIds,
                    pageNumber)
                .map(jobService::getJob);

            List<Project> projects = new ArrayList<>();

            if (projectId == null) {
                projects.addAll(projectService.getProjects());
            } else {
                projects.add(projectService.getProject(projectId));
            }

            List<Workflow> workflows = workflowService.getWorkflows(
                CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

            workflowExecutionPage = jobsPage.map(job -> new WorkflowExecution(
                Validate.notNull(job.getId(), "id"),
                CollectionUtils.getFirst(
                    projects,
                    project -> CollectionUtils.contains(
                        projectWorkflowService.getWorkflowIds(project.getId()), job.getWorkflowId())),
                OptionalUtils.map(
                    instanceJobService.fetchJobInstanceId(job.getId(), Type.AUTOMATION),
                    projectInstanceService::getProjectInstance),
                new JobDTO(job),
                CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())),
                getTriggerExecutionDTO(
                    projectInstanceId,
                    OptionalUtils.orElse(
                        triggerExecutionService.fetchJobTriggerExecution(Validate.notNull(job.getId(), "id")), null),
                    job)));
        }

        return workflowExecutionPage;
    }

    private ComponentDefinition getComponentDefinition(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return componentDefinitionService.getComponentDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion());
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

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number projectInstanceId, TriggerExecution triggerExecution, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (projectInstanceId != null && triggerExecution != null) {
            ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
                projectInstanceId.longValue(), job.getWorkflowId());

            triggerExecutionDTO = new TriggerExecutionDTO(
                getComponentDefinition(triggerExecution.getType()), projectInstanceWorkflow.getInputs(),
                triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()), triggerExecution);
        }

        return triggerExecutionDTO;
    }
}
