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
import com.bytechef.automation.configuration.constant.ProjectErrorType;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.WorkflowDTO;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
import com.bytechef.platform.configuration.exception.ConfigurationException;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
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
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ProjectInstanceFacade projectInstanceFacade;
    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final TagService tagService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowNodeParameterFacade workflowNodeParameterFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, ProjectWorkflowService projectWorkflowService,
        ProjectInstanceService projectInstanceService, ProjectService projectService,
        ProjectInstanceFacade projectInstanceFacade, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        TagService tagService, WorkflowFacade workflowFacade, WorkflowNodeParameterFacade workflowNodeParameterFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.categoryService = categoryService;
        this.projectWorkflowService = projectWorkflowService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.projectInstanceFacade = projectInstanceFacade;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.tagService = tagService;
        this.workflowFacade = workflowFacade;
        this.workflowNodeParameterFacade = workflowNodeParameterFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public WorkflowDTO addWorkflow(long id, @NonNull String definition) {
        checkProjectWorkflowStatus(id, null);

        Project project = projectService.getProject(id);

        Workflow workflow = workflowService.create(definition, Format.JSON, SourceType.JDBC);

        ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
            id, project.getLastVersion(), workflow.getId());

        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(id);

        for (ProjectInstance projectInstance : projectInstances) {
            ProjectInstanceWorkflow projectInstanceWorkflow = new ProjectInstanceWorkflow();

            projectInstanceWorkflow.setProjectInstanceId(projectInstance.getId());
            projectInstanceWorkflow.setWorkflowId(workflow.getId());

            projectInstanceWorkflowService.create(projectInstanceWorkflow);
        }

        return new WorkflowDTO(workflowFacade.getWorkflow(workflow.getId()), projectWorkflow);
    }

    @Override
    public ProjectDTO createProject(@NonNull ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        Category category = projectDTO.category();

        if (projectDTO.category() != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        List<Tag> tags = checkTags(projectDTO.tags());

        if (!tags.isEmpty()) {
            project.setTags(tags);
        }

        project = projectService.create(project);

        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Format.JSON, SourceType.JDBC);

        projectWorkflowService.addWorkflow(
            project.getId(), project.getLastVersion(), Validate.notNull(workflow.getId(), "id"));

        return new ProjectDTO(
            category, project, tags,
            projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastVersion()));
    }

    @Override
    public void deleteProject(long id) {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances(id);

        for (ProjectInstance projectInstance : projectInstances) {
            projectInstanceFacade.deleteProjectInstance(projectInstance.getId());
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(id);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            workflowService.delete(projectWorkflow.getWorkflowId());
        }

        projectWorkflowService.deleteProjectWorkflows(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getId)
                .toList());

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteWorkflow(@NonNull String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        checkProjectWorkflowStatus(project.getId(), workflowId);

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

        projectWorkflowService.removeWorkflow(project.getId(), project.getLastVersion(), workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public Map<String, ?> deleteWorkflowParameter(String workflowId, String workflowNodeName, String path) {
        Project project = projectService.getWorkflowProject(workflowId);

        ProjectWorkflowStatusResult projectWorkflowStatusResult = checkProjectWorkflowStatus(
            project.getId(), workflowId);

        if (projectWorkflowStatusResult != null) {
            workflowId = projectWorkflowStatusResult.workflowId;
        }

        return workflowNodeParameterFacade.deleteParameter(workflowId, workflowNodeName, path);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        final Project newProject = new Project();

        newProject.setName(generateName(project.getName()));
        newProject.setTagIds(project.getTagIds());

        copyWorkflowIds(projectWorkflowService.getWorkflowIds(project.getId(), project.getLastVersion()))
            .forEach(workflowId -> projectWorkflowService.addWorkflow(
                newProject.getId(), newProject.getLastVersion(), workflowId));

        return toProjectDTO(projectService.create(newProject));
    }

    @Override
    public String duplicateWorkflow(long id, @NonNull String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        checkProjectWorkflowStatus(project.getId(), workflowId);

        Workflow workflow = workflowService.duplicateWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", MapUtils.getString(definitionMap, "label", "(2)") + " (2)");

        workflowService.update(
            Validate.notNull(workflow.getId(), "id"),
            JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());

        projectWorkflowService.addWorkflow(id, project.getLastVersion(), workflow.getId());

        return workflow.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO getProject(long id) {
        Project project = projectService.getProject(id);

        return toProjectDTO(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getProjectCategories() {
        List<Project> projects = projectService.getProjects();

        return categoryService.getCategories(
            CollectionUtils.filter(CollectionUtils.map(projects, Project::getCategoryId), Objects::nonNull));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags() {
        List<Project> projects = projectService.getProjects();

        return tagService.getTags(CollectionUtils.flatMap(projects, Project::getTagIds));
    }

    @Override
    public WorkflowDTO getProjectWorkflow(String workflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return new WorkflowDTO(workflowFacade.getWorkflow(workflowId), projectWorkflow);
    }

    @Override
    public WorkflowDTO getProjectWorkflow(long projectWorkflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(projectWorkflowId);

        return new WorkflowDTO(workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowDTO> getProjectWorkflows() {
        return projectWorkflowService.getProjectWorkflows()
            .stream()
            .map(projectWorkflow -> new WorkflowDTO(
                workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowDTO> getProjectWorkflows(long id) {
        Project project = projectService.getProject(id);

        return projectWorkflowService
            .getProjectWorkflows(project.getId(), project.getLastVersion())
            .stream()
            .map(projectWorkflow -> new WorkflowDTO(
                workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .toList();
    }

    @Override
    public List<WorkflowDTO> getProjectVersionWorkflows(long id, int projectVersion) {
        return projectWorkflowService.getProjectWorkflows(id, projectVersion)
            .stream()
            .map(projectWorkflow -> new WorkflowDTO(
                workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjects(Long categoryId, boolean projectInstances, Long tagId, Status status) {
        return getProjects(null, categoryId, projectInstances, tagId, status);
    }

    @Override
    public List<ProjectDTO> getWorkspaceProjects(
        long workspaceId, Long categoryId, boolean projectInstances, Long tagId, Status status) {

        return getProjects(workspaceId, categoryId, projectInstances, tagId, status);
    }

    @Override
    public ProjectDTO updateProject(@NonNull ProjectDTO projectDTO) {
        Category category = projectDTO.category() == null ? null : categoryService.save(projectDTO.category());
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();

        project.setTags(tags);

        return new ProjectDTO(
            category, projectService.update(project), tags,
            projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastVersion()));
    }

    @Override
    public void updateProjectTags(long id, @NonNull List<Tag> tags) {
        tags = checkTags(tags);

        projectService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public WorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        ProjectWorkflowStatusResult projectWorkflowStatusResult = checkProjectWorkflowStatus(
            projectWorkflow.getProjectId(), workflowId);

        if (projectWorkflowStatusResult != null) {
            workflowId = projectWorkflowStatusResult.workflowId;
            version = projectWorkflowStatusResult.version;
        }

        return new WorkflowDTO(workflowFacade.update(workflowId, definition, version), projectWorkflow);
    }

    @Override
    public UpdateParameterResultDTO updateWorkflowParameter(
        String workflowId, String workflowNodeName, String path, Object value) {

        Project project = projectService.getWorkflowProject(workflowId);

        ProjectWorkflowStatusResult projectWorkflowStatusResult = checkProjectWorkflowStatus(
            project.getId(), workflowId);

        if (projectWorkflowStatusResult != null) {
            workflowId = projectWorkflowStatusResult.workflowId;
        }

        return workflowNodeParameterFacade.updateParameter(workflowId, workflowNodeName, path, value);
    }

    private ProjectWorkflowStatusResult checkProjectWorkflowStatus(long id, @Nullable String workflowId) {
        ProjectWorkflowStatusResult projectWorkflowStatusResult = null;

        Project project = projectService.getProject(id);

        List<ProjectWorkflow> latestProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastVersion());

        List<String> latestWorkflowIds = latestProjectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        if (workflowId != null && !latestWorkflowIds.contains(workflowId)) {
            throw new ConfigurationException(
                "Older version of the workflow id=%s cannot be updated.".formatted(workflowId),
                ProjectErrorType.UPDATE_OLD_WORKFLOW);
        }

        if (project.getLastStatus() == Status.PUBLISHED) {
            int lastVersion = project.getLastVersion();
            int newVersion = projectService.addVersion(id);

            for (ProjectWorkflow projectWorkflow : latestProjectWorkflows) {
                String oldWorkflowId = projectWorkflow.getWorkflowId();

                Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

                if (Objects.equals(workflowId, oldWorkflowId)) {
                    projectWorkflowStatusResult = new ProjectWorkflowStatusResult(
                        duplicatedWorkflow.getId(), duplicatedWorkflow.getVersion());
                }

                projectWorkflow.setProjectVersion(newVersion);
                projectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

                projectWorkflowService.update(projectWorkflow);

                projectWorkflowService.addWorkflow(
                    project.getId(), lastVersion, oldWorkflowId, projectWorkflow.getWorkflowReferenceCode());

                workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
                workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            }
        }

        return projectWorkflowStatusResult;
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

    private List<ProjectDTO> getProjects(
        Long workspaceId, Long categoryId, boolean projectInstances, Long tagId, Status status) {

        List<Long> projectIds = List.of();

        if (projectInstances) {
            projectIds = projectInstanceService.getProjectIds();
        }

        List<Project> projects = projectService.getProjects(workspaceId, categoryId, projectIds, tagId, status);

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
                    tag -> CollectionUtils.contains(project.getTagIds(), tag.getId())),
                projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastVersion())));
    }

    private ProjectDTO toProjectDTO(Project project) {
        return new ProjectDTO(
            getCategory(project), project, tagService.getTags(project.getTagIds()),
            projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastVersion()));
    }

    private record ProjectWorkflowStatusResult(String workflowId, int version) {
    }
}
