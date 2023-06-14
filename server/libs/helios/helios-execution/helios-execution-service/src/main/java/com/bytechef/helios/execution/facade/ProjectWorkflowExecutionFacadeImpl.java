
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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.helios.execution.dto.ProjectWorkflowExecutionDTO;
import com.bytechef.hermes.execution.dto.JobDTO;
import com.bytechef.hermes.execution.facade.JobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowExecutionFacadeImpl implements ProjectWorkflowExecutionFacade {

    private final JobFacade jobFacade;
    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectService projectService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowExecutionFacadeImpl(
        JobFacade jobFacade, JobService jobService, ProjectInstanceService projectInstanceService,
        ProjectService projectService, WorkflowService workflowService) {

        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public ProjectWorkflowExecutionDTO getProjectWorkflowExecution(long id) {
        JobDTO jobDTO = jobFacade.getJob(id);

        return new ProjectWorkflowExecutionDTO(
            Objects.requireNonNull(jobDTO.id()),
            OptionalUtils.orElse(projectInstanceService.fetchJobProjectInstance(jobDTO.id()), null),
            jobDTO,
            projectService.getWorkflowProject(jobDTO.workflowId()),
            workflowService.getWorkflow(jobDTO.workflowId()));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public Page<ProjectWorkflowExecutionDTO> searchProjectWorkflowExecutions(
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

        List<Workflow> workflows = workflowService.getWorkflows(
            CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

        return jobsPage.map(job -> new ProjectWorkflowExecutionDTO(
            Objects.requireNonNull(job.getId()),
            OptionalUtils.orElse(projectInstanceService.fetchJobProjectInstance(job.getId()), null),
            new JobDTO(job, List.of()),
            CollectionUtils.getFirst(
                projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId())),
            CollectionUtils.getFirst(workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId()))));
    }
}
