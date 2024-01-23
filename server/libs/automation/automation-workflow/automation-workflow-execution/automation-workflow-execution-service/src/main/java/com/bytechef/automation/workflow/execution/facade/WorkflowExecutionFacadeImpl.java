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

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecution;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.registry.component.WorkflowNodeType;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.component.registry.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.constant.Type;
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
public class WorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobService jobService;
    private final InstanceJobService instanceJobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobService jobService, InstanceJobService instanceJobService, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        TriggerExecutionService triggerExecutionService, TriggerFileStorage triggerFileStorage,
        WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobService = jobService;
        this.instanceJobService = instanceJobService;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
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

        JobDTO jobDTO = new JobDTO(
            job, taskFileStorage.readJobOutputs(job.getOutputs()), getJobTaskExecutions(id));
        Optional<Long> projectInstanceIdOptional = instanceJobService.fetchJobInstanceId(
            Validate.notNull(job.getId(), ""), Type.AUTOMATION);

        return new WorkflowExecution(
            jobDTO.id(), projectService.getWorkflowProject(jobDTO.workflowId()),
            OptionalUtils.map(projectInstanceIdOptional, projectInstanceService::getProjectInstance),
            jobDTO, workflowService.getWorkflow(jobDTO.workflowId()),
            getTriggerExecutionDTO(
                OptionalUtils.orElse(projectInstanceIdOptional, null),
                triggerExecutionService.getJobTriggerExecution(Validate.notNull(job.getId(), "id")), job));
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
                CollectionUtils.map(workflowService.getWorkflows(Type.AUTOMATION.getId()), Workflow::getId));
        }

        if (workflowIds.isEmpty()) {
            return Page.empty();
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

            return jobsPage.map(job -> new WorkflowExecution(
                Validate.notNull(job.getId(), "id"),
                CollectionUtils.getFirst(
                    projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId())),
                OptionalUtils.map(
                    instanceJobService.fetchJobInstanceId(job.getId(), Type.AUTOMATION),
                    projectInstanceService::getProjectInstance),
                new JobDTO(job),
                CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())),
                getTriggerExecutionDTO(
                    projectInstanceId,
                    triggerExecutionService.getJobTriggerExecution(Validate.notNull(job.getId(), "id")), job)));
        }
    }

    private ComponentDefinition getComponentDefinition(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return componentDefinitionService.getComponentDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion());
    }

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return CollectionUtils.map(
            taskExecutionService.getJobTaskExecutions(jobId),
            taskExecution -> new TaskExecutionDTO(
                getComponentDefinition(taskExecution.getType()),
                taskFileStorage.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)),
                taskExecution.getOutput() == null
                    ? null
                    : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()),
                taskExecutionService.getTaskExecution(taskExecution.getId())));
    }

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number projectInstanceId, TriggerExecution triggerExecution, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (projectInstanceId != null) {
            ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
                projectInstanceId.longValue(), job.getWorkflowId());

            triggerExecutionDTO = new TriggerExecutionDTO(
                getComponentDefinition(triggerExecution.getType()), projectInstanceWorkflow.getInputs(),
                triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()), triggerExecution);
        }

        return triggerExecutionDTO;
    }

    private static void updateOperations(
        Map.Entry<String, Map<String, Object>> connectionEntry, List<Map<String, Object>> workflowOperations) {

        for (int i = 0; i < workflowOperations.size(); i++) {
            Map<String, Object> workflowTask = workflowOperations.get(i);

            if (Objects.equals(MapUtils.getString(workflowTask, WorkflowConstants.NAME), connectionEntry.getKey())) {
                workflowOperations.set(
                    i,
                    MapUtils.concat(workflowTask, Map.of(WorkflowConnection.CONNECTIONS, connectionEntry.getValue())));
            }
        }
    }
}
