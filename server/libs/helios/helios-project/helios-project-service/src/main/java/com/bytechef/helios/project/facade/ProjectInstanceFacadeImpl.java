
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

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.factory.JobFactory;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.project.constant.ProjectConstants;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.domain.ProjectInstance;
import com.bytechef.helios.project.domain.ProjectInstance.Status;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.hermes.workflow.connection.WorkflowConnection;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.coordinator.job.InstanceFacade;
import com.bytechef.hermes.workflow.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.trigger.TriggerLifecycleExecutor;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Transactional
public class ProjectInstanceFacadeImpl implements ProjectInstanceFacade, InstanceFacade {

    private final ConnectionService connectionService;
    private final JobFactory jobFactory;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final TriggerLifecycleExecutor triggerLifecycleExecutor;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeImpl(
        ConnectionService connectionService, JobFactory jobFactory, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService, TriggerLifecycleExecutor triggerLifecycleExecutor, WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.jobFactory = jobFactory;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.triggerLifecycleExecutor = triggerLifecycleExecutor;
        this.workflowService = workflowService;
    }

    @Override
    // Propagation.NEVER is set because of sending job messages via queue in monolith mode, where it can happen
    // the case where a job is finished and completion task executed, but the transaction is not yet committed and
    // the job id is missing.
    @Transactional(propagation = Propagation.NEVER)
    @SuppressFBWarnings("NP")
    public long createJob(long id, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            id, workflowId);

        long jobId = jobFactory.create(new JobParameters(projectInstanceWorkflow.getInputs(), workflowId));

        projectInstanceWorkflowService.addJob(Objects.requireNonNull(projectInstanceWorkflow.getId()), jobId);

        return jobId;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        ProjectInstance projectInstance = projectInstanceDTO.toProjectInstance();
        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        if (!tags.isEmpty()) {
            projectInstance.setTags(tags);
        }

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService.create(
            projectInstanceDTO.projectInstanceWorkflows() == null
                ? List.of()
                : projectInstanceDTO.projectInstanceWorkflows());

        projectInstance = projectInstanceService.create(projectInstance);

        // TODO activate only enabled workflows

        enableWorkflowTriggers(Objects.requireNonNull(projectInstance.getId()), projectInstanceWorkflows);

