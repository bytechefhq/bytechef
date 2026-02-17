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
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class ProjectWorkflowExecutionFacadeImpl implements ProjectWorkflowExecutionFacade {

    private static final Logger logger = LoggerFactory.getLogger(ProjectWorkflowExecutionFacadeImpl.class);

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final Evaluator evaluator;
    private final EnvironmentService environmentService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectFacade projectFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerFileStorage triggerFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService, Evaluator evaluator,
        EnvironmentService environmentService, JobService jobService, PrincipalJobService principalJobService,
        ProjectFacade projectFacade, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, TriggerExecutionService triggerExecutionService,
        TriggerFileStorage triggerFileStorage, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.evaluator = evaluator;
        this.environmentService = environmentService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectFacade = projectFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
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

        Map<String, ?> outputs = job.getOutputs() == null
            ? null
            : taskFileStorage.readJobOutputs(job.getOutputs());

        JobDTO jobDTO = new JobDTO(job, outputs, getJobTaskExecutions(id));

        Optional<Long> projectDeploymentIdOptional = principalJobService.fetchJobPrincipalId(
            Validate.notNull(job.getId(), ""), PlatformType.AUTOMATION);

        return new WorkflowExecutionDTO(
            jobDTO.id(), projectService.getWorkflowProject(jobDTO.workflowId()),
            projectDeploymentIdOptional
                .map(projectDeploymentService::getProjectDeployment)
                .orElse(null),
            jobDTO, workflowService.getWorkflow(jobDTO.workflowId()),
            getTriggerExecutionDTO(
                projectDeploymentIdOptional.orElse(null),
                triggerExecutionService.fetchJobTriggerExecution(Validate.notNull(job.getId(), "id"))
                    .orElse(null),
                job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Boolean embedded, Long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate,
        Long projectId, Long projectDeploymentId, String workflowId, long workspaceId, int pageNumber) {

        List<String> workflowIds = new ArrayList<>();

        if (workflowId != null) {
            workflowIds.add(workflowId);
        } else if (projectId != null) {
            workflowIds.addAll(projectWorkflowService.getProjectWorkflowIds(projectId));
        } else {
            workflowIds.addAll(
                CollectionUtils.map(
                    projectFacade.getWorkspaceProjectWorkflows(workspaceId), ProjectWorkflowDTO::getId));
        }

        Page<WorkflowExecutionDTO> workflowExecutionPage;

        if (workflowIds.isEmpty()) {
            workflowExecutionPage = Page.empty();
        } else {
            List<Long> projectDeploymentIds = new ArrayList<>();

            if (projectDeploymentId != null) {
                projectDeploymentIds.add(projectDeploymentId);
            } else {
                Environment environment =
                    environmentId == null ? null : environmentService.getEnvironment(environmentId);

                projectDeploymentIds.addAll(
                    projectDeploymentService.getProjectDeployments(embedded, environment, null, null, null)
                        .stream()
                        .map(ProjectDeployment::getId)
                        .toList());
            }

            if (projectDeploymentIds.isEmpty()) {
                workflowExecutionPage = Page.empty();
            } else {
                Page<Job> jobsPage = principalJobService
                    .getJobIds(
                        jobStatus, jobStartDate, jobEndDate, projectDeploymentIds, PlatformType.AUTOMATION, workflowIds,
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

                Map<Long, List<String>> projectWorkflowIdsMap = projects.stream()
                    .collect(Collectors.toMap(
                        project -> Validate.notNull(project.getId(), "id"),
                        project -> projectWorkflowService.getProjectWorkflowIds(project.getId())));

                List<WorkflowExecutionDTO> workflowExecutionDTOs = new ArrayList<>();

                for (Job job : jobsPage) {
                    Optional<Workflow> workflowOptional = CollectionUtils.findFirst(
                        workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId()));

                    if (workflowOptional.isEmpty()) {
                        if (logger.isWarnEnabled()) {
                            logger.warn(
                                "Skipping job id={}: workflow '{}' not found", job.getId(), job.getWorkflowId());
                        }

                        continue;
                    }

                    Optional<Project> projectOptional = CollectionUtils.findFirst(
                        projects,
                        project -> CollectionUtils.contains(
                            projectWorkflowIdsMap.get(project.getId()), job.getWorkflowId()));

                    if (projectOptional.isEmpty()) {
                        if (logger.isWarnEnabled()) {
                            logger.warn(
                                "Skipping job id={}: no project found for workflow '{}'",
                                job.getId(), job.getWorkflowId());
                        }

                        continue;
                    }

                    Optional<Long> jobDeploymentIdOptional = principalJobService.fetchJobPrincipalId(
                        job.getId(), PlatformType.AUTOMATION);

                    ProjectDeployment jobProjectDeployment = jobDeploymentIdOptional
                        .map(projectDeploymentService::getProjectDeployment)
                        .orElse(null);

                    TriggerExecution triggerExecution = jobDeploymentIdOptional.isPresent()
                        ? triggerExecutionService.fetchJobTriggerExecution(
                            Validate.notNull(job.getId(), "id"))
                            .orElse(null)
                        : null;

                    workflowExecutionDTOs.add(new WorkflowExecutionDTO(
                        Validate.notNull(job.getId(), "id"),
                        projectOptional.get(),
                        jobProjectDeployment,
                        new JobDTO(job),
                        workflowOptional.get(),
                        getTriggerExecutionDTO(
                            jobDeploymentIdOptional.orElse(null),
                            triggerExecution,
                            job)));
                }

                workflowExecutionPage = new PageImpl<>(
                    workflowExecutionDTOs, jobsPage.getPageable(), jobsPage.getTotalElements());
            }
        }

        return workflowExecutionPage;
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

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number projectDeploymentId, TriggerExecution triggerExecution, Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (projectDeploymentId != null && triggerExecution != null) {
            DefinitionResult definitionResult = getDefinition(triggerExecution.getType());

            WorkflowTrigger workflowTrigger = triggerExecution.getWorkflowTrigger();

            Map<String, ?> workflowTriggerParameters = workflowTrigger.getParameters();

            Map<String, Object> inputs = job.getInputs()
                .entrySet()
                .stream()
                .filter(input -> !workflowTriggerParameters.containsKey(input.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            triggerExecutionDTO = new TriggerExecutionDTO(
                triggerExecution, definitionResult.title(), definitionResult.icon(),
                workflowTrigger.evaluateParameters(inputs, evaluator),
                triggerExecution.getOutput() == null ? null
                    : triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()));
        }

        return triggerExecutionDTO;
    }

    record DefinitionResult(String title, String icon) {
    }
}
