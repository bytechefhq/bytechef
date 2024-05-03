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
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.constant.ProjectInstanceErrorType;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.automation.configuration.dto.ProjectInstanceDTO;
import com.bytechef.automation.configuration.dto.ProjectInstanceWorkflowDTO;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectInstanceFacadeImpl implements ProjectInstanceFacade {

    private final ConnectionService connectionService;
    private final InstanceJobFacade instanceJobFacade;
    private final InstanceJobService instanceJobService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerExecutionService triggerExecutionService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final String webhookUrl;
    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeImpl(
        ConnectionService connectionService, InstanceJobFacade instanceJobFacade, InstanceJobService instanceJobService,
        JobFacade jobFacade, JobService jobService, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService, TriggerDefinitionService triggerDefinitionService,
        TriggerExecutionService triggerExecutionService, TriggerLifecycleFacade triggerLifecycleFacade,
        @Value("${bytechef.webhook-url}") String webhookUrl,
        WorkflowConnectionFacade workflowConnectionFacade, WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.instanceJobFacade = instanceJobFacade;
        this.instanceJobService = instanceJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.webhookUrl = webhookUrl;
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        ProjectInstance projectInstance = projectInstanceDTO.toProjectInstance();

        long projectId = Validate.notNull(projectInstance.getProjectId(), "projectId");

        Project project = projectService.getProject(Validate.notNull(projectInstance.getProjectId(), "projectId"));

        if (!project.isPublished()) {
            throw new ConfigurationException(
                "Project id=%s is not published".formatted(projectId),
                ProjectInstanceErrorType.CREATE_PROJECT_INSTANCE);
        }

        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        if (!tags.isEmpty()) {
            projectInstance.setTags(tags);
        }

        projectInstance = projectInstanceService.create(projectInstance);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = createProjectInstanceWorkflows(
            projectInstance, CollectionUtils.map(
                projectInstanceDTO.projectInstanceWorkflows(), ProjectInstanceWorkflowDTO::toProjectInstanceWorkflow));

        return new ProjectInstanceDTO(
            projectInstance,
            CollectionUtils.map(
                projectInstanceWorkflows,
                projectInstanceWorkflow -> new ProjectInstanceWorkflowDTO(
                    projectInstanceWorkflow, getWorkflowLastExecutionDate(projectInstanceWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        projectInstanceWorkflow.getProjectInstanceId(), projectInstanceWorkflow.getWorkflowId()))),
            projectService.getProject(projectInstance.getProjectId()),
            getProjectInstanceLastExecutionDate(Validate.notNull(projectInstance.getId(), "id")), tags);
    }

    private List<ProjectInstanceWorkflow> createProjectInstanceWorkflows(
        ProjectInstance projectInstance, List<ProjectInstanceWorkflow> projectInstanceWorkflows) {

        projectInstanceWorkflows = projectInstanceWorkflowService.create(
            projectInstanceWorkflows
                .stream()
                .peek(projectInstanceWorkflow -> {
                    if (projectInstanceWorkflow.isEnabled()) {
                        List<ProjectInstanceWorkflowConnection> projectInstanceWorkflowConnections =
                            projectInstanceWorkflow.getConnections();
                        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

                        validateConnections(projectInstanceWorkflowConnections, workflow);
                        validateInputs(projectInstanceWorkflow.getInputs(), workflow);
                    }

                    projectInstanceWorkflow.setProjectInstanceId(Validate.notNull(projectInstance.getId(), "id"));
                })
                .toList());

        return projectInstanceWorkflows;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long createProjectInstanceWorkflowJob(Long id, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            id, workflowId);

        return instanceJobFacade.createJob(
            new JobParameters(workflowId, projectInstanceWorkflow.getInputs()), id, Type.AUTOMATION);
    }

    @Override
    public void deleteProjectInstance(long id) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(id);

        if (projectInstance.isEnabled()) {
            throw new ConfigurationException(
                "Project instance id=%s is enabled".formatted(id), ProjectInstanceErrorType.DELETE_PROJECT_INSTANCE);
        }

        List<ProjectInstanceWorkflow> projectInstanceWorkflows =
            projectInstanceWorkflowService.getProjectInstanceWorkflows(id);

        List<Long> jobIds = instanceJobService.getJobIds(id, Type.AUTOMATION);

        for (long jobId : jobIds) {
            triggerExecutionService.deleteJobTriggerExecution(jobId);

            instanceJobService.deleteInstanceJobs(jobId, Type.AUTOMATION);

            jobFacade.deleteJob(jobId);
        }

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            projectInstanceWorkflowService.delete(projectInstanceWorkflow.getId());
        }

        projectInstanceService.delete(id);

