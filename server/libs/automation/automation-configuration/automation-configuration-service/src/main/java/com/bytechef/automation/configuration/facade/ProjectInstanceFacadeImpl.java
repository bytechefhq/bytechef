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
import com.bytechef.atlas.execution.service.JobService;
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
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
@Service
public class ProjectInstanceFacadeImpl implements ProjectInstanceFacade {

    private final InstanceJobFacade instanceJobFacade;
    private final InstanceJobService instanceJobService;
    private final JobService jobService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final String webhookUrl;
    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeImpl(
        InstanceJobFacade instanceJobFacade, InstanceJobService instanceJobService, JobService jobService,
        ProjectInstanceService projectInstanceService, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        ProjectService projectService, TagService tagService, TriggerLifecycleFacade triggerLifecycleFacade,
        @Value("bytechef.webhookUrl") String webhookUrl, WorkflowConnectionFacade workflowConnectionFacade,
        WorkflowService workflowService) {

        this.instanceJobFacade = instanceJobFacade;
        this.instanceJobService = instanceJobService;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.webhookUrl = webhookUrl;
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        ProjectInstance projectInstance = projectInstanceDTO.toProjectInstance();

        long projectId = Validate.notNull(projectInstance.getProjectId(), "projectId");

        if (!projectService.isProjectEnabled(projectId)) {
            throw new IllegalStateException("Project id=%s is not published".formatted(projectId));
        }

        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        if (!tags.isEmpty()) {
            projectInstance.setTags(tags);
        }

        projectInstance = projectInstanceService.create(projectInstance);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = createProjectInstanceWorkflows(
            CollectionUtils.map(
                projectInstanceDTO.projectInstanceWorkflows(), ProjectInstanceWorkflowDTO::toProjectInstanceWorkflow),
            projectInstance);

        return new ProjectInstanceDTO(
            getProjectInstanceLastExecutionDate(Validate.notNull(projectInstance.getId(), "id")), projectInstance,
            CollectionUtils.map(projectInstanceWorkflows, ProjectInstanceWorkflowDTO::new),
            projectService.getProject(projectInstance.getProjectId()), tags);
    }

    private List<ProjectInstanceWorkflow> createProjectInstanceWorkflows(
        List<ProjectInstanceWorkflow> projectInstanceWorkflows, ProjectInstance projectInstance) {

        projectInstanceWorkflows = projectInstanceWorkflows == null
            ? List.of()
            : projectInstanceWorkflows;

        projectInstanceWorkflows = projectInstanceWorkflowService.create(
            projectInstanceWorkflows.stream()
                .peek(projectInstanceWorkflow -> projectInstanceWorkflow.setProjectInstanceId(projectInstance.getId()))
                .toList());
        return projectInstanceWorkflows;
    }

    @Override
    public long createProjectInstanceWorkflowJob(Long id, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            id, workflowId);

        Job job = instanceJobFacade.createJob(
            new JobParameters(workflowId, projectInstanceWorkflow.getInputs()), id, Type.AUTOMATION);

