/*
 * Copyright 2025 ByteChef
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
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
@Service
public class ProjectFacadeImpl implements ProjectFacade {

    private final EnvironmentService environmentService;
    private final CategoryService categoryService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final TagService tagService;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        EnvironmentService environmentService, CategoryService categoryService,
        ProjectWorkflowService projectWorkflowService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, TagService tagService,
        WorkflowCacheManager workflowCacheManager, WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.environmentService = environmentService;
        this.categoryService = categoryService;
        this.projectWorkflowService = projectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.tagService = tagService;
        this.workflowCacheManager = workflowCacheManager;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public ProjectWorkflow addWorkflow(long id, String definition) {
        Project project = projectService.getProject(id);

        Workflow workflow = workflowService.create(definition, Format.JSON, SourceType.JDBC);

        return projectWorkflowService.addWorkflow(id, project.getLastVersion(), workflow.getId());
    }

    @Override
    public long createProject(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();
        Category category = projectDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        List<Tag> tags = checkTags(projectDTO.tags());

        if (!tags.isEmpty()) {
            project.setTags(tags);
        }

        project = projectService.create(project);

        return project.getId();
    }

    @Override
    public void deleteProject(long id) {
        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(id);

        for (ProjectDeployment projectDeployment : projectDeployments) {
            projectDeploymentFacade.deleteProjectDeployment(projectDeployment.getId());
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(id);

        projectWorkflowService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getId)
                .toList());

        workflowService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getWorkflowId)
                .toList());

        workflowTestConfigurationService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getWorkflowId)
                .toList());

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(project.getId());

        for (ProjectDeployment projectDeployment : projectDeployments) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(Validate.notNull(projectDeployment.getId(), "id"));

            if (CollectionUtils.anyMatch(
                projectDeploymentWorkflows,
                projectDeploymentWorkflow -> Objects.equals(projectDeploymentWorkflow.getWorkflowId(), workflowId))) {

                projectDeploymentWorkflows.stream()
                    .filter(
                        projectDeploymentWorkflow -> Objects.equals(
                            projectDeploymentWorkflow.getWorkflowId(), workflowId))
                    .findFirst()
                    .ifPresent(
                        projectDeploymentWorkflow -> projectDeploymentWorkflowService.delete(
                            projectDeploymentWorkflow.getId()));
            }
        }

        for (ProjectVersion projectVersion : project.getProjectVersions()) {
            projectWorkflowService.delete(project.getId(), projectVersion.getVersion(), workflowId);
        }

        workflowTestConfigurationService.delete(workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        Project newProject = new Project();

        newProject.setName(generateName(project.getName()));
        newProject.setTagIds(project.getTagIds());
        newProject.setWorkspaceId(project.getWorkspaceId());

        List<String> workflowIds = copyWorkflowIds(
            projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastVersion()));

        newProject = projectService.create(newProject);

        for (String workflowId : workflowIds) {
            projectWorkflowService.addWorkflow(newProject.getId(), newProject.getLastVersion(), workflowId);
        }

        return toProjectDTO(newProject);
    }

    @Override
    public String duplicateWorkflow(long id, String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        Workflow workflow = workflowService.duplicateWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", MapUtils.getString(definitionMap, "label", "(2)") + " (2)");

        workflowService.update(
            Validate.notNull(workflow.getId(), "id"), JsonUtils.writeWithDefaultPrettyPrinter(definitionMap),
            workflow.getVersion());

        projectWorkflowService.addWorkflow(id, project.getLastVersion(), workflow.getId());

        return workflow.getId();
    }

    @Override
    public byte[] exportProject(long id) {
        Project project = projectService.getProject(id);
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            id, project.getLastVersion());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos)) {

            Map<String, Object> projectData = new HashMap<>();

            projectData.put("name", project.getName());
            projectData.put("description", project.getDescription());

            ZipEntry projectEntry = new ZipEntry("project.json");

            zos.putNextEntry(projectEntry);

            String projectJson = JsonUtils.write(projectData);

            zos.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zos.closeEntry();

            for (ProjectWorkflow projectWorkflow : projectWorkflows) {
                Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());
                Format format = workflow.getFormat();

                String name = format.name();

                String fileName = String.format(
                    "workflow-%s.%s.json",
                    projectWorkflow.getUuid(), StringUtils.sanitize(name.toLowerCase(), 100));

                ZipEntry workflowEntry = new ZipEntry(fileName);

                zos.putNextEntry(workflowEntry);

                String definition = workflow.getDefinition();

                zos.write(definition.getBytes(StandardCharsets.UTF_8));

                zos.closeEntry();
            }

            zos.finish();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export project", e);
        }
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
    public ProjectWorkflowDTO getProjectWorkflow(String workflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return new ProjectWorkflowDTO(workflowFacade.getWorkflow(workflowId), projectWorkflow);
    }

    @Override
    public ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectWorkflows() {
        return projectWorkflowService.getProjectWorkflows()
            .stream()
            .map(projectWorkflow -> workflowFacade.fetchWorkflow(projectWorkflow.getWorkflowId())
                .map(workflowDTO -> new ProjectWorkflowDTO(workflowDTO, projectWorkflow))
                .orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectWorkflows(long projectId) {
        Project project = projectService.getProject(projectId);

        return projectWorkflowService
            .getProjectWorkflows(project.getId(), project.getLastVersion())
            .stream()
            .map(projectWorkflow -> new ProjectWorkflowDTO(
                workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .sorted(
                (projectWorkflow1, projectWorkflow2) -> {
                    String label1 = projectWorkflow1.getLabel();
                    String label2 = projectWorkflow2.getLabel();

                    return label1.compareToIgnoreCase(label2);
                })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectVersionWorkflows(
        long projectId, int projectVersion, boolean includeAllFields) {

        if (includeAllFields) {
            return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
                .stream()
                .map(projectWorkflow -> new ProjectWorkflowDTO(
                    workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
                .toList();
        } else {
            return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
                .stream()
                .map(projectWorkflow -> new ProjectWorkflowDTO(
                    workflowService.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
                .toList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjects(Long categoryId, Boolean projectDeployments, Long tagId, Status status) {
        return getProjects(null, categoryId, tagId, projectDeployments, status, true, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, Long categoryId, boolean includeAllFields, Boolean projectDeployments, Status status,
        Long tagId, long workspaceId) {

        return getProjects(
            apiCollections, categoryId, tagId, projectDeployments, status, includeAllFields, workspaceId);
    }

    @Override
    public List<ProjectWorkflowDTO> getWorkspaceProjectWorkflows(long workspaceId) {
        List<Long> projectIds = projectService.getProjects(null, null, null, null, null, workspaceId)
            .stream()
            .map(Project::getId)
            .toList();

        return projectWorkflowService.getProjectWorkflows(projectIds)
            .stream()
            .map(projectWorkflow -> new ProjectWorkflowDTO(
                workflowService.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .toList();
    }

    @Override
    public long importProject(byte[] projectData, long workspaceId) {
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(projectData))) {
            Map<String, String> projectInfo = null;
            List<String> workflowDefinitions = new ArrayList<>();

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                byte[] entryData = zis.readAllBytes();

                String name = entry.getName();

                if ("project.json".equals(name)) {
                    projectInfo = JsonUtils.read(
                        new String(entryData, StandardCharsets.UTF_8), new TypeReference<>() {});
                } else if (name.startsWith("workflow-")) {
                    workflowDefinitions.add(new String(entryData, StandardCharsets.UTF_8));
                }

                zis.closeEntry();
            }

            if (projectInfo == null) {
                throw new RuntimeException("project.json not found in import file");
            }

            Project project = new Project();

            project.setName(projectInfo.get("name"));
            project.setDescription(projectInfo.get("description"));
            project.setWorkspaceId(workspaceId);

            ProjectDTO projectDTO = new ProjectDTO(project);

            long projectId = createProject(projectDTO);

            for (String workflowDefinition : workflowDefinitions) {
                addWorkflow(projectId, workflowDefinition);
            }

            return projectId;

        } catch (IOException e) {
            throw new RuntimeException("Failed to import project", e);
        }
    }

    @Override
    public int publishProject(long id, String description, boolean syncWithGit) {
        Project project = projectService.getProject(id);

        int oldProjectVersion = project.getLastVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), oldProjectVersion);

        int newProjectVersion = projectService.publishProject(id, description, syncWithGit);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.update(oldProjectWorkflow);

            projectWorkflowService.addWorkflow(
                project.getId(), oldProjectVersion, oldWorkflowId, oldProjectWorkflow.getUuid());

            workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
        }

        return newProjectVersion;
    }

    @Override
    public void updateProject(ProjectDTO projectDTO) {
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();
        Category category = projectDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        project.setTags(tags);

        projectService.update(project);
    }

    @Override
    public void updateProjectTags(long id, List<Tag> tags) {
        tags = checkTags(tags);

        projectService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        workflowService.update(workflowId, definition, version);

        for (String cacheName : WorkflowNodeOutputFacade.WORKFLOW_CACHE_NAMES) {
            for (Environment environment : environmentService.getEnvironments()) {
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName, environment.ordinal());
            }
        }
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
        Boolean apiCollections, Long categoryId, Long tagId, Boolean projectDeployments, Status status,
        boolean includeAllFields, Long workspaceId) {

        List<Project> projects = projectService.getProjects(
            apiCollections, categoryId, projectDeployments, tagId, status, workspaceId);

        if (includeAllFields) {
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
                    projectWorkflowService.getProjectProjectWorkflowIds(project.getId(), project.getLastVersion()),
                    CollectionUtils.filter(
                        tagService.getTags(
                            projects.stream()
                                .flatMap(curProject -> CollectionUtils.stream(curProject.getTagIds()))
                                .filter(Objects::nonNull)
                                .toList()),
                        tag -> CollectionUtils.contains(project.getTagIds(), tag.getId()))));
        } else {
            return CollectionUtils.map(projects, ProjectDTO::new);
        }
    }

    private ProjectDTO toProjectDTO(Project project) {
        return new ProjectDTO(
            getCategory(project), project,
            projectWorkflowService.getProjectProjectWorkflowIds(project.getId(), project.getLastVersion()),
            tagService.getTags(project.getTagIds()));
    }
}
