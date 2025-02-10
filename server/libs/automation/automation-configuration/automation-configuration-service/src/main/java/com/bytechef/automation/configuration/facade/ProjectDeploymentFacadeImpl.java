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
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.exception.ConfigurationException;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
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
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectDeploymentFacadeImpl implements ProjectDeploymentFacade {

    private final ConnectionService connectionService;
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
        ConnectionService connectionService, PrincipalJobFacade principalJobFacade,
        PrincipalJobService principalJobService,
        JobFacade jobFacade, JobService jobService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService,
        TriggerDefinitionService triggerDefinitionService, TriggerExecutionService triggerExecutionService,
        TriggerLifecycleFacade triggerLifecycleFacade, ApplicationProperties applicationProperties,
        ComponentConnectionFacade componentConnectionFacade, WorkflowService workflowService) {

        this.connectionService = connectionService;
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
        ProjectDeployment projectDeployment = projectDeploymentDTO.toProjectDeployment();

        long projectId = Validate.notNull(projectDeployment.getProjectId(), "projectId");

        Project project = projectService.getProject(projectId);

        if (!project.isPublished()) {
            throw new ConfigurationException(
                "Project id=%s is not published".formatted(projectId),
                ProjectDeploymentErrorType.CREATE_PROJECT_DEPLOYMENT);
        }

        if (project.getLastVersion() == projectDeployment.getProjectVersion()) {
            throw new ConfigurationException(
                "Project version v=%s cannot be in DRAFT".formatted(projectDeployment.getProjectVersion()),
                ProjectDeploymentErrorType.CREATE_PROJECT_DEPLOYMENT);
        }

        List<Tag> tags = checkTags(projectDeploymentDTO.tags());

        if (!tags.isEmpty()) {
            projectDeployment.setTags(tags);
        }

        projectDeployment = projectDeploymentService.create(projectDeployment);

        checkProjectDeploymentWorkflows(
            projectDeployment, -1,
            CollectionUtils.map(
                projectDeploymentDTO.projectDeploymentWorkflows(),
                ProjectDeploymentWorkflowDTO::toProjectDeploymentWorkflow),
            List.of());

        return projectDeployment.getId();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createProjectDeploymentWorkflowJob(Long id, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                id, workflowId);

        return principalJobFacade.createJob(
            new JobParametersDTO(workflowId, projectDeploymentWorkflow.getInputs()), id, ModeType.AUTOMATION);
    }

    @Override
    public void deleteProjectDeployment(long id) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(id);

        if (projectDeployment.isEnabled()) {
            enableProjectDeployment(projectDeployment.getId(), false);
        }

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(id);

        List<Long> jobIds = principalJobService.getJobIds(id, ModeType.AUTOMATION);

        for (long jobId : jobIds) {
            triggerExecutionService.deleteJobTriggerExecution(jobId);

            principalJobService.deletePrincipalJobs(jobId, ModeType.AUTOMATION);

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
    @Transactional(readOnly = true)
    public ProjectDeploymentDTO getProjectDeployment(long id) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(id);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion());
        List<String> workflowIds = projectWorkflowService.getWorkflowIds(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion());

        return new ProjectDeploymentDTO(
            projectDeployment,
            CollectionUtils.map(
                CollectionUtils.filter(
                    projectDeploymentWorkflowService.getProjectDeploymentWorkflows(id),
                    projectDeploymentWorkflow -> workflowIds.contains(projectDeploymentWorkflow.getWorkflowId())),
                projectDeploymentWorkflow -> new ProjectDeploymentWorkflowDTO(
                    projectDeploymentWorkflow,
                    getWorkflowLastExecutionDate(projectDeploymentWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        projectDeploymentWorkflow.getProjectDeploymentId(), projectDeploymentWorkflow.getWorkflowId()),
                    getWorkflowReferenceCode(
                        projectDeploymentWorkflow.getWorkflowId(), projectDeployment.getProjectVersion(),
                        projectWorkflows))),
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
        long id, Environment environment, Long projectId, Long tagId, boolean includeAllFields) {

        return getProjectDeployments(id, environment, projectId, tagId, includeAllFields);
    }

    @Override
    public void updateProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO) {
        ProjectDeployment projectDeployment = projectDeploymentDTO.toProjectDeployment();

        List<Tag> tags = checkTags(projectDeploymentDTO.tags());

        if (!tags.isEmpty()) {
            projectDeployment.setTags(tags);
        }

        ProjectDeployment oldProjectDeployment =
            projectDeploymentService.getProjectDeployment(projectDeployment.getId());

        projectDeploymentService.update(projectDeployment);

        checkProjectDeploymentWorkflows(
            projectDeployment, oldProjectDeployment.getProjectVersion(),
            CollectionUtils.map(
                projectDeploymentDTO.projectDeploymentWorkflows(),
                ProjectDeploymentWorkflowDTO::toProjectDeploymentWorkflow),
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
                String workflowReferenceCode = allProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(
                        curProjectWorkflow.getWorkflowId(), projectDeploymentWorkflow.getWorkflowId()))
                    .findFirst()
                    .map(ProjectWorkflow::getWorkflowReferenceCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Project workflow with workflowId=%s not found".formatted(
                            projectDeploymentWorkflow.getWorkflowId())));

                String oldWorkflowId = allProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(
                        curProjectWorkflow.getWorkflowReferenceCode(), workflowReferenceCode) &&
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

            if (oldProjectDeploymentWorkflow == null) {
                validateProjectDeploymentWorkflow(projectDeploymentWorkflow);

                projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());

                projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

                if (projectDeployment.isEnabled() && projectDeploymentWorkflow.isEnabled()) {
                    enableProjectDeploymentWorkflow(
                        projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), true);
                }
            } else {
                boolean oldProjectDeploymentWorkflowEnabled = oldProjectDeploymentWorkflow.isEnabled();

                validateProjectDeploymentWorkflow(projectDeploymentWorkflow);

                String oldWorkflowId = oldProjectDeploymentWorkflow.getWorkflowId();

                oldProjectDeploymentWorkflow.setConnections(projectDeploymentWorkflow.getConnections());
                oldProjectDeploymentWorkflow.setEnabled(projectDeploymentWorkflow.isEnabled());
                oldProjectDeploymentWorkflow.setInputs(projectDeploymentWorkflow.getInputs());
                oldProjectDeploymentWorkflow.setWorkflowId(projectDeploymentWorkflow.getWorkflowId());

                if (projectDeploymentWorkflow.isEnabled()) {
                    projectDeploymentWorkflowService.update(oldProjectDeploymentWorkflow);

                    if (projectDeployment.isEnabled() && !oldProjectDeploymentWorkflowEnabled) {
                        validateProjectDeploymentWorkflow(projectDeploymentWorkflow);

                        doEnableProjectDeploymentWorkflow(
                            projectDeployment.getId(), projectDeploymentWorkflow.getWorkflowId(), true);
                    }
                } else {
                    if (oldProjectDeploymentWorkflowEnabled) {
                        doEnableProjectDeploymentWorkflow(projectDeployment.getId(), oldWorkflowId, false);
                    }

                    projectDeploymentWorkflowService.update(oldProjectDeploymentWorkflow);
                }
            }
        }

        for (ProjectDeploymentWorkflow oldProjectDeploymentWorkflow : oldProjectDeploymentWorkflows) {
            String workflowReferenceCode = allProjectWorkflows.stream()
                .filter(curProjectWorkflow -> Objects.equals(
                    curProjectWorkflow.getWorkflowId(), oldProjectDeploymentWorkflow.getWorkflowId()))
                .findFirst()
                .map(ProjectWorkflow::getWorkflowReferenceCode)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Project workflow with workflowId=%s not found".formatted(
                        oldProjectDeploymentWorkflow.getWorkflowId())));

            String workflowId = allProjectWorkflows.stream()
                .filter(curProjectWorkflow -> Objects.equals(
                    curProjectWorkflow.getWorkflowReferenceCode(), workflowReferenceCode) &&
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
                ModeType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                projectWorkflow.getWorkflowReferenceCode(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
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

            if (requiredComponentConnections.size() != projectDeploymentWorkflow.getConnectionsCount()) {
                throw new ConfigurationException(
                    "Not all required connections are set for a workflow with id=%s".formatted(workflow.getId()),
                    ProjectDeploymentErrorType.REQUIRED_WORKFLOW_CONNECTIONS);
            }
        }

        if (projectDeployment.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(projectDeploymentWorkflow);
            } else {
                disableWorkflowTriggers(projectDeploymentWorkflow);
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

            if (Objects.equals(workflowNodeType.componentName(), "manual")) {
                continue;
            }

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                ModeType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                projectWorkflow.getWorkflowReferenceCode(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), workflowExecutionId, workflowNodeType, workflowTrigger.getParameters(),
                getConnectionId(projectDeploymentWorkflow.getProjectDeploymentId(), workflow.getId(), workflowTrigger),
                getWebhookUrl(workflowExecutionId));
        }
    }

    private List<Tag> filterTags(List<Tag> tags, ProjectDeployment projectDeployment) {
        return CollectionUtils.filter(tags, tag -> containsTag(projectDeployment, tag));
    }

    private Long getConnectionId(long projectDeploymentId, String workflowId, WorkflowTrigger workflowTrigger) {
        return componentConnectionFacade
            .getComponentConnections(workflowTrigger)
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

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private Instant getProjectDeploymentLastExecutionDate(long projectDeploymentId) {
        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastJobId(projectDeploymentId, ModeType.AUTOMATION), this::getJobEndDate, null);
    }

    private List<ProjectDeploymentDTO> getProjectDeployments(
        Long workspaceId, Environment environment, Long projectId, Long tagId, boolean includeAllFields) {

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            workspaceId, environment, projectId, tagId, List.of());

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

                    List<String> workflowIds = projectWorkflowService.getWorkflowIds(
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
                            projectDeploymentWorkflow -> new ProjectDeploymentWorkflowDTO(
                                projectDeploymentWorkflow,
                                getWorkflowLastExecutionDate(projectDeploymentWorkflow.getWorkflowId()),
                                getStaticWebhookUrl(
                                    projectDeploymentWorkflow.getProjectDeploymentId(),
                                    projectDeploymentWorkflow.getWorkflowId()),
                                getWorkflowReferenceCode(
                                    projectDeploymentWorkflow.getWorkflowId(), projectDeployment.getProjectVersion(),
                                    projectWorkflows))),
                        project,
                        getProjectDeploymentLastExecutionDate(Validate.notNull(projectDeployment.getId(), "id")),
                        filterTags(tags, projectDeployment));
                });
        } else {
            return CollectionUtils.map(projectDeployments, ProjectDeploymentDTO::new);
        }
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
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName());

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
                !Objects.equals(triggerDefinition.getName(), "manual")) {

                ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

                return getWebhookUrl(
                    WorkflowExecutionId.of(
                        ModeType.AUTOMATION, projectDeploymentId, projectWorkflow.getWorkflowReferenceCode(),
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

    private Instant getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(workflowId), Job::getEndDate, null);
    }

    private String getWorkflowReferenceCode(
        String workflowId, int projectVersion, List<ProjectWorkflow> projectWorkflows) {

        return projectWorkflows.stream()
            .filter(projectWorkflow -> Objects.equals(projectWorkflow.getWorkflowId(), workflowId) &&
                projectWorkflow.getProjectVersion() == projectVersion)
            .findFirst()
            .map(ProjectWorkflow::getWorkflowReferenceCode)
            .orElseThrow();
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
                projectDeploymentWorkflowConnection.getKey());

            if (!Objects.equals(connection.getComponentName(), componentConnection.componentName())) {
                throw new IllegalArgumentException(
                    "Connection component name does not match workflow connection component name");
            }
        }
    }

    private void validateProjectDeploymentWorkflowInputs(Map<String, ?> inputs, Workflow workflow) {
        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
                Validate.notEmpty((String) inputs.get(input.name()), "Missing required param: " + input.name());
            }
        }
    }
}
