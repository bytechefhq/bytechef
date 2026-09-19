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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.exception.ProjectDeploymentErrorType;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectDeploymentFacadeImpl implements ProjectDeploymentFacade {

    private static final Logger log = LoggerFactory.getLogger(ProjectDeploymentFacadeImpl.class);

    private final ConnectionService connectionService;
    private final Evaluator evaluator;
    private final EnvironmentService environmentService;
    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final String webhookUrl;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentFacadeImpl(
        ConnectionService connectionService, Evaluator evaluator, EnvironmentService environmentService,
        PrincipalJobFacade principalJobFacade, PrincipalJobService principalJobService, JobFacade jobFacade,
        JobService jobService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService,
        TriggerDefinitionService triggerDefinitionService, TriggerExecutionService triggerExecutionService,
        TriggerLifecycleFacade triggerLifecycleFacade, ApplicationProperties applicationProperties,
        ComponentConnectionFacade componentConnectionFacade, WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.evaluator = evaluator;
        this.environmentService = environmentService;
        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.webhookUrl = applicationProperties.getWebhookUrl();
        this.componentConnectionFacade = componentConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    // The whole DTO, not its projectId: the evaluator reads the target environment off it so that the role
    // checked is the one held in the environment being deployed into.
    @PreAuthorize("hasPermission(#projectDeploymentDTO, 'WORKFLOW_EDIT')")
    public long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO) {
        return createProjectDeployment(
            projectDeploymentDTO.toProjectDeployment(), CollectionUtils.map(
                projectDeploymentDTO.projectDeploymentWorkflows(),
                ProjectDeploymentWorkflowDTO::toProjectDeploymentWorkflow),
            projectDeploymentDTO.tags());
    }

    @Override
    public long createProjectDeployment(
        ProjectDeployment projectDeployment, String workflowId, List<ProjectDeploymentWorkflowConnection> connections) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(connections);
        projectDeploymentWorkflow.setInputs(Map.of());
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return createProjectDeployment(projectDeployment, List.of(projectDeploymentWorkflow), List.of());
    }

    @Override
    public long createProjectDeployment(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows,
        List<Tag> tags) {

        long projectId = Validate.notNull(projectDeployment.getProjectId(), "projectId");

        Project project = projectService.getProject(projectId);

        if (!project.isPublished()) {
            throw new ConfigurationException(
                "Project id=%s is not published".formatted(projectId),
                ProjectDeploymentErrorType.PROJECT_NOT_PUBLISHED);
        }

        if (project.getLastProjectVersion() == projectDeployment.getProjectVersion()) {
            throw new ConfigurationException(
                "Project version v=%s cannot be in DRAFT".formatted(projectDeployment.getProjectVersion()),
                ProjectDeploymentErrorType.INVALID_PROJECT_VERSION);
        }

        if (!tags.isEmpty()) {
            projectDeployment.setTags(checkTags(tags));
        }

        projectDeployment = projectDeploymentService.create(projectDeployment);

        checkProjectDeploymentWorkflows(projectDeployment, -1, projectDeploymentWorkflows, List.of());

        return projectDeployment.getId();
    }

    // Starts a job in the deployment's environment, so a VIEWER must not reach it: an on-demand run of a Production
    // deployment is a write to the outside world. The deployment id is resolved server-side, which is what makes the
    // environment half of the check trustworthy — the caller does not get to say which environment they are judged in.
    @Override
    @PreAuthorize("hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')")
    @Transactional(propagation = Propagation.NEVER)
    public long createProjectDeploymentWorkflowJob(Long id, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                id, workflowId);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(id);

        return principalJobFacade.createJob(
            new JobParametersDTO(
                workflowId, projectDeploymentWorkflow.getInputs(),
                Map.of("projectVersion", projectDeployment.getProjectVersion())),
            id, PlatformType.AUTOMATION);
    }

    // Guarded here rather than beneath: ProjectDeploymentServiceImpl.delete carries no guard, and the triggers, jobs
    // and
    // deployment workflows are torn down before it is reached.
    @Override
    @PreAuthorize("hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_DELETE')")
    public void deleteProjectDeployment(long id) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(id);

        if (projectDeployment.isEnabled()) {
            enableProjectDeployment(projectDeployment.getId(), false);
        }

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(id);

        List<Long> jobIds = principalJobService.getJobIds(id, PlatformType.AUTOMATION);

        for (long jobId : jobIds) {
            triggerExecutionService.deleteJobTriggerExecution(jobId);

            principalJobService.deletePrincipalJobs(jobId, PlatformType.AUTOMATION);
        }

        List<Long> orderedJobIds = jobIds.stream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .toList();

        for (long jobId : orderedJobIds) {
            jobFacade.deleteJob(jobId);
        }

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            projectDeploymentWorkflowService.delete(projectDeploymentWorkflow.getId());
        }

        projectDeploymentService.delete(id);

