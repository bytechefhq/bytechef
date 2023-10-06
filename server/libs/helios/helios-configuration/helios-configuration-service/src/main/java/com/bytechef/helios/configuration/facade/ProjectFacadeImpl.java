
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
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.dto.ProjectDTO;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Transactional
@Service
public class ProjectFacadeImpl implements ProjectFacade {

    private final CategoryService categoryService;
    private final ProjectService projectService;
    private final ProjectInstanceService projectInstanceService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, ProjectInstanceService projectInstanceService, ProjectService projectService,
        TagService tagService, WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public Workflow addProjectWorkflow(long id, String label, String description, String definition) {
        if (definition == null) {
            definition = "{\"description\": \"%s\", \"label\": \"%s\", \"tasks\": []}"
                .formatted(description, label);
        }

        Workflow workflow = workflowService.create(
            definition, Format.JSON, SourceType.JDBC, ProjectConstants.PROJECT_TYPE);

        projectService.addWorkflow(id, workflow.getId());

        return workflow;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        Category category = projectDTO.category();

        if (projectDTO.category() != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        if (org.springframework.util.CollectionUtils.isEmpty(projectDTO.workflowIds())) {
            Workflow workflow = workflowService.create(
                null, Format.JSON, SourceType.JDBC, ProjectConstants.PROJECT_TYPE);

            project.setWorkflowIds(List.of(Objects.requireNonNull(workflow.getId())));
        }

        List<Tag> tags = checkTags(projectDTO.tags());

        if (!tags.isEmpty()) {
            project.setTags(tags);
        }

        return new ProjectDTO(projectService.create(project), category, tags);
    }

    @Override
    public void deleteProject(long id) {
        Project project = projectService.getProject(id);

        for (String workflowId : project.getWorkflowIds()) {
            workflowService.delete(workflowId);
        }

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteWorkflow(long id, String workflowId) {
        projectService.removeWorkflow(id, workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        project.setId(null);
        project.setName(generateName(project.getName()));
        project.setVersion(0);
        project.setTagIds(project.getTagIds());
        project.setWorkflowIds(copyWorkflowIds(project.getWorkflowIds()));

        project = projectService.create(project);

        return new ProjectDTO(
            project, project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tagService.getTags(project.getTagIds()));
    }

    @Override
    public Workflow duplicateWorkflow(long id, String workflowId) {
        Workflow workflow = workflowService.duplicateWorkflow(workflowId);

        projectService.addWorkflow(id, workflow.getId());

        return workflow;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO getProject(long id) {
        Project project = projectService.getProject(id);

        return new ProjectDTO(
            project, project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tagService.getTags(project.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getProjectCategories() {
        List<Project> projects = projectService.getProjects();

        return categoryService.getCategories(
            projects.stream()
                .map(Project::getCategoryId)
                .filter(Objects::nonNull)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags() {
        List<Project> projects = projectService.getProjects();

        List<Long> tagIds = projects.stream()
            .map(Project::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getProjectWorkflows(long id) {
        Project project = projectService.getProject(id);

        return workflowService.getWorkflows(project.getWorkflowIds());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getPublishedProjects(Long categoryId, boolean projectInstances, Long tagId) {
        List<Long> projectIds = null;

        if (projectInstances) {
            projectIds = projectInstanceService.getProjectIds();
        }

        List<Project> projects = projectService.getPublishedProjects(categoryId, projectIds, tagId);

        return CollectionUtils.map(
            projects,
            project -> new ProjectDTO(
                project,
                CollectionUtils.findFirstOrElse(
                    categoryService.getCategories(
                        projects.stream()
                            .map(Project::getCategoryId)
                            .filter(Objects::nonNull)
                            .toList()),
                    category -> Objects.equals(project.getCategoryId(), category.getId()),
                    null),
                CollectionUtils.filter(
                    tagService.getTags(
                        projects.stream()
                            .flatMap(curProject -> CollectionUtils.stream(curProject.getTagIds()))
                            .filter(Objects::nonNull)
                            .toList()),
                    tag -> CollectionUtils.contains(project.getTagIds(), tag.getId()))));
    }

    @Override
    public ProjectDTO updateProject(ProjectDTO projectDTO) {
        Category category = projectDTO.category() == null ? null : categoryService.save(projectDTO.category());
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();

        project.setTags(tags);

        return new ProjectDTO(projectService.update(project), category, tags);
    }

    @Override
    public void updateProjectTags(long id, List<Tag> tags) {
        tags = checkTags(tags);

        Project project = projectService.update(id, CollectionUtils.map(tags, Tag::getId));

        new ProjectDTO(
            project, project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tags);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return org.springframework.util.CollectionUtils.isEmpty(tags)
            ? Collections.emptyList()
            : tagService.save(tags);
    }

    private List<String> copyWorkflowIds(List<String> workflowIds) {
        List<String> newWorkflowIds = new ArrayList<>();

        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(
                workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType(),
                ProjectConstants.PROJECT_TYPE);

            newWorkflowIds.add(workflow.getId());
        }
        return newWorkflowIds;
    }

    private String generateName(String oldName) {
        List<Project> projects = projectService.getProjects();

        int addendum = 0;

        for (Project curProject : projects) {
            String name = curProject.getName();

            if (name.startsWith(oldName)) {
                addendum++;
            }
        }

        return oldName + " (%s)".formatted(addendum);
    }
}
