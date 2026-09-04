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

import static com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO.buildHierarchy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
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
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
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

    private static final Logger log = LoggerFactory.getLogger(ProjectWorkflowExecutionFacadeImpl.class);

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final Evaluator evaluator;
    private final EnvironmentService environmentService;
    private final WorkflowExecutionRowService workflowExecutionRowService;
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
        EnvironmentService environmentService, WorkflowExecutionRowService workflowExecutionRowService,
        JobService jobService,
        PrincipalJobService principalJobService,
        ProjectFacade projectFacade, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, TriggerExecutionService triggerExecutionService,
        TriggerFileStorage triggerFileStorage, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.evaluator = evaluator;
        this.environmentService = environmentService;
        this.workflowExecutionRowService = workflowExecutionRowService;
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

        JobDTO jobDTO = new JobDTO(job, outputs, getJobTaskExecutions(id, false));

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
    public WorkflowExecutionDTO getTriggerExecutionWorkflowExecution(long triggerExecutionId) {
        TriggerExecution triggerExecution = triggerExecutionService.getTriggerExecution(triggerExecutionId);

        WorkflowExecutionDTO workflowExecutionDTO = buildTriggerWorkflowExecutionDTOs(List.of(triggerExecution))
            .get(triggerExecutionId);

        if (workflowExecutionDTO == null) {
            throw new NoSuchElementException(
                "Trigger execution " + triggerExecutionId + " does not belong to a deployed workflow");
        }

        return workflowExecutionDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskExecutionDTO getWorkflowExecutionTaskExecution(long id, long taskExecutionId) {
        TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        validateTaskExecutionBelongsToJob(id, taskExecution, taskExecutionId);

        return toTaskExecutionDTO(taskExecution, null, true);
    }

    private void validateTaskExecutionBelongsToJob(long id, TaskExecution taskExecution, long taskExecutionId) {
        long currentJobId = Objects.requireNonNull(taskExecution.getJobId());

        while (currentJobId != id) {
            Job job = jobService.getJob(currentJobId);

            Long parentTaskExecutionId = job.getParentTaskExecutionId();

            if (parentTaskExecutionId == null) {
                throw new IllegalArgumentException(
                    "Task execution " + taskExecutionId + " does not belong to workflow execution " + id);
            }

            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentTaskExecutionId);

            currentJobId = Objects.requireNonNull(parentTaskExecution.getJobId());
        }
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
                Page<WorkflowExecutionRowDTO> rowsPage = workflowExecutionRowService.getWorkflowExecutionRows(
                    jobStatus, jobStartDate, jobEndDate, projectDeploymentIds, PlatformType.AUTOMATION, workflowIds,
                    true, getFailedTriggerWorkflowExecutionIds(jobStatus, projectDeploymentIds, workflowIds),
                    pageNumber);

                List<Long> jobIds = rowsPage.getContent()
                    .stream()
                    .filter(row -> row.kind() == WorkflowExecutionRowDTO.Kind.JOB)
                    .map(WorkflowExecutionRowDTO::id)
                    .toList();

                List<Job> jobs = jobService.getJobs(jobIds);

                Map<Long, Job> jobMap = jobs.stream()
                    .collect(Collectors.toMap(
                        job -> Validate.notNull(job.getId(), "id"), Function.identity()));

                List<Project> projects = new ArrayList<>();

                if (projectId == null) {
                    projects.addAll(projectService.getProjects());
                } else {
                    projects.add(projectService.getProject(projectId));
                }

                List<Workflow> workflows = workflowService.getWorkflows(
                    CollectionUtils.map(jobs, Job::getWorkflowId));

                List<Long> projectIds = projects.stream()
                    .map(project -> Validate.notNull(project.getId(), "id"))
                    .toList();

                Map<Long, List<String>> projectWorkflowIdsMap = projectWorkflowService.getProjectWorkflows(projectIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                        ProjectWorkflow::getProjectId,
                        Collectors.mapping(ProjectWorkflow::getWorkflowId, Collectors.toList())));

                List<PrincipalJob> principalJobs =
                    principalJobService.getPrincipalJobs(jobIds, PlatformType.AUTOMATION);

                Map<Long, Long> jobToPrincipalMap = principalJobs.stream()
                    .collect(Collectors.toMap(PrincipalJob::getJobId, PrincipalJob::getPrincipalId));

                List<Long> principalIds = principalJobs.stream()
                    .map(PrincipalJob::getPrincipalId)
                    .distinct()
                    .toList();

                Map<Long, ProjectDeployment> deploymentMap =
                    projectDeploymentService.getProjectDeployments(principalIds)
                        .stream()
                        .collect(Collectors.toMap(
                            deployment -> Validate.notNull(deployment.getId(), "id"), Function.identity()));

                List<TriggerExecution> triggerExecutions =
                    triggerExecutionService.getJobTriggerExecutions(jobIds);

                Map<Long, TriggerExecution> triggerExecutionByJobIdMap = new HashMap<>();

                for (TriggerExecution triggerExecution : triggerExecutions) {
                    for (Long triggerJobId : triggerExecution.getJobIds()) {
                        triggerExecutionByJobIdMap.putIfAbsent(triggerJobId, triggerExecution);
                    }
                }

                Map<Long, WorkflowExecutionDTO> jobWorkflowExecutionDTOMap = buildWorkflowExecutionDTOs(
                    jobIds, jobMap, workflows, projects, projectWorkflowIdsMap, jobToPrincipalMap, deploymentMap,
                    triggerExecutionByJobIdMap)
                        .stream()
                        .collect(Collectors.toMap(WorkflowExecutionDTO::id, Function.identity()));

                Map<Long, WorkflowExecutionDTO> triggerWorkflowExecutionDTOMap = buildTriggerWorkflowExecutionDTOs(
                    triggerExecutionService.getTriggerExecutions(
                        rowsPage.getContent()
                            .stream()
                            .filter(row -> row.kind() == WorkflowExecutionRowDTO.Kind.TRIGGER_EXECUTION)
                            .map(WorkflowExecutionRowDTO::id)
                            .toList()));

                List<WorkflowExecutionDTO> workflowExecutionDTOs = rowsPage.getContent()
                    .stream()
                    .map(row -> row.kind() == WorkflowExecutionRowDTO.Kind.JOB
                        ? jobWorkflowExecutionDTOMap.get(row.id())
                        : triggerWorkflowExecutionDTOMap.get(row.id()))
                    .filter(Objects::nonNull)
                    .toList();

                workflowExecutionPage = new PageImpl<>(
                    workflowExecutionDTOs, rowsPage.getPageable(), rowsPage.getTotalElements());
            }
        }

        return workflowExecutionPage;
    }

    private List<WorkflowExecutionDTO> buildWorkflowExecutionDTOs(
        List<Long> jobIds, Map<Long, Job> jobMap, List<Workflow> workflows, List<Project> projects,
        Map<Long, List<String>> projectWorkflowIdsMap, Map<Long, Long> jobToPrincipalMap,
        Map<Long, ProjectDeployment> deploymentMap, Map<Long, TriggerExecution> triggerExecutionByJobIdMap) {

        List<WorkflowExecutionDTO> workflowExecutionDTOs = new ArrayList<>();

        for (Long jobId : jobIds) {
            Job job = jobMap.get(jobId);

            if (job == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Skipping job id={}: job not found", jobId);
                }

                continue;
            }

            Optional<Workflow> workflowOptional = CollectionUtils.findFirst(
                workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId()));

            if (workflowOptional.isEmpty()) {
                if (log.isWarnEnabled()) {
                    log.warn(
                        "Skipping job id={}: workflow '{}' not found", job.getId(), job.getWorkflowId());
                }

                continue;
            }

            Optional<Project> projectOptional = CollectionUtils.findFirst(
                projects,
                project -> CollectionUtils.contains(
                    projectWorkflowIdsMap.getOrDefault(project.getId(), List.of()),
                    job.getWorkflowId()));

            if (projectOptional.isEmpty()) {
                if (log.isWarnEnabled()) {
                    log.warn(
                        "Skipping job id={}: no project found for workflow '{}'",
                        job.getId(), job.getWorkflowId());
                }

                continue;
            }

            Long deploymentId = jobToPrincipalMap.get(jobId);

            ProjectDeployment jobProjectDeployment =
                deploymentId == null ? null : deploymentMap.get(deploymentId);

            TriggerExecution triggerExecution =
                deploymentId == null ? null : triggerExecutionByJobIdMap.get(jobId);

            Map<String, ?> outputs = job.getOutputs() == null
                ? null
                : taskFileStorage.readJobOutputs(job.getOutputs());

            workflowExecutionDTOs.add(new WorkflowExecutionDTO(
                Validate.notNull(job.getId(), "id"),
                projectOptional.get(),
                jobProjectDeployment,
                new JobDTO(job, outputs, getSubflowJobTaskExecutions(job.getId())),
                workflowOptional.get(),
                getTriggerExecutionDTO(deploymentId, triggerExecution, job)));
        }

        return workflowExecutionDTOs;
    }

    /**
     * The encoded workflow execution ids under which a trigger of any workflow in scope, deployed by any deployment in
     * scope, records its executions. Only the project version a deployment points at counts, and every id appears once,
     * since the ids are bound one JDBC parameter each. Empty when the status filter rules failed triggers out, which
     * keeps them off the page.
     */
    private List<String> getFailedTriggerWorkflowExecutionIds(
        Status jobStatus, List<Long> projectDeploymentIds, List<String> workflowIds) {

        if (jobStatus != null && jobStatus != Status.FAILED) {
            return List.of();
        }

        Map<Long, ProjectDeployment> deploymentMap = projectDeploymentService.getProjectDeployments(
            projectDeploymentIds)
            .stream()
            .collect(Collectors.toMap(deployment -> Validate.notNull(deployment.getId(), "id"), Function.identity()));

        Map<String, Workflow> workflowMap = workflowService.getWorkflows(workflowIds)
            .stream()
            .collect(Collectors.toMap(Workflow::getId, Function.identity()));

        Set<String> workflowExecutionIds = new LinkedHashSet<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(
            deploymentMap.values()
                .stream()
                .map(ProjectDeployment::getProjectId)
                .distinct()
                .toList())) {

            Workflow workflow = workflowMap.get(projectWorkflow.getWorkflowId());

            if (workflow == null) {
                continue;
            }

            for (ProjectDeployment deployment : deploymentMap.values()) {
                if (!Objects.equals(deployment.getProjectId(), projectWorkflow.getProjectId()) ||
                    deployment.getProjectVersion() != projectWorkflow.getProjectVersion()) {

                    continue;
                }

                for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
                    workflowExecutionIds.add(
                        WorkflowExecutionId.of(
                            PlatformType.AUTOMATION, Validate.notNull(deployment.getId(), "id"),
                            projectWorkflow.getUuidAsString(), workflowTrigger.getName())
                            .toString());
                }
            }
        }

        return new ArrayList<>(workflowExecutionIds);
    }

    /**
     * Execution rows for trigger executions that produced no job, keyed by trigger execution id. The deployment,
     * project and workflow come from the trigger execution's workflow execution id.
     */
    private Map<Long, WorkflowExecutionDTO>
        buildTriggerWorkflowExecutionDTOs(List<TriggerExecution> triggerExecutions) {
        Map<Long, WorkflowExecutionDTO> workflowExecutionDTOMap = new HashMap<>();

        for (TriggerExecution triggerExecution : triggerExecutions) {
            WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

            long projectDeploymentId = workflowExecutionId.getJobPrincipalId();

            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

            Optional<String> workflowIdOptional = projectWorkflowService.fetchProjectWorkflowWorkflowId(
                projectDeploymentId, workflowExecutionId.getWorkflowUuid());

            if (workflowIdOptional.isEmpty()) {
                if (log.isWarnEnabled()) {
                    log.warn(
                        "Skipping trigger execution id={}: workflow '{}' not found for deployment {}",
                        triggerExecution.getId(), workflowExecutionId.getWorkflowUuid(), projectDeploymentId);
                }

                continue;
            }

            workflowExecutionDTOMap.put(
                Validate.notNull(triggerExecution.getId(), "id"),
                new WorkflowExecutionDTO(
                    triggerExecution.getId(), projectService.getProject(projectDeployment.getProjectId()),
                    projectDeployment, null, workflowService.getWorkflow(workflowIdOptional.get()),
                    getTriggerExecutionDTO(projectDeploymentId, triggerExecution, null)));
        }

        return workflowExecutionDTOMap;
    }

    private DefinitionResult getDefinition(String type, Map<String, DefinitionResult> definitionResultCache) {
        return definitionResultCache.computeIfAbsent(type, this::resolveDefinition);
    }

    private DefinitionResult resolveDefinition(String type) {
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

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId, boolean includeTaskData) {
        List<Long> childJobIds = jobService.getChildJobIds(jobId);
        Map<Long, Job> childJobMap = jobService.getJobs(childJobIds)
            .stream()
            .collect(Collectors.toMap(Job::getParentTaskExecutionId, Function.identity()));

        Map<String, DefinitionResult> definitionResultCache = new HashMap<>();

        List<TaskExecutionDTO> taskExecutionDTOs = CollectionUtils.map(
            taskExecutionService.getJobTaskExecutions(jobId),
            taskExecution -> toTaskExecutionDTO(
                taskExecution, toJobDTO(childJobMap.get(taskExecution.getId()), includeTaskData), includeTaskData,
                definitionResultCache));

        return buildHierarchy(taskExecutionDTOs);
    }

    List<TaskExecutionDTO> getSubflowJobTaskExecutions(long jobId) {
        List<Long> childJobIds = jobService.getChildJobIds(jobId);

        if (childJobIds.isEmpty()) {
            return List.of();
        }

        return jobService.getJobs(childJobIds)
            .stream()
            .filter(childJob -> childJob.getParentTaskExecutionId() != null)
            .map(childJob -> toTaskExecutionDTO(
                taskExecutionService.getTaskExecution(childJob.getParentTaskExecutionId()), toSubflowJobDTO(childJob),
                false))
            .toList();
    }

    private TriggerExecutionDTO getTriggerExecutionDTO(
        Number projectDeploymentId, TriggerExecution triggerExecution, @Nullable Job job) {

        TriggerExecutionDTO triggerExecutionDTO = null;

        if (projectDeploymentId != null && triggerExecution != null) {
            DefinitionResult definitionResult = resolveDefinition(triggerExecution.getType());

            WorkflowTrigger workflowTrigger = triggerExecution.getWorkflowTrigger();

            Map<String, ?> workflowTriggerParameters = workflowTrigger.getParameters();

            Map<String, ?> input;

            if (job == null) {
                input = workflowTriggerParameters;
            } else {
                Map<String, Object> inputs = job.getInputs()
                    .entrySet()
                    .stream()
                    .filter(entry -> !workflowTriggerParameters.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                input = workflowTrigger.evaluateParameters(inputs, evaluator);
            }

            triggerExecutionDTO = new TriggerExecutionDTO(
                triggerExecution, definitionResult.title(), definitionResult.icon(), input,
                triggerExecution.getOutput() == null ? null
                    : triggerFileStorage.readTriggerExecutionOutput(triggerExecution.getOutput()));
        }

        return triggerExecutionDTO;
    }

    private JobDTO toJobDTO(Job job, boolean includeTaskData) {
        if (job == null) {
            return null;
        }

        Map<String, ?> outputs = includeTaskData
            ? CollectionUtils.asMap(job.getOutputs(), taskFileStorage::readJobOutputs)
            : null;

        return new JobDTO(job, outputs, getJobTaskExecutions(Objects.requireNonNull(job.getId()), includeTaskData));
    }

    private JobDTO toSubflowJobDTO(Job job) {
        return new JobDTO(job, null, getSubflowJobTaskExecutions(Objects.requireNonNull(job.getId())));
    }

    TaskExecutionDTO toTaskExecutionDTO(TaskExecution taskExecution, JobDTO childJob, boolean includeTaskData) {
        return toTaskExecutionDTO(taskExecution, childJob, includeTaskData, new HashMap<>());
    }

    private TaskExecutionDTO toTaskExecutionDTO(
        TaskExecution taskExecution, JobDTO childJob, boolean includeTaskData,
        Map<String, DefinitionResult> definitionResultCache) {

        DefinitionResult definitionResult = getDefinition(taskExecution.getType(), definitionResultCache);

        Map<String, ?> input = null;
        Object output = null;

        if (includeTaskData) {
            Map<String, ?> context = taskFileStorage.readContextValue(
                contextService.peek(Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION));
            WorkflowTask workflowTask = taskExecution.getWorkflowTask();

            input = workflowTask.evaluateParameters(context, evaluator);
            output = taskExecution.getOutput() == null
                ? null
                : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput());
        }

        return new TaskExecutionDTO(
            taskExecution, definitionResult.title(), definitionResult.icon(), input, output, childJob);
    }

    record DefinitionResult(String title, String icon) {
    }
}
