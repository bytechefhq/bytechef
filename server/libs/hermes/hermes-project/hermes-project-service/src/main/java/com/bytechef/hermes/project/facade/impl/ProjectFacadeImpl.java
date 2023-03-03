
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

package com.bytechef.hermes.project.facade.impl;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.hermes.project.facade.ProjectFacade;
import com.bytechef.category.service.CategoryService;
import com.bytechef.hermes.project.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectFacadeImpl implements ProjectFacade {

    private final CategoryService categoryService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, ProjectService projectService, TagService tagService,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public Project addWorkflow(long id, String name, String description, String definition) {
        if (definition == null) {
            definition = "{\"label\": \"%s\", \"description\": \"%s\", \"tasks\": []}"
                .formatted(name, description);
        }

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return projectService.addWorkflow(id, workflow.getId());
    }

    @Override
    @SuppressFBWarnings("NP")
    public Project create(Project project) {
        if (project.getCategory() != null) {
            Category category = project.getCategory();

            project.setCategory(categoryService.save(category));
        }

        if (CollectionUtils.isEmpty(project.getWorkflowIds())) {
            Workflow workflow = workflowService.create(null, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            project.setWorkflowIds(List.of(workflow.getId()));
        }

        if (!CollectionUtils.isEmpty(project.getTags())) {
            project.setTags(tagService.save(project.getTags()));
        }

        return projectService.create(project);
    }

    @Override
    public void delete(Long id) {
//        Project project = projectService.getProject(id);

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public Project duplicate(long id) {
        Project project = getProject(id);

        List<String> workflowIds = new ArrayList<>();

        for (String workflowId : project.getWorkflowIds()) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());

            workflowIds.add(workflow.getId());
        }

        project.setId(null);
        project.setVersion(0);
        project.setWorkflowIds(workflowIds);

        return projectService.create(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProject(Long id) {
        Project project = projectService.getProject(id);

        if (project.getCategoryId() != null) {
            categoryService.fetchCategory(project.getCategoryId())
                .ifPresent(project::setCategory);
        }

        project.setTags(tagService.getTags(project.getTagIds()));

        return project;
    }

    @Override
    public List<Category> getProjectCategories() {
        List<Project> projects = projectService.getProjects(null, null);

        List<Long> categoryIds = projects.stream()
            .map(Project::getCategoryId)
            .filter(Objects::nonNull)
            .toList();

        return categoryService.getCategories(categoryIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects(List<Long> categoryIds, List<Long> tagIds) {
        List<Project> projects = projectService.getProjects(categoryIds, tagIds);

        List<Category> categories = categoryService.getCategories(projects.stream()
            .map(Project::getCategoryId)
            .filter(Objects::nonNull)
            .toList());

        for (Category category : categories) {
            projects.stream()
                .filter(project -> Objects.equals(project.getCategoryId(), category.getId()))
                .forEach(project -> project.setCategory(category));
        }

        List<Tag> tags = tagService.getTags(projects.stream()
            .flatMap(project -> project.getTagIds()
                .stream())
            .filter(Objects::nonNull)
            .toList());

        for (Project project : projects) {
            project.setTags(
                tags.stream()
                    .filter(tag -> {
                        List<Long> curTagIds = project.getTagIds();

                        return curTagIds.contains(tag.getId());
                    })
                    .toList());
        }

        return projects;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags() {
        List<Project> projects = projectService.getProjects(null, null);

        List<Long> tagIds = projects.stream()
            .map(Project::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    public List<Workflow> getProjectWorkflows(Long id) {
        Project project = projectService.getProject(id);

        return workflowService.getWorkflows(project.getWorkflowIds());
    }

    @Override
    public Project update(Long id, List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        return projectService.update(id, tags);
    }

    @Override
    public Project update(Project project) {
        project
            .setCategory(project.getCategory() == null ? null : categoryService.save(project.getCategory()));
        project
            .setTags(
                CollectionUtils.isEmpty(project.getTags())
                    ? Collections.emptyList()
                    : tagService.save(project.getTags()));

        return projectService.update(project);
    }
}