// TODO find a way to delete all tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    // Enabling installs the deployment's triggers with its own connections, so unattended repeating execution of a
    // Production workflow is obtainable here — strictly more than the single on-demand run the job guard denies.
    // Disabling is the first half of deleteProjectDeployment's tear-down and is reachable on its own, so without this a
    // member refused delete could still take a Production deployment's live triggers down. Nothing beneath closes
    // either: ProjectDeploymentServiceImpl.updateEnabled has no guard of its own. deleteProjectDeployment reaches this
    // by self-invocation, which does not cross the proxy, so deleting needs DEPLOYMENT_DELETE and not DEPLOYMENT_EDIT
    // as well.
    @Override
    @PreAuthorize("hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')")
    public void enableProjectDeployment(long projectDeploymentId, boolean enable) {
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId);

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            if (!projectDeploymentWorkflow.isEnabled()) {
                continue;
            }

            if (enable) {
                enableWorkflowTriggers(projectDeploymentWorkflow);
            } else {
                disableWorkflowTriggers(projectDeploymentWorkflow);
            }
        }

        projectDeploymentService.updateEnabled(projectDeploymentId, enable);
    }

    // Same reasoning as enableProjectDeployment, one level down: enabling reaches enableWorkflowTriggers and disabling
    // also calls stopRunningJobs, which kills the started jobs of this one workflow. The row acted on is looked up from
    // (projectDeploymentId, workflowId), so the id the guard keys on is the id the write is selected by.
    @Override
    @PreAuthorize("hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')")
    public void enableProjectDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enable) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = doEnableProjectDeploymentWorkflow(
            projectDeploymentId, workflowId, enable);

        projectDeploymentWorkflowService.updateEnabled(projectDeploymentWorkflow.getId(), enable);
    }

    // Needs its own annotation: it reaches the overload above by self-invocation, which does not cross the proxy, so
    // that guard never fires for this entry point. The check is environment-blind on purpose rather than by oversight —
    // no SpEL built-in reaches PermissionService.hasWorkspaceScopeForProject(long, String, Environment), which does
    // exist and is implemented; AutomationMethodSecurityExpressionRoot simply exposes no passthrough for it, so writing
    // an environment-aware expression here would mean adding one. Its only production caller is the embedded
    // connected-user facade, which runs under skip-checks where every guard is inert, so an environment-blind check
    // here costs nothing and is what a future caller reaching the bean directly would otherwise not get at all.
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'DEPLOYMENT_EDIT')")
    public void enableProjectDeploymentWorkflow(
        long projectId, String workflowId, boolean enable, Environment environment) {

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(projectId, environment);

        enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enable);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_VIEW')")
    @Transactional(readOnly = true)
    public ProjectDeploymentDTO getProjectDeployment(long id) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(id);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion());
        List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion());

        return new ProjectDeploymentDTO(
            projectDeployment,
            CollectionUtils.map(
                CollectionUtils.filter(
                    projectDeploymentWorkflowService.getProjectDeploymentWorkflows(id),
                    projectDeploymentWorkflow -> workflowIds.contains(projectDeploymentWorkflow.getWorkflowId())),
                projectDeploymentWorkflow -> toProjectDeploymentWorkflowDTO(
                    projectDeploymentWorkflow, projectDeployment, projectWorkflows)),
            projectService.getProject(projectDeployment.getProjectId()),
            getProjectDeploymentLastExecutionDate(Validate.notNull(projectDeployment.getId(), "id")),
            tagService.getTags(projectDeployment.getTagIds()));
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#id, 'DEPLOYMENT_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<Tag> getProjectDeploymentTags(long id, Long environmentId) {
        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            false, environment, null, null, id);

        return tagService.getTags(
            projectDeployments.stream()
                .map(ProjectDeployment::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#id, 'DEPLOYMENT_VIEW', #environmentId)")
    public List<ProjectDeploymentDTO> getWorkspaceProjectDeployments(
        long id, Long environmentId, Long projectId, Long tagId, boolean includeAllFields) {

        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            false, environment, projectId, tagId, id);

        if (includeAllFields) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(CollectionUtils.map(projectDeployments, ProjectDeployment::getId));
            List<Project> projects = getProjects(projectDeployments);
            List<Tag> tags = getTags(projectDeployments);

            List<Long> projectIds = projectDeployments.stream()
                .map(ProjectDeployment::getProjectId)
                .distinct()
                .toList();

            List<ProjectWorkflow> allProjectWorkflows = projectWorkflowService.getProjectWorkflows(projectIds);

            return CollectionUtils.map(
                projectDeployments,
                projectDeployment -> {
                    Project project = CollectionUtils.getFirst(
                        projects, curProject -> Objects.equals(curProject.getId(), projectDeployment.getProjectId()));

                    List<String> workflowIds = allProjectWorkflows.stream()
                        .filter(projectWorkflow -> Objects.equals(
                            projectWorkflow.getProjectId(), projectDeployment.getProjectId()) &&
                            projectWorkflow.getProjectVersion() == projectDeployment.getProjectVersion())
                        .map(ProjectWorkflow::getWorkflowId)
                        .toList();

                    List<ProjectWorkflow> deploymentProjectWorkflows = allProjectWorkflows.stream()
                        .filter(projectWorkflow -> Objects.equals(
                            projectWorkflow.getProjectId(), projectDeployment.getProjectId()))
                        .toList();

                    return new ProjectDeploymentDTO(
                        projectDeployment,
                        CollectionUtils.map(
                            CollectionUtils.filter(
                                projectDeploymentWorkflows,
                                projectDeploymentWorkflow -> Objects.equals(
                                    projectDeploymentWorkflow.getProjectDeploymentId(), projectDeployment.getId()) &&
                                    workflowIds.contains(projectDeploymentWorkflow.getWorkflowId())),
                            projectDeploymentWorkflow -> toProjectDeploymentWorkflowDTO(
                                projectDeploymentWorkflow, projectDeployment, deploymentProjectWorkflows)),
                        project,
                        getProjectDeploymentLastExecutionDate(Validate.notNull(projectDeployment.getId(), "id")),
                        filterTags(tags, projectDeployment));
                });
        } else {
            return CollectionUtils.map(projectDeployments, ProjectDeploymentDTO::new);
        }
    }

    // The deployment's id rather than the whole DTO, which is where this deliberately parts company with the sibling
    // createProjectDeployment. On create the DTO's environment IS the environment the row lands in, so reading it off
    // the DTO is sound. On update the environment is immutable — ProjectDeploymentServiceImpl.update copies onto the
    // persisted row and never touches it — so a DTO-supplied environment would let a caller name the environment they
    // are judged in and edit a Production deployment with a Development role. Keyed on the id, both the workspace and
    // the environment come from the stored row.
    @Override
    @PreAuthorize("hasPermission(#projectDeploymentDTO.id, 'ProjectDeployment', 'DEPLOYMENT_CREATE')")
    public void updateProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO) {
        updateProjectDeployment(
            projectDeploymentDTO.toProjectDeployment(),
            CollectionUtils.map(
                projectDeploymentDTO.projectDeploymentWorkflows(),
                ProjectDeploymentWorkflowDTO::toProjectDeploymentWorkflow),
            projectDeploymentDTO.tags());
    }

    // Deliberately unguarded here: the only caller is the embedded connected-user facade, which runs under
    // skip-checks, and the write it ends in already requires DEPLOYMENT_CREATE on this projectId in
    // ProjectDeploymentServiceImpl.update. A facade guard keyed on projectId would only restate that check; the
    // environment half cannot be expressed from an environmentId, so it is not gained by adding one.
    @Override
    public void updateProjectDeployment(
        long projectId, int projectVersion, String workflowUuid,
        List<ProjectDeploymentWorkflowConnection> connections, Long environmentId) {

        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            projectDeploymentService.getProjectDeploymentId(projectId, environment));

        projectDeployment.setProjectVersion(projectVersion);

        List<ProjectDeploymentWorkflow> oldProjectDeploymentWorkflows = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeployment.getId());

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectWorkflowService
            .getProjectWorkflows(projectDeployment.getProjectId(), projectVersion)
            .stream()
            .map(curProjectWorkflow -> {
                ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

                projectDeploymentWorkflow.setProjectDeploymentId(projectId);
                projectDeploymentWorkflow.setWorkflowId(curProjectWorkflow.getWorkflowId());

                if (Objects.equals(curProjectWorkflow.getUuidAsString(), workflowUuid)) {
                    projectDeploymentWorkflow.setConnections(connections);
                    projectDeploymentWorkflow.setEnabled(
                        getFirst(curProjectWorkflow, oldProjectDeploymentWorkflows, projectDeployment)
                            .map(ProjectDeploymentWorkflow::isEnabled)
                            .orElse(false));
                    projectDeploymentWorkflow.setInputs(Map.of());
                } else {
                    return getFirst(curProjectWorkflow, oldProjectDeploymentWorkflows, projectDeployment)
                        .map(curProjectDeploymentWorkflow -> {
                            projectDeploymentWorkflow.setConnections(curProjectDeploymentWorkflow.getConnections());
                            projectDeploymentWorkflow.setEnabled(curProjectDeploymentWorkflow.isEnabled());
                            projectDeploymentWorkflow.setInputs(curProjectDeploymentWorkflow.getInputs());

                            return projectDeploymentWorkflow;
                        })
                        .orElse(projectDeploymentWorkflow);
                }

                return projectDeploymentWorkflow;
            })
            .toList();

        updateProjectDeployment(projectDeployment, projectDeploymentWorkflows, List.of());
    }

    // Deliberately unguarded here: this is the shared tail both other overloads reach by self-invocation, so a guard
    // would not fire for them anyway, and the write it performs is the already-guarded
    // ProjectDeploymentServiceImpl.update(ProjectDeployment).
    @Override
    public void updateProjectDeployment(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows,
        List<Tag> tags) {

        if (!tags.isEmpty()) {
            projectDeployment.setTags(tags);
        }

        ProjectDeployment oldProjectDeployment = projectDeploymentService.getProjectDeployment(
            projectDeployment.getId());

        projectDeploymentService.update(projectDeployment);

        checkProjectDeploymentWorkflows(
            projectDeployment, oldProjectDeployment.getProjectVersion(), projectDeploymentWorkflows,
            projectWorkflowService.getProjectWorkflows(projectDeployment.getProjectId()));
    }

    // The tag-only ProjectDeploymentServiceImpl.update(long, List) overload carries no guard — only
    // update(ProjectDeployment) does — so this was reachable by any authenticated caller.
    @Override
    @PreAuthorize("hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')")
    public void updateProjectDeploymentTags(long id, List<Tag> tags) {
        projectDeploymentService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    // Repoints a deployed workflow's connections and inputs, and ProjectDeploymentWorkflowServiceImpl.update carries no
    // guard. Keyed on the row's own id, because that is what the write selects by — update() looks the row up by
    // getId() and neither reads nor copies the argument's projectDeploymentId. Keying on projectDeploymentId instead
    // would check a deployment the write never touches: the REST path
    // /project-deployments/{id}/project-deployment-workflows/{projectDeploymentWorkflowId} supplies the two
    // independently, so a member who is editor in Development and viewer in Production could satisfy a
    // projectDeploymentId-keyed check against Development while repointing a Production row.
    @Override
    @PreAuthorize("hasPermission(#projectDeploymentWorkflow.id, 'ProjectDeploymentWorkflow', 'DEPLOYMENT_EDIT')")
    public void updateProjectDeploymentWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        validateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        projectDeploymentWorkflowService.update(projectDeploymentWorkflow);
    }

    private void checkProjectDeploymentWorkflows(
        ProjectDeployment projectDeployment, int oldProjectVersion,
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows, List<ProjectWorkflow> allProjectWorkflows) {

        List<ProjectDeploymentWorkflow> oldProjectDeploymentWorkflows = List.of();

        if (oldProjectVersion != -1) {
            oldProjectDeploymentWorkflows = projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                projectDeployment.getId());
        }

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            ProjectDeploymentWorkflow oldProjectDeploymentWorkflow = null;

            if (oldProjectVersion != -1) {
                String workflowUuid = allProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(
                        curProjectWorkflow.getWorkflowId(), projectDeploymentWorkflow.getWorkflowId()))
                    .findFirst()
                    .map(ProjectWorkflow::getUuidAsString)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Project workflow with workflowId=%s not found".formatted(
                            projectDeploymentWorkflow.getWorkflowId())));

                String oldWorkflowId = allProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(
                        curProjectWorkflow.getUuidAsString(), workflowUuid) &&
                        curProjectWorkflow.getProjectVersion() == oldProjectVersion)
                    .map(ProjectWorkflow::getWorkflowId)
                    .findFirst()
                    .orElse(null);

                if (oldWorkflowId != null) {
                    oldProjectDeploymentWorkflow = oldProjectDeploymentWorkflows.stream()
                        .filter(
                            curProjectDeploymentWorkflow -> Objects.equals(curProjectDeploymentWorkflow.getWorkflowId(),
                                oldWorkflowId))
                        .findFirst()
                        .orElse(null);
                }
            }

            validateProjectDeploymentWorkflow(projectDeploymentWorkflow);

            if (oldProjectDeploymentWorkflow == null) {
                projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());

                projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

                if (projectDeployment.isEnabled() && projectDeploymentWorkflow.isEnabled()) {
                    enableProjectDeploymentWorkflow(
                        projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), true);
                }
            } else {
                String oldWorkflowId = oldProjectDeploymentWorkflow.getWorkflowId();
                boolean wasEnabled = oldProjectDeploymentWorkflow.isEnabled();

                oldProjectDeploymentWorkflow.setConnections(projectDeploymentWorkflow.getConnections());
                oldProjectDeploymentWorkflow.setEnabled(projectDeploymentWorkflow.isEnabled());
                oldProjectDeploymentWorkflow.setInputs(projectDeploymentWorkflow.getInputs());
                oldProjectDeploymentWorkflow.setWorkflowId(projectDeploymentWorkflow.getWorkflowId());

                if (projectDeploymentWorkflow.isEnabled()) {
                    if (projectDeployment.isEnabled() && wasEnabled) {
                        doEnableProjectDeploymentWorkflow(
                            projectDeployment.getId(), oldWorkflowId, false);
                    }

                    projectDeploymentWorkflowService.update(oldProjectDeploymentWorkflow);

                    if (projectDeployment.isEnabled()) {
                        doEnableProjectDeploymentWorkflow(
                            projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), true);
                    }
                } else {
                    if (wasEnabled) {
                        doEnableProjectDeploymentWorkflow(projectDeployment.getId(), oldWorkflowId, false);
                    }

                    projectDeploymentWorkflowService.update(oldProjectDeploymentWorkflow);
                }
            }
        }

        for (ProjectDeploymentWorkflow oldProjectDeploymentWorkflow : oldProjectDeploymentWorkflows) {
            String workflowUuid = allProjectWorkflows.stream()
                .filter(curProjectWorkflow -> Objects.equals(
                    curProjectWorkflow.getWorkflowId(), oldProjectDeploymentWorkflow.getWorkflowId()))
                .findFirst()
                .map(ProjectWorkflow::getUuidAsString)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Project workflow with workflowId=%s not found".formatted(
                        oldProjectDeploymentWorkflow.getWorkflowId())));

            String workflowId = allProjectWorkflows.stream()
                .filter(curProjectWorkflow -> Objects.equals(
                    curProjectWorkflow.getUuidAsString(), workflowUuid) &&
                    curProjectWorkflow.getProjectVersion() == projectDeployment.getProjectVersion())
                .findFirst()
                .map(ProjectWorkflow::getWorkflowId)
                .orElse(null);

            if (workflowId == null || CollectionUtils.noneMatch(
                projectDeploymentWorkflows,
                projectDeploymentWorkflow -> Objects.equals(projectDeploymentWorkflow.getWorkflowId(), workflowId))) {

                if (oldProjectDeploymentWorkflow.isEnabled()) {
                    doEnableProjectDeploymentWorkflow(
                        projectDeployment.getId(), oldProjectDeploymentWorkflow.getWorkflowId(), false);
                }

                projectDeploymentWorkflowService.delete(oldProjectDeploymentWorkflow.getId());
            }
        }
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private static boolean containsTag(ProjectDeployment projectDeployment, Tag tag) {
        List<Long> tagIds = projectDeployment.getTagIds();

        return tagIds.contains(tag.getId());
    }

    private void disableWorkflowTriggers(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                projectWorkflow.getUuidAsString(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.evaluateParameters(projectDeploymentWorkflow.getInputs(), evaluator),
                getConnectionId(projectDeploymentWorkflow.getProjectDeploymentId(), workflow.getId(), workflowTrigger));
        }
    }

    private ProjectDeploymentWorkflow doEnableProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowId, boolean enable) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                projectDeploymentId, workflowId);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        if (enable) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            List<ComponentConnection> requiredComponentConnections = CollectionUtils.concat(
                WorkflowTrigger.of(workflow)
                    .stream()
                    .flatMap(workflowTrigger -> CollectionUtils.stream(
                        componentConnectionFacade.getComponentConnections(workflowTrigger)))
                    .filter(ComponentConnection::required)
                    .toList(),
                workflow.getTasks(true)
                    .stream()
                    .flatMap(workflowTask -> CollectionUtils.stream(
                        componentConnectionFacade.getComponentConnections(workflowTask)))
                    .filter(ComponentConnection::required)
                    .toList());

            List<String> workflowNodeNames = requiredComponentConnections.stream()
                .map(ComponentConnection::workflowNodeName)
                .toList();

            List<ProjectDeploymentWorkflowConnection> connections = projectDeploymentWorkflow.getConnections()
                .stream()
                .filter(connection -> workflowNodeNames.contains(connection.getWorkflowNodeName()))
                .toList();

            if (!requiredComponentConnections.isEmpty() && requiredComponentConnections.size() != connections.size()) {
                throw new ConfigurationException(
                    "Not all required connections are set for a workflow with id=%s".formatted(workflow.getId()),
                    ProjectDeploymentErrorType.WORKFLOW_CONNECTIONS_NOT_FOUND);
            }
        }

        if (projectDeployment.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(projectDeploymentWorkflow);
            } else {
                disableWorkflowTriggers(projectDeploymentWorkflow);
                // Also stop any currently running jobs for this workflow under this project deployment
                stopRunningJobs(projectDeploymentWorkflow);
            }
        }

        return projectDeploymentWorkflow;
    }

    private void enableWorkflowTriggers(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        validateProjectDeploymentWorkflowInputs(projectDeploymentWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            projectDeploymentWorkflow.getProjectDeploymentId());

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), "manual")) {
                continue;
            }

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                projectWorkflow.getUuidAsString(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), workflowExecutionId, workflowNodeType,
                workflowTrigger.evaluateParameters(projectDeploymentWorkflow.getInputs(), evaluator),
                getConnectionId(projectDeploymentWorkflow.getProjectDeploymentId(), workflow.getId(), workflowTrigger),
                getWebhookUrl(workflowExecutionId), projectDeployment.getEnvironmentId());
        }
    }

    private void stopRunningJobs(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        List<Long> principalIds = List.of(projectDeploymentWorkflow.getProjectDeploymentId());
        List<String> workflowIds = List.of(projectDeploymentWorkflow.getWorkflowId());

        int pageNumber = 0;

        while (true) {
            Page<Long> page = principalJobService.getJobIds(
                Job.Status.STARTED, null, null, principalIds, PlatformType.AUTOMATION, workflowIds, false, pageNumber);

            List<Long> jobIds = page.getContent();

            if (jobIds.isEmpty()) {
                break;
            }

            for (Long jobId : jobIds) {
                jobFacade.stopJob(jobId);
            }

            if (page.hasNext()) {
                pageNumber++;
            } else {
                break;
            }
        }
    }

    private List<Tag> filterTags(List<Tag> tags, ProjectDeployment projectDeployment) {
        return CollectionUtils.filter(tags, tag -> containsTag(projectDeployment, tag));
    }

    private Long getConnectionId(long projectDeploymentId, String workflowId, WorkflowTrigger workflowTrigger) {
        return componentConnectionFacade.getComponentConnections(workflowTrigger)
            .stream()
            .findFirst()
            .map(workflowConnection -> getConnectionId(
                projectDeploymentId, workflowId, workflowConnection.workflowNodeName(), workflowConnection.key()))
            .orElse(null);
    }

    private Long getConnectionId(
        long projectDeploymentId, String workflowId, String workflowNodeName, String workflowConnectionKey) {

        return projectDeploymentWorkflowService
            .fetchProjectDeploymentWorkflowConnection(
                projectDeploymentId, workflowId, workflowNodeName, workflowConnectionKey)
            .map(ProjectDeploymentWorkflowConnection::getConnectionId)
            .orElse(null);
    }

    private Optional<ProjectDeploymentWorkflow> getFirst(
        ProjectWorkflow curProjectWorkflow, List<ProjectDeploymentWorkflow> oldProjectDeploymentWorkflows,
        ProjectDeployment projectDeployment) {
        return oldProjectDeploymentWorkflows.stream()
            .filter(curProjectDeploymentWorkflow -> {
                String projectDeploymentWorkflowUuid =
                    projectWorkflowService.getProjectWorkflowUuid(
                        projectDeployment.getId(), curProjectDeploymentWorkflow.getWorkflowId());

                return Objects.equals(
                    projectDeploymentWorkflowUuid, curProjectWorkflow.getUuidAsString());
            })
            .findFirst();
    }

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private Job.Status getJobStatus(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getStatus();
    }

    private Instant getProjectDeploymentLastExecutionDate(long projectDeploymentId) {
        return principalJobService.fetchLastJobId(projectDeploymentId, PlatformType.AUTOMATION)
            .map(this::getJobEndDate)
            .orElse(null);
    }

    private List<Project> getProjects(List<ProjectDeployment> projectDeployments) {
        return projectService.getProjects(
            projectDeployments
                .stream()
                .map(ProjectDeployment::getProjectId)
                .filter(Objects::nonNull)
                .toList());
    }

    private String getStaticWebhookUrl(long projectDeploymentId, String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            TriggerDefinition triggerDefinition;

            try {
                triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                    Objects.requireNonNull(triggerWorkflowNodeType.operation()));
            } catch (Exception exception) {
                log.error(
                    "Failed to get trigger definition for workflow trigger type={}",
                    triggerWorkflowNodeType.name(),
                    exception);

                continue;
            }

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
                !Objects.equals(triggerDefinition.getName(), "manual")) {

                ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

                return getWebhookUrl(
                    WorkflowExecutionId.of(
                        PlatformType.AUTOMATION, projectDeploymentId, projectWorkflow.getUuidAsString(),
                        workflowTrigger.getName()));
            }
        }

        return null;
    }

    private List<Tag> getTags(List<ProjectDeployment> projectDeployments) {
        return tagService.getTags(
            projectDeployments.stream()
                .flatMap(projectDeployment -> CollectionUtils.stream(projectDeployment.getTagIds()))
                .filter(Objects::nonNull)
                .toList());
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl.replace("{id}", workflowExecutionId.toString());
    }

    private Instant getWorkflowLastExecutionDate(long projectDeploymentId, List<String> workflowIds) {
        return principalJobService.fetchLastWorkflowJobId(projectDeploymentId, workflowIds, PlatformType.AUTOMATION)
            .map(this::getJobEndDate)
            .orElse(null);
    }

    private Job.Status getWorkflowLastExecutionStatus(long projectDeploymentId, List<String> workflowIds) {
        return principalJobService.fetchLastWorkflowJobId(projectDeploymentId, workflowIds, PlatformType.AUTOMATION)
            .map(this::getJobStatus)
            .orElse(null);
    }

    private String getWorkflowUuid(
        String workflowId, int projectVersion, List<ProjectWorkflow> projectWorkflows) {

        return projectWorkflows.stream()
            .filter(projectWorkflow -> Objects.equals(projectWorkflow.getWorkflowId(), workflowId) &&
                projectWorkflow.getProjectVersion() == projectVersion)
            .findFirst()
            .map(ProjectWorkflow::getUuidAsString)
            .orElseThrow();
    }

    private ProjectDeploymentWorkflowDTO toProjectDeploymentWorkflowDTO(
        ProjectDeploymentWorkflow projectDeploymentWorkflow, ProjectDeployment projectDeployment,
        List<ProjectWorkflow> projectWorkflows) {

        String workflowUuid = getWorkflowUuid(
            projectDeploymentWorkflow.getWorkflowId(), projectDeployment.getProjectVersion(),
            projectWorkflows);

        List<String> workflowUuidWorkflowIds = projectWorkflows.stream()
            .filter(projectWorkflow -> Objects.equals(
                projectWorkflow.getUuidAsString(), workflowUuid))
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        return new ProjectDeploymentWorkflowDTO(
            projectDeploymentWorkflow,
            getWorkflowLastExecutionDate(projectDeployment.getId(), workflowUuidWorkflowIds),
            getWorkflowLastExecutionStatus(projectDeployment.getId(), workflowUuidWorkflowIds),
            getStaticWebhookUrl(
                projectDeploymentWorkflow.getProjectDeploymentId(),
                projectDeploymentWorkflow.getWorkflowId()),
            workflowUuid);
    }

    private void validateProjectDeploymentWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        if (projectDeploymentWorkflow.isEnabled()) {
            List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections =
                projectDeploymentWorkflow.getConnections();
            Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

            validateProjectDeploymentWorkflowConnections(projectDeploymentWorkflowConnections, workflow);
            validateProjectDeploymentWorkflowInputs(projectDeploymentWorkflow.getInputs(), workflow);
        }
    }

    private void validateProjectDeploymentWorkflowConnections(
        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections, Workflow workflow) {

        for (ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection : projectDeploymentWorkflowConnections) {
            Connection connection = connectionService.getConnection(
                projectDeploymentWorkflowConnection.getConnectionId());

            ComponentConnection componentConnection = componentConnectionFacade.getComponentConnection(
                workflow.getId(), projectDeploymentWorkflowConnection.getWorkflowNodeName(),
                projectDeploymentWorkflowConnection.getWorkflowConnectionKey());

            if (!Objects.equals(connection.getComponentName(), componentConnection.componentName())) {
                throw new ConfigurationException(
                    "Connection component name does not match workflow connection component name",
                    ConnectionErrorType.INVALID_CONNECTION_COMPONENT_NAME);
            }
        }
    }

    static void validateProjectDeploymentWorkflowInputs(Map<String, ?> inputs, Workflow workflow) {
        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Object value = inputs.get(input.name());

                Assert.notNull(value, "Missing required param: " + input.name());

                if (value instanceof String string) {
                    Assert.hasText(string, "Missing required param: " + input.name());
                }
            }
        }
    }
}
