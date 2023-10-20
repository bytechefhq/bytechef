
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

import com.bytechef.atlas.job.JobParameters;
import com.bytechef.atlas.job.JobFactory;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.domain.ProjectInstance;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.project.service.ProjectService;
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

    private final JobFactory jobFactory;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectService projectService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeImpl(
        JobFactory jobFactory, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService) {

        this.jobFactory = jobFactory;
        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectService = projectService;
        this.tagService = tagService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstanceDTO) {
        ProjectInstance projectInstance = projectInstanceDTO.toProjectInstance();

        List<Tag> tags = projectInstanceDTO.tags();

        if (!org.springframework.util.CollectionUtils.isEmpty(tags)) {
            tags = tagService.save(tags);

            projectInstance.setTags(tags);
        }

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService.create(
            projectInstanceDTO.projectInstanceWorkflows());

        return new ProjectInstanceDTO(
            getLastExecutionDate(projectInstance.getId()),
            projectInstanceService.create(projectInstance), projectInstanceWorkflows,
            projectService.getProject(projectInstance.getProjectId()), tags);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    @SuppressFBWarnings("NP")
    public long createProjectInstanceJob(long projectInstanceId, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            projectInstanceId, workflowId);

        long jobId = jobFactory.create(new JobParameters(projectInstanceWorkflow.getInputParameters(), workflowId));

        projectInstanceWorkflowService.addJob(projectInstanceWorkflow.getId(), jobId);

        return jobId;
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
            getLastExecutionDate(projectInstance.getId()),
            projectInstance,
            projectInstanceWorkflowService.getProjectInstanceWorkflows(projectInstanceId),
            projectService.getProject(projectInstance.getProjectId()),
            tagService.getTags(projectInstance.getTagIds()));
    }

    @Override
    public List<Tag> getProjectInstanceTags() {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances();

        List<Long> tagIds = projectInstances.stream()
            .map(ProjectInstance::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    public List<ProjectInstanceDTO> searchProjectInstances(List<Long> projectIds, List<Long> tagIds) {
        List<ProjectInstance> projectInstances = projectInstanceService.searchProjectInstances(projectIds, tagIds);

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
            .getProjectInstanceWorkflows(
                CollectionUtils.map(projectInstances, ProjectInstance::getId));

        List<Project> projects = projectService.getProjects(
            projectInstances.stream()
                .map(ProjectInstance::getProjectId)
                .filter(Objects::nonNull)
                .toList());

        List<Tag> tags = tagService.getTags(
            projectInstances.stream()
                .flatMap(projectInstance -> CollectionUtils.stream(projectInstance.getTagIds()))
                .filter(Objects::nonNull)
                .toList());

        return CollectionUtils.map(
            projectInstances,
            projectInstance -> new ProjectInstanceDTO(
                getLastExecutionDate(projectInstance.getId()),
                projectInstance,
                CollectionUtils.filter(
                    projectInstanceWorkflows,
                    projectInstanceWorkflow -> Objects.equals(
                        projectInstanceWorkflow.getProjectInstanceId(), projectInstance.getId())),
                CollectionUtils.getFirst(
                    projects, project -> Objects.equals(project.getId(), projectInstance.getProjectId())),
                CollectionUtils.filter(
                    tags,
                    tag -> {
                        List<Long> curTagIds = projectInstance.getTagIds();

                        return curTagIds.contains(tag.getId());
                    })));
    }

    @Override
    public ProjectInstanceDTO update(ProjectInstanceDTO projectInstanceDTO) {
        List<Tag> tags = org.springframework.util.CollectionUtils.isEmpty(projectInstanceDTO.tags())
            ? Collections.emptyList()
            : tagService.save(projectInstanceDTO.tags());

        List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService.update(
            projectInstanceDTO.projectInstanceWorkflows());

        return new ProjectInstanceDTO(
            getLastExecutionDate(projectInstanceDTO.id()),
            projectInstanceService.update(
                projectInstanceDTO.id(), projectInstanceDTO.description(), projectInstanceDTO.name(),
                projectInstanceDTO.status(), CollectionUtils.map(tags, Tag::getId), projectInstanceDTO.version()),
            projectInstanceWorkflows,
            projectService.getProject(projectInstanceDTO.projectId()),
            tags);
    }

    @Override
    public void updateProjectInstanceTags(Long projectInstanceId, List<Tag> tags) {
        tags = org.springframework.util.CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        projectInstanceService.update(projectInstanceId, CollectionUtils.map(tags, Tag::getId));
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
}
