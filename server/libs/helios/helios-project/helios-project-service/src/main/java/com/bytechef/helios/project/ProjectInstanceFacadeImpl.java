
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

package com.bytechef.helios.project;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.domain.ProjectInstance;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.job.ProjectInstanceJobFactory;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.hermes.trigger.executor.TriggerLifecycleExecutor;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.transaction.annotation.Propagation;
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

    private final ProjectInstanceJobFactory projectInstanceJobFactory;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final TriggerLifecycleExecutor triggerLifecycleExecutor;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeImpl(
        ProjectInstanceJobFactory projectInstanceJobFactory, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService, TriggerLifecycleExecutor triggerLifecycleExecutor) {

        this.projectInstanceJobFactory = projectInstanceJobFactory;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.triggerLifecycleExecutor = triggerLifecycleExecutor;
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
            projectInstanceDTO.projectInstanceWorkflows());

        return new ProjectInstanceDTO(
            getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())),
            projectInstanceService.create(projectInstance), projectInstanceWorkflows,
            projectService.getProject(projectInstance.getProjectId()), tags);
    }

    @Override
    // Propagation.NEVER is set because of sending job messages via queue in monolith mode, where it can happen
    // the case where a job is finished and completion task executed, but the transaction is not yet committed and
    // the job id is missing.
    @Transactional(propagation = Propagation.NEVER)
    @SuppressFBWarnings("NP")
    public long createProjectInstanceJob(String workflowId, long projectInstanceId) {
        return projectInstanceJobFactory.createJob(workflowId, projectInstanceId);
    }

    @Override
    public void deleteProjectInstance(long projectInstanceId) {
        projectService.delete(projectInstanceId);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @SuppressFBWarnings("NP")
    public ProjectInstanceDTO getProjectInstance(long projectInstanceId) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(projectInstanceId);

        return new ProjectInstanceDTO(
            getLastExecutionDate(Objects.requireNonNull(projectInstance.getId())),
            projectInstance, projectInstanceWorkflowService.getProjectInstanceWorkflows(projectInstanceId),
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
    public ProjectInstanceDTO update(ProjectInstanceDTO projectInstanceDTO) {
        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService.update(
            projectInstanceDTO.projectInstanceWorkflows());
        List<Tag> tags = checkTags(projectInstanceDTO.tags());

        return new ProjectInstanceDTO(
            getLastExecutionDate(projectInstanceDTO.id()),
            projectInstanceService.update(
                projectInstanceDTO.id(), projectInstanceDTO.description(), projectInstanceDTO.name(),
                projectInstanceDTO.status(), CollectionUtils.map(tags, Tag::getId), projectInstanceDTO.version()),
            projectInstanceWorkflows, projectService.getProject(projectInstanceDTO.projectId()),
            tags);
    }

    @Override
    public void updateProjectInstanceTags(Long projectInstanceId, List<Tag> tags) {
        projectInstanceService.update(projectInstanceId, CollectionUtils.map(checkTags(tags), Tag::getId));
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

    private List<Tag> filterTags(List<Tag> tags, ProjectInstance projectInstance) {
        return tags.stream()
            .filter(
                tag -> {
                    List<Long> curTagIds = projectInstance.getTagIds();

                    return curTagIds.contains(tag.getId());
                })
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
}
