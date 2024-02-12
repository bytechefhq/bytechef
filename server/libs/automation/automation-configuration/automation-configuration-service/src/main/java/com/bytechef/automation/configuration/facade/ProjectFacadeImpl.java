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
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.constant.Type;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
@Service
public class ProjectFacadeImpl implements ProjectFacade {

    private static final String WORKFLOW_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [
            ],
            "triggers": [
            ],
            "tasks": [
            ]
        }
        """;

    private final CategoryService categoryService;
    private final ProjectService projectService;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, ProjectInstanceService projectInstanceService, ProjectService projectService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, TagService tagService,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public Workflow addProjectWorkflow(long id, @NonNull String definition) {
        Workflow workflow = workflowService.create(
            definition, Format.JSON, SourceType.JDBC, Type.AUTOMATION.getId());

        projectService.addWorkflow(id, workflow.getId());

        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(id);

        for (ProjectInstance projectInstance : projectInstances) {
            ProjectInstanceWorkflow projectInstanceWorkflow = new ProjectInstanceWorkflow();

            projectInstanceWorkflow.setProjectInstanceId(projectInstance.getId());
            projectInstanceWorkflow.setWorkflowId(workflow.getId());

            projectInstanceWorkflowService.create(projectInstanceWorkflow);
        }

        return workflow;
    }

    @Override
    public ProjectDTO createProject(@NonNull ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        Category category = projectDTO.category();

        if (projectDTO.category() != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        if (CollectionUtils.isEmpty(projectDTO.workflowIds())) {
            Workflow workflow = workflowService.create(
                WORKFLOW_DEFINITION, Format.JSON, SourceType.JDBC, Type.AUTOMATION.getId());

            project.setWorkflowIds(List.of(Validate.notNull(workflow.getId(), "id")));
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
    public void deleteWorkflow(long id, @NonNull String workflowId) {
        projectService.removeWorkflow(id, workflowId);

        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(id);

        for (ProjectInstance projectInstance : projectInstances) {
            List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
                .getProjectInstanceWorkflows(Validate.notNull(projectInstance.getId(), "id"));

            if (CollectionUtils.anyMatch(
                projectInstanceWorkflows,
                projectInstanceWorkflow -> Objects.equals(projectInstanceWorkflow.getWorkflowId(), workflowId))) {

                throw new IllegalArgumentException("Workflow id=%s is in use".formatted(workflowId));
            }
        }

        workflowService.delete(workflowId);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        project.setId(null);
        project.setName(generateName(project.getName()));
        project.setPublishedDate(null);
        project.setVersion(0);
        project.setTagIds(project.getTagIds());
        project.setWorkflowIds(copyWorkflowIds(project.getWorkflowIds()));

        project = projectService.create(project);

        return getProjectDTO(project);
    }

    @Override
    public Workflow duplicateWorkflow(long id, @NonNull String workflowId) {
        Workflow workflow = workflowService.duplicateWorkflow(workflowId);

        projectService.addWorkflow(id, workflow.getId());

        return workflow;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO getProject(long id) {
        Project project = projectService.getProject(id);

        return getProjectDTO(project);
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
    public List<ProjectDTO> getProjects(Long categoryId, boolean projectInstances, Long tagId, Boolean published) {
        List<Long> projectIds = null;

        if (projectInstances) {
            projectIds = projectInstanceService.getProjectIds();
        }

        List<Project> projects = projectService.getProjects(categoryId, projectIds, tagId, published);

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
    public ProjectDTO publishProject(long id) {
        return getProjectDTO(projectService.publish(id));
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
    public ProjectDTO updateProject(@NonNull ProjectDTO projectDTO) {
        Category category = projectDTO.category() == null ? null : categoryService.save(projectDTO.category());
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();

        project.setTags(tags);

        return new ProjectDTO(projectService.update(project), category, tags);
    }

    @Override
    public void updateProjectTags(long id, @NonNull List<Tag> tags) {
        tags = checkTags(tags);

        Project project = projectService.update(id, CollectionUtils.map(tags, Tag::getId));

        new ProjectDTO(
            project, project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tags);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private List<String> copyWorkflowIds(List<String> workflowIds) {
        List<String> newWorkflowIds = new ArrayList<>();

        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(
                workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType(),
                Type.AUTOMATION.getId());

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

    private ProjectDTO getProjectDTO(Project project) {
        return new ProjectDTO(
            project, project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tagService.getTags(project.getTagIds()));
    }
}
