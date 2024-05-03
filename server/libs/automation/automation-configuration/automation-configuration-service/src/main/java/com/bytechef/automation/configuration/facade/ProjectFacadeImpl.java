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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.constant.ProjectErrorType;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
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
    private final JobService jobService;
    private final ProjectService projectService;
    private final ProjectInstanceFacade projectInstanceFacade;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final TagService tagService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, JobService jobService, ProjectInstanceService projectInstanceService,
        ProjectService projectService, ProjectInstanceFacade projectInstanceFacade,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, TagService tagService,
        WorkflowFacade workflowFacade,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.projectInstanceFacade = projectInstanceFacade;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.tagService = tagService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
    }

    @Override
    public Workflow addWorkflow(long id, @NonNull String definition) {
        checkProjectStatus(id, null);

        Workflow workflow = workflowService.create(definition, Format.JSON, SourceType.JDBC);

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
    public void checkProjectStatus(long id, @Nullable String workflowId) {
        Project project = projectService.getProject(id);

        List<String> latestWorkflowIds = project.getWorkflowIds(project.getLastVersion());

        if (workflowId != null && !latestWorkflowIds.contains(workflowId)) {
            throw new ConfigurationException(
                "Older version of the workflow cannot be updated.", ProjectErrorType.UPDATE_OLD_WORKFLOW);
        }

        if (project.getLastStatus() == Status.PUBLISHED) {
            List<String> duplicatedVersionWorkflowIds = new ArrayList<>();

            for (String curWorkflowId : latestWorkflowIds) {
                Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(curWorkflowId);

                jobService.updateWorkflowId(curWorkflowId, duplicatedWorkflow.getId());

                duplicatedVersionWorkflowIds.add(duplicatedWorkflow.getId());

                List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(project.getId());

                for (ProjectInstance projectInstance : projectInstances) {
                    List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
                        .getProjectInstanceWorkflows(projectInstance.getId());

                    for (ProjectInstanceWorkflow projectInstanceWorkflow : projectInstanceWorkflows) {
                        if (Objects.equals(projectInstanceWorkflow.getWorkflowId(), curWorkflowId)) {
                            projectInstanceWorkflow.setWorkflowId(duplicatedWorkflow.getId());

                            projectInstanceWorkflowService.update(projectInstanceWorkflow);
                        }
                    }
                }
            }

            projectService.addVersion(id, duplicatedVersionWorkflowIds);
        }
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
                WORKFLOW_DEFINITION, Format.JSON, SourceType.JDBC);

            project.addWorkflowId(Validate.notNull(workflow.getId(), "id"));
        }

        List<Tag> tags = checkTags(projectDTO.tags());

        if (!tags.isEmpty()) {
            project.setTags(tags);
        }

        return new ProjectDTO(category, projectService.create(project), tags);
    }

    @Override
    public void deleteProject(long id) {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(id);

        for (ProjectInstance projectInstance : projectInstances) {
            projectInstanceFacade.deleteProjectInstance(projectInstance.getId());
        }

        Project project = projectService.getProject(id);

        List<String> workflowIds = project.getAllWorkflowIds();

        for (String workflowId : workflowIds) {
            workflowService.delete(workflowId);
        }

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteWorkflow(@NonNull String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        checkProjectStatus(project.getId(), workflowId);

        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(project.getId());

        for (ProjectInstance projectInstance : projectInstances) {
            List<ProjectInstanceWorkflow> projectInstanceWorkflows = projectInstanceWorkflowService
                .getProjectInstanceWorkflows(Validate.notNull(projectInstance.getId(), "id"));

            if (CollectionUtils.anyMatch(
                projectInstanceWorkflows,
                projectInstanceWorkflow -> Objects.equals(projectInstanceWorkflow.getWorkflowId(), workflowId))) {

                projectInstanceWorkflows.stream()
                    .filter(
                        projectInstanceWorkflow -> Objects.equals(projectInstanceWorkflow.getWorkflowId(), workflowId))
                    .findFirst()
                    .ifPresent(
                        projectInstanceWorkflow -> projectInstanceWorkflowService.delete(
                            projectInstanceWorkflow.getId()));
            }
        }

        projectService.removeWorkflow(project.getId(), workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        Project newProject = new Project();

        newProject.setName(generateName(project.getName()));
        newProject.setTagIds(project.getTagIds());

        copyWorkflowIds(project.getWorkflowIds(project.getLastVersion()))
            .forEach(newProject::addWorkflowId);

        newProject = projectService.create(newProject);

        return getProjectDTO(newProject);
    }

    @Override
    public String duplicateWorkflow(long id, @NonNull String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        checkProjectStatus(project.getId(), workflowId);

        Workflow workflow = workflowService.duplicateWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", MapUtils.getString(definitionMap, "label", "(2)") + " (2)");

        workflowService.update(
            Validate.notNull(workflow.getId(), "id"),
            JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        projectService.addWorkflow(id, workflow.getId());

        return workflow.getId();
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
            projects
                .stream()
                .map(Project::getCategoryId)
                .filter(Objects::nonNull)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjects(Long categoryId, boolean projectInstances, Long tagId, Status status) {
        List<Long> projectIds = List.of();

        if (projectInstances) {
            projectIds = projectInstanceService.getProjectIds();
        }

        List<Project> projects = projectService.getProjects(categoryId, projectIds, tagId, status);

        return CollectionUtils.map(
            projects,
            project -> new ProjectDTO(
                CollectionUtils.findFirstFilterOrElse(
                    categoryService.getCategories(
                        projects
                            .stream()
                            .map(Project::getCategoryId)
                            .filter(Objects::nonNull)
                            .toList()),
                    category -> Objects.equals(project.getCategoryId(), category.getId()),
                    null),
                project,
                CollectionUtils.filter(
                    tagService.getTags(
                        projects.stream()
                            .flatMap(curProject -> CollectionUtils.stream(curProject.getTagIds()))
                            .filter(Objects::nonNull)
                            .toList()),
                    tag -> CollectionUtils.contains(project.getTagIds(), tag.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags() {
        List<Project> projects = projectService.getProjects();

        List<Long> tagIds = projects
            .stream()
            .map(Project::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getProjectWorkflows() {
        return workflowService.getWorkflows(
            projectService.getProjects()
                .stream()
                .flatMap(project -> CollectionUtils.stream(project.getAllWorkflowIds()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getProjectWorkflows(long id) {
        Project project = projectService.getProject(id);

        return workflowService.getWorkflows(project.getWorkflowIds(project.getLastVersion()));
    }

    @Override
    public List<Workflow> getProjectVersionWorkflows(long id, int projectVersion) {
        Project project = projectService.getProject(id);

        return workflowService.getWorkflows(project.getWorkflowIds(projectVersion));
    }

    @Override
    public ProjectDTO updateProject(@NonNull ProjectDTO projectDTO) {
        Category category = projectDTO.category() == null ? null : categoryService.save(projectDTO.category());
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();

        project.setTags(tags);

        return new ProjectDTO(category, projectService.update(project), tags);
    }

    @Override
    public void updateProjectTags(long id, @NonNull List<Tag> tags) {
        tags = checkTags(tags);

        projectService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public WorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        Project project = projectService.getWorkflowProject(workflowId);

        checkProjectStatus(project.getId(), workflowId);

        return workflowFacade.update(workflowId, definition, version);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private List<String> copyWorkflowIds(List<String> workflowIds) {
        List<String> newWorkflowIds = new ArrayList<>();

        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(
                workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());

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

    private Category getCategory(Project project) {
        return project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId());
    }

    private ProjectDTO getProjectDTO(Project project) {
        return new ProjectDTO(getCategory(project), project, tagService.getTags(project.getTagIds()));
    }
}