// TODO find a way to delete all tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void enableProjectInstance(long projectInstanceId, boolean enable) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(projectInstanceId);

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            if (!projectInstanceWorkflow.isEnabled()) {
                continue;
            }

            if (enable) {
                enableWorkflowTriggers(projectInstanceWorkflow);
            } else {
                disableWorkflowTriggers(projectInstanceWorkflow);
            }
        }

        projectInstanceService.updateEnabled(projectInstanceId, enable);
    }

    @Override
    public void enableProjectInstanceWorkflow(long projectInstanceId, String workflowId, boolean enable) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            projectInstanceId, workflowId);

        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(
            projectInstanceWorkflow.getProjectInstanceId());

        if (projectInstance.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(projectInstanceWorkflow);
            } else {
                disableWorkflowTriggers(projectInstanceWorkflow);
            }
        }

        projectInstanceWorkflowService.updateEnabled(projectInstanceWorkflow.getId(), enable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectInstanceDTO getProjectInstance(long id) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(id);

        Project project = projectService.getProject(projectInstance.getProjectId());

        List<String> workflowIds = project.getWorkflowIds(projectInstance.getProjectVersion());

        return new ProjectInstanceDTO(
            projectInstance,
            CollectionUtils.map(
                CollectionUtils.filter(
                    projectInstanceWorkflowService.getProjectInstanceWorkflows(id),
                    projectInstanceWorkflow -> workflowIds.contains(projectInstanceWorkflow.getWorkflowId())),
                projectInstanceWorkflow -> new ProjectInstanceWorkflowDTO(
                    projectInstanceWorkflow,
                    getWorkflowLastExecutionDate(projectInstanceWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        projectInstanceWorkflow.getProjectInstanceId(), projectInstanceWorkflow.getWorkflowId()))),
            projectService.getProject(projectInstance.getProjectId()),
            getProjectInstanceLastExecutionDate(Validate.notNull(projectInstance.getId(), "id")),
            tagService.getTags(projectInstance.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectInstanceTags() {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances();

        return tagService.getTags(
            projectInstances
                .stream()
                .map(ProjectInstance::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInstanceDTO> getProjectInstances(Environment environment, Long projectId, Long tagId) {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(
            environment, projectId, tagId);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(CollectionUtils.map(projectInstances, ProjectInstance::getId));
        List<Project> projects = getProjects(projectInstances);
        List<Tag> tags = getTags(projectInstances);

        return CollectionUtils.map(
            projectInstances,
            projectInstance -> {
                Project project = CollectionUtils.getFirst(
                    projects, curProject -> Objects.equals(curProject.getId(), projectInstance.getProjectId()));

                List<String> workflowIds = project.getWorkflowIds(projectInstance.getProjectVersion());

                return new ProjectInstanceDTO(
                    projectInstance,
                    CollectionUtils.map(
                        CollectionUtils.filter(
                            projectInstanceWorkflows,
                            projectInstanceWorkflow -> Objects.equals(
                                projectInstanceWorkflow.getProjectInstanceId(), projectInstance.getId()) &&
                                workflowIds.contains(projectInstanceWorkflow.getWorkflowId())),
                        projectInstanceWorkflow -> new ProjectInstanceWorkflowDTO(
                            projectInstanceWorkflow,
                            getWorkflowLastExecutionDate(projectInstanceWorkflow.getWorkflowId()),
                            getStaticWebhookUrl(
                                projectInstanceWorkflow.getProjectInstanceId(),
                                projectInstanceWorkflow.getWorkflowId()))),
                    project, getProjectInstanceLastExecutionDate(Validate.notNull(projectInstance.getId(), "id")),
                    filterTags(tags, projectInstance));
            });
    }

    @Override
    public ProjectInstanceDTO updateProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceDTO
            .projectInstanceWorkflows()
            .stream()
            .map(ProjectInstanceWorkflowDTO::toProjectInstanceWorkflow)
            .peek(this::validateInputs)
            .toList();

        projectInstanceWorkflows = projectInstanceWorkflowService.update(projectInstanceWorkflows);

        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        return new ProjectInstanceDTO(
            projectInstanceService.update(projectInstanceDTO.toProjectInstance()),
            CollectionUtils.map(
                projectInstanceWorkflows,
                projectInstanceWorkflow -> new ProjectInstanceWorkflowDTO(
                    projectInstanceWorkflow,
                    getWorkflowLastExecutionDate(projectInstanceWorkflow.getWorkflowId()),
                    getStaticWebhookUrl(
                        projectInstanceWorkflow.getProjectInstanceId(), projectInstanceWorkflow.getWorkflowId()))),
            projectService.getProject(projectInstanceDTO.projectId()),
            getProjectInstanceLastExecutionDate(projectInstanceDTO.id()), tags);
    }

    @Override
    public void updateProjectInstanceTags(long id, List<Tag> tags) {
        projectInstanceService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
    public ProjectInstanceWorkflow updateProjectInstanceWorkflow(ProjectInstanceWorkflow projectInstanceWorkflow) {
        validateInputs(projectInstanceWorkflow);

        return projectInstanceWorkflowService.update(projectInstanceWorkflow);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private static boolean containsTag(ProjectInstance projectInstance, Tag tag) {
        List<Long> tagIds = projectInstance.getTagIds();

        return tagIds.contains(tag.getId());
    }

    private void disableWorkflowTriggers(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                Type.AUTOMATION, projectInstanceWorkflow.getProjectInstanceId(), workflow.getId(),
                workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
                getConnectionId(projectInstanceWorkflow.getProjectInstanceId(), workflow.getId(), workflowTrigger));
        }
    }

    private void enableWorkflowTriggers(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        validateInputs(projectInstanceWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                Type.AUTOMATION, projectInstanceWorkflow.getProjectInstanceId(), workflow.getId(),
                workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.getParameters(),
                getConnectionId(projectInstanceWorkflow.getProjectInstanceId(), workflow.getId(), workflowTrigger),
                getWebhookUrl(workflowExecutionId));
        }
    }

    private List<Tag> filterTags(List<Tag> tags, ProjectInstance projectInstance) {
        return CollectionUtils.filter(tags, tag -> containsTag(projectInstance, tag));
    }

    private Long getConnectionId(long projectInstanceId, String workflowId, WorkflowTrigger workflowTrigger) {
        return workflowConnectionFacade
            .getWorkflowConnections(workflowTrigger)
            .stream()
            .findFirst()
            .map(workflowConnection -> getConnectionId(
                projectInstanceId, workflowId, workflowConnection.workflowNodeName(), workflowConnection.key()))
            .orElse(null);
    }

    private Long getConnectionId(
        long projectInstanceId, String workflowId, String workflowNodeName, String workflowConnectionKey) {

        return projectInstanceWorkflowService
            .fetchProjectInstanceWorkflowConnection(
                projectInstanceId, workflowId, workflowNodeName, workflowConnectionKey)
            .map(ProjectInstanceWorkflowConnection::getConnectionId)
            .orElse(null);
    }

    private LocalDateTime getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private LocalDateTime getProjectInstanceLastExecutionDate(long projectInstanceId) {
        return OptionalUtils.mapOrElse(
            instanceJobService.fetchLastJobId(projectInstanceId, Type.AUTOMATION), this::getJobEndDate, null);
    }

    private List<Project> getProjects(List<ProjectInstance> projectInstances) {
        return projectService.getProjects(
            projectInstances
                .stream()
                .map(ProjectInstance::getProjectId)
                .filter(Objects::nonNull)
                .toList());
    }

    private String getStaticWebhookUrl(long projectInstanceId, String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerWorkflowNodeType.componentName(), triggerWorkflowNodeType.componentVersion(),
                triggerWorkflowNodeType.componentOperationName());

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
                !Objects.equals(triggerDefinition.getName(), "manual")) {

                return getWebhookUrl(
                    WorkflowExecutionId.of(
                        Type.AUTOMATION, projectInstanceId, workflow.getId(), workflowTrigger.getName()));
            }
        }

        return null;
    }

    private List<Tag> getTags(List<ProjectInstance> projectInstances) {
        return tagService.getTags(
            projectInstances
                .stream()
                .flatMap(projectInstance -> CollectionUtils.stream(projectInstance.getTagIds()))
                .filter(Objects::nonNull)
                .toList());
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl.replace("{id}", workflowExecutionId.toString());
    }

    private LocalDateTime getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(
            jobService.fetchLastWorkflowJob(workflowId),
            Job::getEndDate,
            null);
    }

    private void validateConnections(
        List<ProjectInstanceWorkflowConnection> projectInstanceWorkflowConnections, Workflow workflow) {

        for (ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection : projectInstanceWorkflowConnections) {
            Connection connection = connectionService.getConnection(
                projectInstanceWorkflowConnection.getConnectionId());

            WorkflowConnection workflowConnection = workflowConnectionFacade.getWorkflowConnection(
                workflow, projectInstanceWorkflowConnection.getWorkflowNodeName(),
                projectInstanceWorkflowConnection.getKey());

            if (!Objects.equals(connection.getComponentName(), workflowConnection.componentName())) {
                throw new IllegalArgumentException(
                    "Connection component name does not match workflow connection component name");
            }
        }
    }

    private void validateInputs(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        validateInputs(projectInstanceWorkflow.getInputs(), workflow);
    }

    private void validateInputs(Map<String, ?> inputs, Workflow workflow) {
        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
                Validate.notEmpty((String) inputs.get(input.name()), "Missing required param: " + input.name());
            }
        }
    }
}
