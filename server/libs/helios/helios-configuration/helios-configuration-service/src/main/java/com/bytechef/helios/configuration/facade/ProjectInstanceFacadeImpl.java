
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

package com.bytechef.helios.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.domain.ProjectInstance;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.dto.ProjectInstanceDTO;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.helios.configuration.connection.WorkflowConnection;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Transactional
public class ProjectInstanceFacadeImpl implements ProjectInstanceFacade {

    private final JobFacade jobFacade;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final String webhookUrl;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeImpl(
        JobFacade jobFacade, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService, TriggerLifecycleFacade triggerLifecycleFacade, String webhookUrl,
        WorkflowService workflowService) {

        this.jobFacade = jobFacade;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.webhookUrl = webhookUrl;
        this.workflowService = workflowService;
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

        return new ProjectInstanceDTO(
            getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())), projectInstance,
            projectInstanceWorkflows, projectService.getProject(projectInstance.getProjectId()), tags);
    }

    @Override
    public void createProjectInstanceWorkflowJob(Long id, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            id, workflowId);

        jobFacade.createJob(new JobParameters(workflowId, projectInstanceWorkflow.getInputs()));
    }

    @Override
    public void deleteProjectInstance(long id) {
        projectInstanceService.delete(id);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(id);

        for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
            if (projectInstanceWorkflow.isEnabled()) {
                throw new IllegalStateException(
                    "ProjectInstanceWorkflow with id=%s must be disabled.".formatted(projectInstanceWorkflow.getId()));
            }
        }

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
                enableWorkflowTrigger(projectInstanceWorkflow);
            } else {
                disableWorkflowTrigger(projectInstanceWorkflow);
            }
        }

        projectInstanceService.updateEnabled(projectInstanceId, enable);
    }

    @Override
    public void enableProjectInstanceWorkflow(long projectInstanceId, String workflowId, boolean enable) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            projectInstanceId, workflowId);

        if (enable) {
            enableWorkflowTrigger(projectInstanceWorkflow);
        } else {
            disableWorkflowTrigger(projectInstanceWorkflow);
        }

        projectInstanceWorkflowService.updateEnabled(projectInstanceWorkflow.getId(), enable);
    }

    @Override
    @SuppressFBWarnings("NP")
    @Transactional(readOnly = true)
    public ProjectInstanceDTO getProjectInstance(long id) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(id);

        return new ProjectInstanceDTO(
            getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())),
            projectInstance, projectInstanceWorkflowService.getProjectInstanceWorkflows(id),
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
            projectInstanceService.update(projectInstanceDTO.toProjectInstance()),
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

    private List<Tag> filterTags(List<Tag> tags, ProjectInstance projectInstance) {
        return tags.stream()
            .filter(tag -> containsTag(projectInstance, tag))
            .toList();
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

    //

    private void disableWorkflowTrigger(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), projectInstanceWorkflow.getProjectInstanceId(), ProjectConstants.PROJECT,
                workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getParameters(),
                getConnectionId(workflowTrigger));
        }
    }

    private void enableWorkflowTrigger(ProjectInstanceWorkflow projectInstanceWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectInstanceWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            triggerLifecycleFacade.executeTriggerEnable(
                workflow.getId(), projectInstanceWorkflow.getProjectInstanceId(), ProjectConstants.PROJECT,
                workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getParameters(),
                getConnectionId(workflowTrigger), webhookUrl);
        }
    }

    private Long getConnectionId(WorkflowConnection workflowConnection) {
        return workflowConnection.getId()
            .orElseGet(() -> getConnectionId(workflowConnection.getOperationName(), workflowConnection.getKey()));
    }

    private Long getConnectionId(WorkflowTrigger workflowTrigger) {
        return WorkflowConnection.of(workflowTrigger)
            .stream()
            .findFirst()
            .map(this::getConnectionId)
            .orElse(null);
    }

    private Long getConnectionId(String workflowConnectionOperationName, String workflowConnectionKey) {
        ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection =
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(
                workflowConnectionOperationName, workflowConnectionKey);

        return projectInstanceWorkflowConnection.getConnectionId();
    }
}