        return new ProjectInstanceDTO(
            getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())), projectInstance,
            projectInstanceWorkflows, projectService.getProject(projectInstance.getProjectId()), tags);
    }

    @Override
    public void deleteProjectInstance(long id) {
        projectInstanceService.delete(id);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(id);

        disableWorkflowTriggers(id, projectInstanceWorkflows);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void enableProjectInstance(long id, boolean enable) {
        projectInstanceService.update(id, enable ? Status.ENABLED : Status.DISABLED);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(id);

        if (enable) {
            enableWorkflowTriggers(id, projectInstanceWorkflows);
        } else {
            disableWorkflowTriggers(id, projectInstanceWorkflows);
        }
    }

    @Override
    public void enableProjectInstanceWorkflow(long id, String workflowId, boolean enable) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            id, workflowId);

        if (enable) {
            enableWorkflowTriggers(id, List.of(projectInstanceWorkflow));
        } else {
            disableWorkflowTriggers(id, List.of(projectInstanceWorkflow));
        }
    }

    @Override
    public Map<String, Object> getInputs(long id, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            id, workflowId);

        return projectInstanceWorkflow.getInputs();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ProjectInstanceDTO getProjectInstance(long id) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(id);

        return new ProjectInstanceDTO(
            getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())),
            projectInstance, projectInstanceWorkflowService.getProjectInstanceWorkflows(id),
            projectService.getProject(projectInstance.getProjectId()), tagService.getTags(projectInstance.getTagIds()));
    }

    @Override
    public List<Tag> getProjectInstanceTags() {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances();

        return tagService.getTags(
            projectInstances.stream()
                .map(ProjectInstance::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    public List<ProjectInstanceDTO> searchProjectInstances(List<Long> projectIds, List<Long> tagIds) {
        List<ProjectInstance> projectInstances = projectInstanceService.searchProjectInstances(projectIds, tagIds);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(CollectionUtils.map(projectInstances, ProjectInstance::getId));
        List<Project> projects = getProjects(projectInstances);
        List<Tag> tags = getTags(projectInstances);

        return CollectionUtils.map(
            projectInstances,
            projectInstance -> new ProjectInstanceDTO(
                getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())), projectInstance,
                projectInstanceWorkflows.stream()
                    .filter(projectInstanceWorkflow -> Objects.equals(
                        projectInstanceWorkflow.getProjectInstanceId(), projectInstance.getId()))
                    .toList(),
                CollectionUtils.getFirst(
                    projects, project -> Objects.equals(project.getId(), projectInstance.getProjectId())),
                filterTags(tags, projectInstance)));
    }

    @Override
    public ProjectInstanceDTO updateProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService.update(
            projectInstanceDTO.projectInstanceWorkflows());
        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        return new ProjectInstanceDTO(
            getLastExecutionDate(projectInstanceDTO.id()),
            projectInstanceService.update(
                projectInstanceDTO.id(), projectInstanceDTO.description(), projectInstanceDTO.name(),
                projectInstanceDTO.status(), CollectionUtils.map(tags, Tag::getId), projectInstanceDTO.version()),
            projectInstanceWorkflows, projectService.getProject(projectInstanceDTO.projectId()), tags);
    }

    @Override
    public void updateProjectInstanceTags(long id, List<Tag> tags) {
        projectInstanceService.update(id, CollectionUtils.map(checkTags(tags), Tag::getId));
    }

    private LocalDateTime getLastExecutionDate(long projectInstanceId) {
        LocalDateTime lastExecutionDate = null;

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(projectInstanceId);

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            LocalDateTime curLastExecutionDate = projectInstanceWorkflow.getLastExecutionDate();

            if (curLastExecutionDate == null) {
                continue;
            }

            if (lastExecutionDate == null) {
                lastExecutionDate = curLastExecutionDate;

                continue;
            }

            if (curLastExecutionDate.isAfter(lastExecutionDate)) {
                lastExecutionDate = curLastExecutionDate;
            }
        }

        return lastExecutionDate;
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return org.springframework.util.CollectionUtils.isEmpty(tags)
            ? Collections.emptyList()
            : tagService.save(tags);
    }

    private static boolean containsTag(ProjectInstance projectInstance, Tag tag) {
        List<Long> curTagIds = projectInstance.getTagIds();

        return curTagIds.contains(tag.getId());
    }

    private void disableWorkflowTriggers(long id, List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

            List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

            for (WorkflowTrigger workflowTrigger : workflowTriggers) {
                triggerLifecycleExecutor.executeTriggerDisable(
                    workflowTrigger,
                    WorkflowExecutionId.of(workflow.getId(), id, ProjectConstants.PROJECT, workflowTrigger),
                    getConnection(workflowTrigger));
            }
        }
    }

    private void enableWorkflowTriggers(long id, List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

            List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

            for (WorkflowTrigger workflowTrigger : workflowTriggers) {
                triggerLifecycleExecutor.executeTriggerEnable(
                    workflowTrigger,
                    WorkflowExecutionId.of(workflow.getId(), id, ProjectConstants.PROJECT, workflowTrigger),
                    getConnection(workflowTrigger));
            }
        }
    }

    private List<Tag> filterTags(List<Tag> tags, ProjectInstance projectInstance) {
        return tags.stream()
            .filter(tag -> containsTag(projectInstance, tag))
            .toList();
    }

    private Connection getConnection(WorkflowConnection workflowConnection) {
        return workflowConnection.getConnectionId()
            .map(connectionService::getConnection)
            .orElseGet(() -> getConnection(workflowConnection.getKey(), workflowConnection.getTaskName()));
    }

    private Connection getConnection(WorkflowTrigger workflowTrigger) {
        return WorkflowConnection.of(workflowTrigger)
            .values()
            .stream()
            .findFirst()
            .map(this::getConnection)
            .orElse(null);
    }

    private Connection getConnection(String key, String taskName) {
        ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection =
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(key, taskName);

        return connectionService.getConnection(projectInstanceWorkflowConnection.getConnectionId());
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

    @Override
    public String getType() {
        return ProjectConstants.PROJECT;
    }
}
