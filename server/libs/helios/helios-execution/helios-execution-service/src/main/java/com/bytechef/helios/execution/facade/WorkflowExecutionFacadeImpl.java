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
import com.bytechef.helios.execution.dto.WorkflowExecutionDTO;
import com.bytechef.hermes.component.registry.ComponentOperation;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerExecutionService triggerExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        InstanceAccessorRegistry instanceAccessorRegistry, JobService jobService, JobTestExecutor jobTestExecutor,
        InstanceJobService instanceJobService, ProjectInstanceService projectInstanceService,
        ProjectService projectService, TaskExecutionService taskExecutionService,
        @Qualifier("workflowAsyncTaskFileStorageFacade") TaskFileStorage taskFileStorage,
        @Qualifier("workflowAsyncTriggerFileStorageFacade") TriggerFileStorage triggerFileStorage,
        TriggerExecutionService triggerExecutionService, WorkflowService workflowService) {

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
        this.triggerFileStorage = triggerFileStorage;
        this.triggerExecutionService = triggerExecutionService;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowExecutionDTO getWorkflowExecution(long id) {
        Job job = jobService.getJob(id);

        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(ProjectConstants.PROJECT_TYPE);
        JobDTO jobDTO = new JobDTO(
            job, taskFileStorage.readJobOutputs(job.getOutputs()), getJobTaskExecutions(id));
        Optional<Long> projectInstanceIdOptional = instanceJobService.fetchJobInstanceId(
            Validate.notNull(job.getId(), ""), ProjectConstants.PROJECT_TYPE);

        return new WorkflowExecutionDTO(
            Validate.notNull(jobDTO.id(), "id"),
            projectService.getWorkflowProject(jobDTO.workflowId()), OptionalUtils.mapOrElse(
                projectInstanceIdOptional,
                projectInstanceService::getProjectInstance, null),
            workflowService.getWorkflow(jobDTO.workflowId()), jobDTO,
            getTriggerExecutionDTO(
                OptionalUtils.orElse(projectInstanceIdOptional, null),
                triggerExecutionService.getJobTriggerExecution(
                    Validate.notNull(job.getId(), "id")),
                instanceAccessor, job));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionDTO> getWorkflowExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber) {

        List<Project> projects;
        Set<String> workflowIds = new HashSet<>();

        if (projectId == null) {
            projects = projectService.getProjects();
        } else {
            Project project = projectService.getProject(projectId);

            workflowIds.addAll(project.getWorkflowIds());

            projects = List.of(project);
        }

        Page<Job> jobsPage;

        if (workflowIds.isEmpty()) {
            jobsPage = Page.empty();
        } else {
            if (projectInstanceId == null) {
                jobsPage = jobService.getJobsPage(
                    jobStatus, jobStartDate, jobEndDate, new ArrayList<>(workflowIds), pageNumber);
            } else {
                Project project = projectService.getProjectInstanceProject(projectInstanceId);

                workflowIds.addAll(project.getWorkflowIds());

                List<Job> jobs = jobService.getJobs(jobStatus, jobStartDate, jobEndDate, new ArrayList<>(workflowIds));

                jobs = CollectionUtils.filter(jobs, job -> {
                    Optional<Long> jobInstanceIdOptional = instanceJobService.fetchJobInstanceId(
                        job.getId(), ProjectConstants.PROJECT_TYPE);

                    return jobInstanceIdOptional.isEmpty()
                        || Objects.equals(jobInstanceIdOptional.get(), projectInstanceId);
                });

                if (jobs.size() > JobService.DEFAULT_PAGE_SIZE * (pageNumber + 1)) {
                    jobs = jobs.subList(
                        JobService.DEFAULT_PAGE_SIZE * pageNumber, JobService.DEFAULT_PAGE_SIZE * (pageNumber + 1));
                }

                long total = jobService.countJobs(jobStatus, jobStartDate, jobEndDate, new ArrayList<>(workflowIds));

                PageRequest pageRequest = PageRequest.of(pageNumber, JobService.DEFAULT_PAGE_SIZE);

                jobsPage = new PageImpl<>(jobs, pageRequest, total);
            }
        }

        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(ProjectConstants.PROJECT_TYPE);
        List<Workflow> workflows = workflowService.getWorkflows(
            CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

        return jobsPage.map(job -> new WorkflowExecutionDTO(
            Validate.notNull(job.getId(), "id"),
            CollectionUtils.findFirstOrElse(
                projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId()), null),
            OptionalUtils.mapOrElse(
                instanceJobService.fetchJobInstanceId(job.getId(), ProjectConstants.PROJECT_TYPE),
                projectInstanceService::getProjectInstance, null),
            CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())),
            new JobDTO(job, Map.of(), List.of()),
            getTriggerExecutionDTO(
                projectInstanceId,
                triggerExecutionService.getJobTriggerExecution(Validate.notNull(job.getId(), "id")),
                instanceAccessor, job)));
    }

    @Override
    public WorkflowExecutionDTO testWorkflow(
        String workflowId, Map<String, Object> inputs, List<TestConnectionDTO> testConnectionDTOs) {

//        TODO triggerTestExecutor

        JobDTO job = jobTestExecutor.execute(new JobParameters(workflowId, inputs));

        return new WorkflowExecutionDTO(
            job.id(), null, null, workflowService.getWorkflow(workflowId), job, null);
    }

    private ComponentDefinition getComponentDefinition(String type) {
        ComponentOperation componentOperation = ComponentOperation.ofType(type);

        return componentDefinitionService.getComponentDefinition(
            componentOperation.componentName(), componentOperation.componentVersion());
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
