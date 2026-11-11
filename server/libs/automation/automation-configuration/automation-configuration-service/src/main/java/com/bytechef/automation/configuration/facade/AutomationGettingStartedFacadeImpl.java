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

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedDTO;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedRunDTO;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AutomationGettingStartedFacadeImpl implements AutomationGettingStartedFacade {

    private static final String COMPLETION_TYPE = "completionType";
    private static final String GETTING_STARTED = "gettingStarted";
    private static final String HTTP_CLIENT = "httpClient";
    private static final String HTTP_CLIENT_GET = "get";
    private static final String LOGGER = "logger";

    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final PropertyService propertyService;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public AutomationGettingStartedFacadeImpl(
        JobService jobService, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, PropertyService propertyService,
        TaskExecutionService taskExecutionService, WorkflowService workflowService) {

        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.propertyService = propertyService;
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public void dismissAutomationGettingStarted(long workspaceId) {
        complete(CompletionType.DISMISSED);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public AutomationGettingStartedDTO getAutomationGettingStarted(long workspaceId) {
        boolean stored = isStored();

        List<ProjectDeployment> projectDeployments = getProjectDeployments();

        List<Long> projectDeploymentIds = getProjectDeploymentIds(projectDeployments);

        AutomationGettingStartedRunDTO firstRun = fetchLatestRun(Job.Status.COMPLETED, projectDeploymentIds);
        AutomationGettingStartedRunDTO lastFailedRun = null;

        if (firstRun == null) {
            lastFailedRun = fetchLatestRun(Job.Status.FAILED, projectDeploymentIds);
        } else if (!stored) {
            complete(CompletionType.COMPLETED);
        }

        List<Project> projects = projectService.getProjects(null, null, null, null, null, null);

        List<Long> projectIds = projects.stream()
            .map(Project::getId)
            .toList();

        List<ProjectWorkflow> projectWorkflows =
            projectIds.isEmpty() ? List.of() : projectWorkflowService.getProjectWorkflows(projectIds);

        List<Workflow> workflows = getWorkflows(projectWorkflows);

        boolean published = projects.stream()
            .anyMatch(Project::isPublished);

        Set<Long> workspaceProjectDeploymentIds =
            getWorkspaceProjectDeploymentIds(workspaceId, projects, projectDeployments);

        firstRun = hideRunDetailsOutsideWorkspace(firstRun, workspaceProjectDeploymentIds);
        lastFailedRun = hideRunDetailsOutsideWorkspace(lastFailedRun, workspaceProjectDeploymentIds);

        return new AutomationGettingStartedDTO(
            stored || firstRun != null, !projectDeployments.isEmpty(), firstRun,
            hasTaskType(workflows, HTTP_CLIENT, HTTP_CLIENT_GET), lastFailedRun, isLive(projectDeployments),
            hasTaskType(workflows, LOGGER, null), !projects.isEmpty(), published, !projectWorkflows.isEmpty());
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public boolean isAutomationGettingStartedCompleted(long workspaceId) {
        if (isStored()) {
            return true;
        }

        List<Long> projectDeploymentIds = getProjectDeploymentIds(getProjectDeployments());

        AutomationGettingStartedRunDTO firstRun = fetchLatestRun(Job.Status.COMPLETED, projectDeploymentIds);

        if (firstRun == null) {
            return false;
        }

        complete(CompletionType.COMPLETED);

        return true;
    }

    private void complete(CompletionType completionType) {
        if (!isStored()) {
            propertyService.save(
                GETTING_STARTED, Map.of(COMPLETION_TYPE, completionType.name()), Scope.AUTOMATION, null);
        }
    }

    private boolean isStored() {
        List<Property> properties = propertyService.getProperties(List.of(GETTING_STARTED), Scope.AUTOMATION, null);

        return !properties.isEmpty();
    }

    private @Nullable AutomationGettingStartedRunDTO fetchLatestRun(
        Job.Status status, List<Long> projectDeploymentIds) {

        if (projectDeploymentIds.isEmpty()) {
            return null;
        }

        Page<Long> jobIdPage = principalJobService.getJobIds(
            status, null, null, projectDeploymentIds, PlatformType.AUTOMATION, List.of(), true, 0);

        List<Long> jobIds = jobIdPage.getContent();

        if (jobIds.isEmpty()) {
            return null;
        }

        long jobId = jobIds.getFirst();

        Job job = jobService.getJob(jobId);

        long projectDeploymentId = principalJobService.getJobPrincipalId(jobId, PlatformType.AUTOMATION);

        Optional<ProjectDeployment> projectDeploymentOptional =
            projectDeploymentService.fetchProjectDeployment(projectDeploymentId);

        Boolean projectDeploymentEnabled = null;

        if (projectDeploymentOptional.isPresent()) {
            ProjectDeployment projectDeployment = projectDeploymentOptional.get();

            projectDeploymentEnabled = projectDeployment.isEnabled();
        }

        Instant date = job.getEndDate() == null ? job.getStartDate() : job.getEndDate();

        return new AutomationGettingStartedRunDTO(
            jobId, job.getStatus(), date, getErrorMessage(job, jobId), projectDeploymentId, projectDeploymentEnabled);
    }

    private @Nullable String getErrorMessage(Job job, long jobId) {
        ExecutionError executionError = job.getError();

        if (executionError != null) {
            return executionError.getMessage();
        }

        if (job.getStatus() != Job.Status.FAILED) {
            return null;
        }

        List<TaskExecution> failedTaskExecutions = taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(
            jobId, List.of(), 0, 1);

        return failedTaskExecutions.stream()
            .map(TaskExecution::getError)
            .filter(Objects::nonNull)
            .map(ExecutionError::getMessage)
            .findFirst()
            .orElse(null);
    }

    private static Set<Long> getWorkspaceProjectDeploymentIds(
        long workspaceId, List<Project> projects, List<ProjectDeployment> projectDeployments) {

        Set<Long> workspaceProjectIds = projects.stream()
            .filter(project -> Objects.equals(project.getWorkspaceId(), workspaceId))
            .map(Project::getId)
            .collect(Collectors.toSet());

        return projectDeployments.stream()
            .filter(projectDeployment -> workspaceProjectIds.contains(projectDeployment.getProjectId()))
            .map(ProjectDeployment::getId)
            .collect(Collectors.toSet());
    }

    private static @Nullable AutomationGettingStartedRunDTO hideRunDetailsOutsideWorkspace(
        @Nullable AutomationGettingStartedRunDTO run, Set<Long> workspaceProjectDeploymentIds) {

        if (run == null || workspaceProjectDeploymentIds.contains(run.projectDeploymentId())) {
            return run;
        }

        return new AutomationGettingStartedRunDTO(
            run.jobId(), run.status(), run.date(), null, run.projectDeploymentId(), null);
    }

    private static List<Long> getProjectDeploymentIds(List<ProjectDeployment> projectDeployments) {
        return projectDeployments.stream()
            .map(ProjectDeployment::getId)
            .toList();
    }

    private List<ProjectDeployment> getProjectDeployments() {
        return projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null);
    }

    private List<Workflow> getWorkflows(List<ProjectWorkflow> projectWorkflows) {
        List<String> workflowIds = projectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .distinct()
            .toList();

        if (workflowIds.isEmpty()) {
            return List.of();
        }

        return workflowService.getWorkflows(workflowIds);
    }

    private boolean isLive(List<ProjectDeployment> projectDeployments) {
        List<Long> enabledProjectDeploymentIds = projectDeployments.stream()
            .filter(ProjectDeployment::isEnabled)
            .map(ProjectDeployment::getId)
            .toList();

        if (enabledProjectDeploymentIds.isEmpty()) {
            return false;
        }

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(enabledProjectDeploymentIds);

        return projectDeploymentWorkflows.stream()
            .anyMatch(ProjectDeploymentWorkflow::isEnabled);
    }

    private static boolean hasTaskType(
        List<Workflow> workflows, String componentName, @Nullable String operationName) {

        for (Workflow workflow : workflows) {
            for (WorkflowTask workflowTask : workflow.getTasks(true)) {
                if (matchesType(workflowTask.getType(), componentName, operationName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean matchesType(@Nullable String type, String componentName, @Nullable String operationName) {
        if (type == null) {
            return false;
        }

        String[] typeParts = type.split("/");

        if (typeParts.length != 3 || !componentName.equals(typeParts[0])) {
            return false;
        }

        return operationName == null || operationName.equals(typeParts[2]);
    }

    private enum CompletionType {
        COMPLETED, DISMISSED
    }
}