        return Validate.notNull(job.getId(), "id");
    }

    @Override
    public void deleteProjectInstance(long id) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(id);

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            if (projectInstanceWorkflow.isEnabled()) {
                throw new IllegalStateException(
                    "The ProjectInstanceWorkflow instance id=%s must be disabled.".formatted(
                        projectInstanceWorkflow.getId()));
            }

            projectInstanceWorkflowService.delete(projectInstanceWorkflow.getId());
        }

        projectInstanceService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
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

        return new ProjectInstanceDTO(
            getProjectInstanceLastExecutionDate(Validate.notNull(projectInstance.getId(), "id")),
            projectInstance,
            CollectionUtils.map(
                projectInstanceWorkflowService.getProjectInstanceWorkflows(id),
                ProjectInstanceWorkflowDTO::new),
            projectService.getProject(projectInstance.getProjectId()), tagService.getTags(projectInstance.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectInstanceTags() {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances();

        return tagService.getTags(
            projectInstances.stream()
                .map(ProjectInstance::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInstanceDTO> getProjectInstances(Long projectId, Long tagId) {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(projectId, tagId);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(CollectionUtils.map(projectInstances, ProjectInstance::getId));
        List<Project> projects = getProjects(projectInstances);
        List<Tag> tags = getTags(projectInstances);

        return CollectionUtils.map(
            projectInstances,
            projectInstance -> new ProjectInstanceDTO(
                getProjectInstanceLastExecutionDate(Validate.notNull(projectInstance.getId(), "id")), projectInstance,
                CollectionUtils.map(
                    CollectionUtils.filter(
                        projectInstanceWorkflows,
                        projectInstanceWorkflow -> Objects.equals(
                            projectInstanceWorkflow.getProjectInstanceId(), projectInstance.getId())),
                    projectInstanceWorkflow -> new ProjectInstanceWorkflowDTO(
                        projectInstanceWorkflow,
                        getWorkflowLastExecutionDate(projectInstanceWorkflow.getWorkflowId()))),
                CollectionUtils.getFirst(
                    projects, project -> Objects.equals(project.getId(), projectInstance.getProjectId())),
                filterTags(tags, projectInstance)));
    }

    @Override
    public ProjectInstanceDTO updateProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService.update(
            CollectionUtils.map(
                projectInstanceDTO.projectInstanceWorkflows(), ProjectInstanceWorkflowDTO::toProjectInstanceWorkflow));
        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        return new ProjectInstanceDTO(
            getProjectInstanceLastExecutionDate(projectInstanceDTO.id()),
            projectInstanceService.update(projectInstanceDTO.toProjectInstance()),
            CollectionUtils.map(projectInstanceWorkflows, ProjectInstanceWorkflowDTO::new),
            projectService.getProject(projectInstanceDTO.projectId()), tags);
    }

    @Override
    public void updateProjectInstanceTags(long id, List<Tag> tags) {
        projectInstanceService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    @Override
    public ProjectInstanceWorkflow updateProjectInstanceWorkflow(ProjectInstanceWorkflow projectInstanceWorkflow) {
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
            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), Type.AUTOMATION, projectInstanceWorkflow.getProjectInstanceId(),
                workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getParameters(),
                getConnectionId(projectInstanceWorkflow.getProjectInstanceId(), workflow.getId(), workflowTrigger));
        }
    }

    private void enableWorkflowTriggers(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        validate(projectInstanceWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), Type.AUTOMATION, projectInstanceWorkflow.getProjectInstanceId(),
                workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getParameters(),
                getConnectionId(
                    projectInstanceWorkflow.getProjectInstanceId(), workflow.getId(), workflowTrigger),
                webhookUrl);
        }
    }

    private List<Tag> filterTags(List<Tag> tags, ProjectInstance projectInstance) {
        return CollectionUtils.filter(tags, tag -> containsTag(projectInstance, tag));
    }

    private Long getConnectionId(long projectInstanceId, String workflowId, WorkflowConnection workflowConnection) {
        return workflowConnection.getId()
            .orElseGet(() -> getConnectionId(
                projectInstanceId, workflowId, workflowConnection.getOperationName(), workflowConnection.getKey()));
    }

    private Long getConnectionId(long projectInstanceId, String workflowId, WorkflowTrigger workflowTrigger) {
        return workflowConnectionFacade.getWorkflowConnections(workflowTrigger)
            .stream()
            .findFirst()
            .map(workflowConnection -> getConnectionId(projectInstanceId, workflowId, workflowConnection))
            .orElse(null);
    }

    private Long getConnectionId(
        long projectInstanceId, String workflowId, String workflowConnectionOperationName,
        String workflowConnectionKey) {

        ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection =
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(
                projectInstanceId, workflowId, workflowConnectionOperationName, workflowConnectionKey);

        return projectInstanceWorkflowConnection.getConnectionId();
    }

    private LocalDateTime getProjectInstanceLastExecutionDate(long projectInstanceId) {
        return OptionalUtils.mapOrElse(
            instanceJobService.fetchLastJobId(projectInstanceId, Type.AUTOMATION),
            jobId -> {
                Job job = jobService.getJob(jobId);

                return job.getEndDate();
            },
            null);
    }

    private List<Project> getProjects(List<ProjectInstance> projectInstances) {
        return projectService.getProjects(
            projectInstances.stream()
                .map(ProjectInstance::getProjectId)
                .filter(Objects::nonNull)
                .toList());
    }

    private List<Tag> getTags(List<ProjectInstance> projectInstances) {
        return tagService.getTags(
            projectInstances.stream()
                .flatMap(projectInstance -> CollectionUtils.stream(projectInstance.getTagIds()))
                .filter(Objects::nonNull)
                .toList());
    }

    private LocalDateTime getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(
            jobService.fetchLastWorkflowJob(workflowId),
            Job::getEndDate,
            null);
    }

    private static void validate(Map<String, ?> inputs, Workflow workflow) {
        // validate inputs

        for (Workflow.Input input : workflow.getInputs()) {
            if (input.required()) {
                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
            }
        }
    }
}
