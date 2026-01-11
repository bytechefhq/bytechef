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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
        PrincipalJobFacade principalJobFacade,
        PrincipalJobService principalJobService, JobFacade jobFacade, JobService jobService,
        ProjectDeploymentService projectDeploymentService,
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void enableProjectDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enable) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = doEnableProjectDeploymentWorkflow(
            projectDeploymentId, workflowId, enable);

        projectDeploymentWorkflowService.updateEnabled(projectDeploymentWorkflow.getId(), enable);
    }

    @Override
    public void enableProjectDeploymentWorkflow(
        long projectId, String workflowId, boolean enable, Environment environment) {

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(projectId, environment);

        enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enable);
    }

    @Override
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
    @Transactional(readOnly = true)
    public List<Tag> getProjectDeploymentTags() {
        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments();

        return tagService.getTags(
            projectDeployments.stream()
                .map(ProjectDeployment::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
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

            return CollectionUtils.map(
                projectDeployments,
                projectDeployment -> {
                    Project project = CollectionUtils.getFirst(
                        projects, curProject -> Objects.equals(curProject.getId(), projectDeployment.getProjectId()));

                    List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(
                        projectDeployment.getProjectId(), projectDeployment.getProjectVersion());

                    List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
                        projectDeployment.getProjectId());

                    return new ProjectDeploymentDTO(
                        projectDeployment,
                        CollectionUtils.map(
                            CollectionUtils.filter(
                                projectDeploymentWorkflows,
                                projectDeploymentWorkflow -> Objects.equals(
                                    projectDeploymentWorkflow.getProjectDeploymentId(), projectDeployment.getId()) &&
                                    workflowIds.contains(projectDeploymentWorkflow.getWorkflowId())),
                            projectDeploymentWorkflow -> toProjectDeploymentWorkflowDTO(
                                projectDeploymentWorkflow, projectDeployment, projectWorkflows)),
                        project,
                        getProjectDeploymentLastExecutionDate(Validate.notNull(projectDeployment.getId(), "id")),
                        filterTags(tags, projectDeployment));
                });
        } else {
            return CollectionUtils.map(projectDeployments, ProjectDeploymentDTO::new);
        }
    }

    @Override
    public void updateProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO) {
        updateProjectDeployment(
            projectDeploymentDTO.toProjectDeployment(),
            CollectionUtils.map(
                projectDeploymentDTO.projectDeploymentWorkflows(),
                ProjectDeploymentWorkflowDTO::toProjectDeploymentWorkflow),
            projectDeploymentDTO.tags());
    }

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

    @Override
    public void updateProjectDeploymentTags(long id, List<Tag> tags) {
        projectDeploymentService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
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

                oldProjectDeploymentWorkflow.setConnections(projectDeploymentWorkflow.getConnections());
                oldProjectDeploymentWorkflow.setEnabled(projectDeploymentWorkflow.isEnabled());
                oldProjectDeploymentWorkflow.setInputs(projectDeploymentWorkflow.getInputs());
                oldProjectDeploymentWorkflow.setWorkflowId(projectDeploymentWorkflow.getWorkflowId());

                if (projectDeploymentWorkflow.isEnabled()) {
                    projectDeploymentWorkflowService.update(oldProjectDeploymentWorkflow);

                    if (projectDeployment.isEnabled()) {
                        if (oldProjectDeploymentWorkflow.isEnabled()) {
                            doEnableProjectDeploymentWorkflow(
                                projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), false);
                            doEnableProjectDeploymentWorkflow(
                                projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), true);
                        } else {
                            doEnableProjectDeploymentWorkflow(
                                projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), true);
                        }
                    }
                } else {
                    if (oldProjectDeploymentWorkflow.isEnabled()) {
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
                getWebhookUrl(workflowExecutionId));
        }
    }

    private void stopRunningJobs(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        List<Long> principalIds = List.of(projectDeploymentWorkflow.getProjectDeploymentId());
        List<String> workflowIds = List.of(projectDeploymentWorkflow.getWorkflowId());

        int pageNumber = 0;

        while (true) {
            org.springframework.data.domain.Page<Long> page = principalJobService.getJobIds(
                Job.Status.STARTED, null, null, principalIds, PlatformType.AUTOMATION, workflowIds, pageNumber);

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

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                Objects.requireNonNull(triggerWorkflowNodeType.operation()));

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

    private Instant getWorkflowLastExecutionDate(List<String> workflowIds) {
        return jobService.fetchLastWorkflowJob(workflowIds)
            .map(Job::getEndDate)
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
            getWorkflowLastExecutionDate(workflowUuidWorkflowIds),
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

    private void validateProjectDeploymentWorkflowInputs(Map<String, ?> inputs, Workflow workflow) {
        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Assert.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
                Assert.hasText((String) inputs.get(input.name()), "Missing required param: " + input.name());
            }
        }
    }
}
