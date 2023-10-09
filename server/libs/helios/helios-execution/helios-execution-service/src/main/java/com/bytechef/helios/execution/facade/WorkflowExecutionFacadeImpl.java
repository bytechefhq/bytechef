
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.helios.execution.dto.WorkflowExecutionDTO;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.execution.dto.JobDTO;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectService projectService;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService,
        JobService jobService, ProjectInstanceService projectInstanceService, ProjectService projectService,
        TaskExecutionService taskExecutionService,
        @Qualifier("workflowAsyncFileStorageFacade") WorkflowFileStorageFacade workflowFileStorageFacade,
        WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowExecutionDTO getWorkflowExecution(long id) {
        Job job = jobService.getJob(id);

        JobDTO jobDTO = new JobDTO(
            job, workflowFileStorageFacade.readJobOutputs(job.getOutputs()), getJobTaskExecutions(id));
        Long projectInstanceId = (Long) job.getMetadata(MetadataConstants.INSTANCE_ID);

        return new WorkflowExecutionDTO(
            Validate.notNull(jobDTO.id(), "id"),
            projectInstanceId == null ? null : projectInstanceService.getProjectInstance(projectInstanceId),
            jobDTO, projectService.getWorkflowProject(jobDTO.workflowId()),
            workflowService.getWorkflow(jobDTO.workflowId()));
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

        if (projectInstanceId == null) {
            jobsPage = jobService.getJobsPage(
                jobStatus, jobStartDate, jobEndDate, new ArrayList<>(workflowIds), pageNumber);
        } else {
            Project project = projectService.getProjectInstanceProject(projectInstanceId);

            workflowIds.addAll(project.getWorkflowIds());

            List<Job> jobs = jobService.getJobs(jobStatus, jobStartDate, jobEndDate, new ArrayList<>(workflowIds));

            jobs = CollectionUtils.filter(jobs, job -> {
                Long curProjectInstanceId = (Long) job.getMetadata(MetadataConstants.INSTANCE_ID);

                return curProjectInstanceId == null || Objects.equals(curProjectInstanceId, projectInstanceId);
            });

            if (jobs.size() > JobService.DEFAULT_PAGE_SIZE * (pageNumber + 1)) {
                jobs = jobs.subList(
                    JobService.DEFAULT_PAGE_SIZE * pageNumber, JobService.DEFAULT_PAGE_SIZE * (pageNumber + 1));
            }

            long total = jobService.countJobs(jobStatus, jobStartDate, jobEndDate, new ArrayList<>(workflowIds));

            PageRequest pageRequest = PageRequest.of(pageNumber, JobService.DEFAULT_PAGE_SIZE);

            jobsPage = new PageImpl<>(jobs, pageRequest, total);
        }

        List<Workflow> workflows = workflowService.getWorkflows(
            CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

        return jobsPage.map(job -> {
            Long jobProjectInstanceId = (Long) job.getMetadata(MetadataConstants.INSTANCE_ID);

            return new WorkflowExecutionDTO(
                Validate.notNull(job.getId(), "id"),
                jobProjectInstanceId == null ? null : projectInstanceService.getProjectInstance(jobProjectInstanceId),
                new JobDTO(job, Map.of(), List.of()),
                CollectionUtils.getFirst(
                    projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId())),
                CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId())));
        });
    }

    private List<TaskExecutionDTO> getJobTaskExecutions(long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .map(taskExecution -> new TaskExecutionDTO(
                getComponentDefinition(taskExecution),
                workflowFileStorageFacade.readContextValue(
                    contextService.peek(
                        Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)),
                taskExecution.getOutput() == null
                    ? null
                    : workflowFileStorageFacade.readTaskExecutionOutput(taskExecution.getOutput()),
                taskExecutionService.getTaskExecution(taskExecution.getId())))
            .toList();
    }

    private ComponentDefinition getComponentDefinition(TaskExecution taskExecution) {
        ComponentOperation componentOperation = ComponentOperation.ofType(taskExecution.getType());

        return componentDefinitionService.getComponentDefinition(
            componentOperation.componentName(), componentOperation.componentVersion());
    }
}
