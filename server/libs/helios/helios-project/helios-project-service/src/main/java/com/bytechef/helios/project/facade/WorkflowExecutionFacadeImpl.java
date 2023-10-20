
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

package com.bytechef.helios.project.facade;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.dto.WorkflowExecutionDTO;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.util.ComponentUtils;
import com.bytechef.hermes.workflow.dto.TaskExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WorkflowExecutionFacadeImpl implements WorkflowExecutionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectService projectService;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService, JobService jobService,
        ProjectInstanceService projectInstanceService, ProjectService projectService,
        TaskExecutionService taskExecutionService, WorkflowService workflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public WorkflowExecutionDTO getWorkflowExecution(long id) {
        Job job = jobService.getJob(id);

        return new WorkflowExecutionDTO(
            Objects.requireNonNull(job.getId()),
            OptionalUtils.orElse(projectInstanceService.fetchJobProjectInstance(job.getId()), null), job,
            projectService.getWorkflowProject(job.getWorkflowId()),
            CollectionUtils.map(
                taskExecutionService.getJobTaskExecutions(
                    Objects.requireNonNull(job.getId())),
                taskExecution -> new TaskExecutionDTO(
                    getComponentDefinition(taskExecution),
                    contextService.peek(
                        Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION),
                    taskExecution)),
            workflowService.getWorkflow(job.getWorkflowId()));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public Page<WorkflowExecutionDTO> searchWorkflowExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber) {

        List<Project> projects;

        if (projectId == null) {
            projects = projectService.getProjects();
        } else {
            projects = List.of(projectService.getProject(projectId));
        }

        List<String> projectWorkflowIds = Collections.emptyList();

        if (projectId != null) {
            Project project = projects.get(0);

            projectWorkflowIds = project.getWorkflowIds();
        }

        Page<Job> jobsPage = jobService.searchJobs(
            jobStatus, jobStartDate, jobEndDate, workflowId, projectWorkflowIds, pageNumber);

        List<TaskExecution> taskExecutions = taskExecutionService.getJobsTaskExecutions(
            CollectionUtils.map(jobsPage.toList(), Job::getId));

        List<Workflow> workflows = workflowService.getWorkflows(
            CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

        return jobsPage.map(job -> new WorkflowExecutionDTO(
            Objects.requireNonNull(job.getId()),
            OptionalUtils.orElse(projectInstanceService.fetchJobProjectInstance(job.getId()), null), job,
            CollectionUtils.getFirst(
                projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId())),
            taskExecutions.stream()
                .filter(taskExecution -> Objects.equals(taskExecution.getJobId(), job.getId()))
                .map(taskExecution -> new TaskExecutionDTO(
                    getComponentDefinition(taskExecution),
                    contextService.peek(
                        Objects.requireNonNull(taskExecution.getId()), Context.Classname.TASK_EXECUTION),
                    taskExecution))
                .toList(),
            CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId()))));
    }

    private ComponentDefinitionDTO getComponentDefinition(TaskExecution taskExecution) {
        ComponentUtils.ComponentType componentType = ComponentUtils.getComponentType(taskExecution.getType());

        return componentDefinitionService.getComponentDefinition(
            componentType.componentName(), componentType.componentVersion());
    }
}
