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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.ai.tool.model.ProjectDeploymentStatusInfo;
import com.bytechef.automation.ai.tool.model.ProjectDetailInfo;
import com.bytechef.automation.ai.tool.model.ProjectInfo;
import com.bytechef.automation.ai.tool.model.ProjectPublishInfo;
import com.bytechef.automation.ai.tool.model.ProjectStatusInfo;
import com.bytechef.automation.ai.tool.model.ProjectVersionInfo;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The ProjectTools class provides a suite of tools for managing and interacting with projects in the ByteChef platform.
 * It encompasses functionalities such as project creation, retrieval, update, deletion, publishing, and searching,
 * along with deployment status management. It reaches the project domain through facades only, so their authorization
 * rules apply to every call.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectTools {

    private static final Logger log = LoggerFactory.getLogger(ProjectTools.class);

    private final ProjectCategoryFacade projectCategoryFacade;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectFacade projectFacade;
    private final ProjectTagFacade projectTagFacade;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public ProjectTools(
        ProjectCategoryFacade projectCategoryFacade, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectFacade projectFacade, ProjectTagFacade projectTagFacade, WorkspaceFacade workspaceFacade) {

        this.projectCategoryFacade = projectCategoryFacade;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectFacade = projectFacade;
        this.projectTagFacade = projectTagFacade;
        this.workspaceFacade = workspaceFacade;
    }

    @Tool(
        description = "List the projects of a workspace. Returns a list of projects with their basic information including id, name, description, and status.")
    public List<ProjectInfo> listProjects(
        @ToolParam(
            required = false, description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) Long workspaceId,
        ToolContext toolContext) {

        long resolvedWorkspaceId = requireWorkspaceId(workspaceId, toolContext, ProjectErrorType.LIST_PROJECTS);

        try {
            List<ProjectInfo> projectInfos = getWorkspaceProjects(resolvedWorkspaceId).stream()
                .map(ProjectTools::getProjectInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug("listProjects(): Found {} projects", projectInfos.size());
            }

            return projectInfos;
        } catch (Exception e) {
            log.error("listProjects(): Failed to list projects", e);

            throw new ExecutionException(
                "Failed to list projects: " + e.getMessage(), e, ProjectErrorType.LIST_PROJECTS);
        }
    }

    @Tool(
        description = "Get comprehensive information about a specific project. Returns detailed project information including id, name, description, status, versions, and metadata.")
    public ProjectDetailInfo getProject(
        @ToolParam(description = "The ID of the project to retrieve") long projectId) {

        try {
            ProjectDTO projectDTO = projectFacade.getProject(projectId);

            List<ProjectVersionInfo> projectVersionInfos = projectDTO.projectVersions()
                .stream()
                .map(projectVersion -> {
                    ProjectVersion.Status status = projectVersion.getStatus();

                    return new ProjectVersionInfo(
                        projectVersion.getVersion(), status.name(), projectVersion.getDescription(),
                        projectVersion.getPublishedDate());
                })
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "getProject({}): Retrieved project {} with {} versions", projectId, projectId,
                    projectVersionInfos.size());
            }

            Category category = projectDTO.category();

            return new ProjectDetailInfo(
                projectDTO.id(), projectDTO.name(), projectDTO.description(), getLastStatusName(projectDTO),
                category == null ? null : category.getId(), projectDTO.workspaceId(), getTagIds(projectDTO.tags()),
                projectVersionInfos, projectDTO.createdDate(), projectDTO.lastModifiedDate(),
                projectDTO.lastPublishedDate());
        } catch (Exception e) {
            log.error("getProject({}): Failed to get project {}", projectId, projectId, e);

            throw new ExecutionException(
                "Failed to get project: " + e.getMessage(), e, ProjectErrorType.GET_PROJECT);
        }
    }

    @Tool(
        description = "Full-text search across the projects of a workspace. Returns a list of projects matching the search query in name or description.")
    public List<ProjectInfo> searchProjects(
        @ToolParam(description = "The search query to match against project names and descriptions") String query,
        @ToolParam(
            required = false, description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) Long workspaceId,
        ToolContext toolContext) {

        long resolvedWorkspaceId = requireWorkspaceId(workspaceId, toolContext, ProjectErrorType.SEARCH_PROJECTS);

        try {
            String lowerQuery = query.toLowerCase()
                .trim();

            List<ProjectInfo> matchingProjects = getWorkspaceProjects(resolvedWorkspaceId).stream()
                .filter(projectDTO -> contains(projectDTO.name(), lowerQuery) ||
                    contains(projectDTO.description(), lowerQuery))
                .map(ProjectTools::getProjectInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "searchProjects({}): Found {} projects matching query '{}'", query, matchingProjects.size(), query);
            }

            return matchingProjects;
        } catch (Exception e) {
            log.error("searchProjects({}): Failed to search projects with query '{}'", query, query, e);

            throw new ExecutionException(
                "Failed to search projects: " + e.getMessage(), e, ProjectErrorType.SEARCH_PROJECTS);
        }
    }

    @Tool(
        description = "Get project deployment and execution status. Returns detailed status information including deployment environments and their states.")
    public ProjectStatusInfo getProjectStatus(
        @ToolParam(description = "The ID of the project to get status for") long projectId) {

        try {
            ProjectDTO projectDTO = projectFacade.getProject(projectId);

            List<ProjectDeploymentStatusInfo> deploymentStatuses = projectDeploymentFacade
                .getWorkspaceProjectDeployments(
                    Objects.requireNonNull(projectDTO.workspaceId()), null, projectId, null, false)
                .stream()
                .map(ProjectTools::getProjectDeploymentStatusInfo)
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "getProjectStatus({}): Retrieved status for project {} with {} deployments", projectId, projectId,
                    deploymentStatuses.size());
            }

            return new ProjectStatusInfo(
                projectDTO.id(), projectDTO.name(), getLastStatusName(projectDTO),
                projectDTO.lastPublishedDate() != null,
                projectDTO.lastProjectVersion(), projectDTO.lastPublishedDate(), deploymentStatuses);
        } catch (Exception e) {
            log.error("getProjectStatus({}): Failed to get status for project {}", projectId, projectId, e);

            throw new ExecutionException(
                "Failed to get project status: " + e.getMessage(), e, ProjectErrorType.GET_PROJECT_STATUS);
        }
    }

    @Tool(
        description = "Create a new project with workflows. Returns the created project information including id, name, description, and status.")
    public ProjectInfo createProject(
        @ToolParam(description = "The name of the new project") String name,
        @ToolParam(required = false, description = "The description of the new project") String description,
        @ToolParam(required = false, description = "The category ID for the project") Long categoryId,
        @ToolParam(
            required = false, description = AutomationToolInvocationContext.WORKSPACE_ID_DESCRIPTION) Long workspaceId,
        @ToolParam(required = false, description = "The tag IDs to associate with the project") List<Long> tagIds,
        ToolContext toolContext) {

        long resolvedWorkspaceId = requireWorkspaceId(workspaceId, toolContext, ProjectErrorType.CREATE_PROJECT);

        try {
            String trimmedDescription = description == null ? null : description.trim();

            long projectId = projectFacade.createProject(
                ProjectDTO.builder()
                    .category(getCategory(categoryId, resolvedWorkspaceId))
                    .description(
                        trimmedDescription == null || trimmedDescription.isEmpty() ? null : trimmedDescription)
                    .name(name)
                    .tags(getTags(tagIds))
                    .workspaceId(resolvedWorkspaceId)
                    .build());

            ProjectDTO projectDTO = projectFacade.getProject(projectId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "createProject({}, {}, {}, {}, {}): Created project {} with name '{}'", name, description,
                    categoryId, resolvedWorkspaceId, tagIds, projectDTO.id(), projectDTO.name());
            }

            return getProjectInfo(projectDTO);
        } catch (Exception e) {
            log.error(
                "createProject({}, {}, {}, {}, {}): Failed to create project with name '{}'", name, description,
                categoryId, resolvedWorkspaceId, tagIds, name, e);

            throw new ExecutionException(
                "Failed to create project: " + e.getMessage(), e, ProjectErrorType.CREATE_PROJECT);
        }
    }

    @Tool(
        description = "Update project settings and metadata. Returns the updated project information including id, name, description, and status.")
    public ProjectInfo updateProject(
        @ToolParam(description = "The ID of the project to update") long projectId,
        @ToolParam(required = false, description = "The new name of the project") String name,
        @ToolParam(required = false, description = "The new description of the project") String description,
        @ToolParam(required = false, description = "The new category ID for the project") Long categoryId,
        @ToolParam(required = false, description = "The new tag IDs to associate with the project") List<Long> tagIds) {

        try {
            ProjectDTO existingProjectDTO = projectFacade.getProject(projectId);

            String trimmedName = name == null ? null : name.trim();
            long workspaceId = Objects.requireNonNull(existingProjectDTO.workspaceId());

            projectFacade.updateProject(
                ProjectDTO.builder()
                    .category(
                        categoryId == null ? existingProjectDTO.category() : getCategory(categoryId, workspaceId))
                    .description(description == null ? existingProjectDTO.description() : description)
                    .id(existingProjectDTO.id())
                    .name(trimmedName == null || trimmedName.isEmpty() ? existingProjectDTO.name() : trimmedName)
                    .projectVersions(existingProjectDTO.projectVersions())
                    .tags(tagIds == null ? existingProjectDTO.tags() : getTags(tagIds))
                    .version(existingProjectDTO.version())
                    .workspaceId(workspaceId)
                    .build());

            ProjectDTO updatedProjectDTO = projectFacade.getProject(projectId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "updateProject({}, {}, {}, {}, {}): Updated project {} with name '{}'", projectId, name,
                    description, categoryId, tagIds, updatedProjectDTO.id(), updatedProjectDTO.name());
            }

            return getProjectInfo(updatedProjectDTO);
        } catch (Exception e) {
            log.error(
                "updateProject({}, {}, {}, {}, {}): Failed to update project {}", projectId, name, description,
                categoryId, tagIds, projectId, e);

            throw new ExecutionException(
                "Failed to update project: " + e.getMessage(), e, ProjectErrorType.UPDATE_PROJECT);
        }
    }

    @Tool(
        description = "Delete a project and all its workflows. Returns a confirmation message.")
    public String deleteProject(
        @ToolParam(description = "The ID of the project to delete") long projectId) {

        try {
            ProjectDTO projectDTO = projectFacade.getProject(projectId);

            String projectName = projectDTO.name();

            projectFacade.deleteProject(projectId);

            if (log.isDebugEnabled()) {
                log.debug("deleteProject({}): Deleted project {} with name '{}'", projectId, projectId, projectName);
            }

            return "Project '" + projectName + "' (ID: " + projectId +
                ") has been successfully deleted along with all its workflows.";
        } catch (Exception e) {
            log.error("deleteProject({}): Failed to delete project {}", projectId, projectId, e);

            throw new ExecutionException(
                "Failed to delete project: " + e.getMessage(), e, ProjectErrorType.DELETE_PROJECT);
        }
    }

    @Tool(
        description = "Publish a project version for deployment. Returns the published project version information.")
    public ProjectPublishInfo publishProject(
        @ToolParam(description = "The ID of the project to publish") long projectId,
        @ToolParam(required = false, description = "The description for this published version") String description) {

        try {
            int publishedVersion = projectFacade.publishProject(projectId, description, false);

            ProjectDTO updatedProjectDTO = projectFacade.getProject(projectId);

            if (log.isDebugEnabled()) {
                log.debug(
                    "publishProject({}, {}): Published project {} version {} with description '{}'", projectId,
                    description, projectId, publishedVersion, description);
            }

            return new ProjectPublishInfo(
                updatedProjectDTO.id(), updatedProjectDTO.name(), publishedVersion, description,
                updatedProjectDTO.lastPublishedDate());
        } catch (Exception e) {
            log.error("publishProject({}, {}): Failed to publish project {}", projectId, description, projectId, e);

            throw new ExecutionException(
                "Failed to publish project: " + e.getMessage(), e, ProjectErrorType.PUBLISH_PROJECT);
        }
    }

    private long requireWorkspaceId(
        @Nullable Long workspaceId, @Nullable ToolContext toolContext, ProjectErrorType projectErrorType) {

        Long resolvedWorkspaceId;

        try {
            resolvedWorkspaceId = AutomationToolInvocationContext.resolveWorkspaceId(
                workspaceId, toolContext, workspaceFacade);
        } catch (WorkspaceNotAccessibleException exception) {
            throw new ExecutionException(exception.getMessage(), exception, projectErrorType);
        }

        if (resolvedWorkspaceId == null) {
            throw new ExecutionException(
                "workspaceId is required - call listWorkspaces to find the workspace id.", projectErrorType);
        }

        return resolvedWorkspaceId;
    }

    private @Nullable Category getCategory(@Nullable Long categoryId, long workspaceId) {
        if (categoryId == null) {
            return null;
        }

        return projectCategoryFacade.getProjectCategories(workspaceId)
            .stream()
            .filter(category -> Objects.equals(category.getId(), categoryId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Category " + categoryId + " not found"));
    }

    private List<Tag> getTags(@Nullable List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = projectTagFacade.getProjectTags()
            .stream()
            .filter(tag -> tagIds.contains(tag.getId()))
            .toList();

        if (tags.size() != tagIds.size()) {
            throw new IllegalArgumentException("One or more tags not found: " + tagIds);
        }

        return tags;
    }

    private List<ProjectDTO> getWorkspaceProjects(long workspaceId) {
        return projectFacade.getWorkspaceProjects(null, null, false, null, null, null, workspaceId);
    }

    private static boolean contains(@Nullable String value, String lowerQuery) {
        return value != null && value.toLowerCase()
            .contains(lowerQuery);
    }

    private static ProjectDeploymentStatusInfo getProjectDeploymentStatusInfo(
        ProjectDeploymentDTO projectDeploymentDTO) {

        Environment environment = projectDeploymentDTO.environment();

        return new ProjectDeploymentStatusInfo(
            projectDeploymentDTO.id(), environment.name(), projectDeploymentDTO.enabled(),
            projectDeploymentDTO.enabled(), projectDeploymentDTO.createdDate(),
            projectDeploymentDTO.lastModifiedDate());
    }

    private static ProjectInfo getProjectInfo(ProjectDTO projectDTO) {
        return new ProjectInfo(
            projectDTO.id(), projectDTO.name(), projectDTO.description(), getStatusName(projectDTO),
            projectDTO.createdDate(), projectDTO.lastModifiedDate());
    }

    private static String getLastStatusName(ProjectDTO projectDTO) {
        ProjectVersion.Status lastStatus = projectDTO.lastStatus();

        return lastStatus.name();
    }

    private static String getStatusName(ProjectDTO projectDTO) {
        ProjectVersion.Status status = projectDTO.lastPublishedDate() == null
            ? ProjectVersion.Status.DRAFT : ProjectVersion.Status.PUBLISHED;

        return status.name();
    }

    private static List<Long> getTagIds(@Nullable List<Tag> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
            .map(Tag::getId)
            .toList();
    }
}
