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

package com.bytechef.ai.mcp.tool.automation.impl;

import com.bytechef.ai.mcp.tool.automation.api.ProjectTools;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.configuration.domain.Environment;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The ProjectTools class provides a suite of tools for managing and interacting with projects in the ByteChef platform.
 * It encompasses functionalities such as project creation, retrieval, update, deletion, publishing, and searching,
 * along with deployment status management. This class integrates with services to enable project workflows and
 * deployment configurations.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(name = "bytechef.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ProjectToolsImpl implements ProjectTools {

    private static final Logger logger = LoggerFactory.getLogger(ProjectToolsImpl.class);

    private final ProjectService projectService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ProjectToolsImpl(ProjectService projectService, ProjectDeploymentService projectDeploymentService) {
        this.projectService = projectService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    @Tool(
        description = "List all projects in ByteChef. Returns a list of projects with their basic information including id, name, description, and status.")
    public List<ProjectToolsImpl.ProjectInfo> listProjects() {
        try {
            List<Project> projects = projectService.getProjects();

            List<ProjectToolsImpl.ProjectInfo> projectInfos = projects.stream()
                .map(ProjectToolsImpl::getProjectInfo)
                .collect(Collectors.toList());

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} projects", projectInfos.size());
            }

            return projectInfos;
        } catch (Exception e) {
            logger.error("Failed to list projects", e);

            throw new RuntimeException("Failed to list projects: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Get comprehensive information about a specific project. Returns detailed project information including id, name, description, status, versions, and metadata.")
    public ProjectToolsImpl.ProjectDetailInfo getProject(
        @ToolParam(description = "The ID of the project to retrieve") long projectId) {

        try {
            Project project = projectService.getProject(projectId);
            List<ProjectVersion> projectVersions = projectService.getProjectVersions(projectId);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved project {} with {} versions", projectId, projectVersions.size());
            }

            ProjectVersion.Status lastStatus = project.getLastStatus();
            List<ProjectToolsImpl.ProjectVersionInfo> projectVersionInfos = projectVersions.stream()
                .map(pv -> {
                    ProjectVersion.Status status = pv.getStatus();

                    return new ProjectToolsImpl.ProjectVersionInfo(
                        pv.getVersion(), status.name(), pv.getDescription(),
                        pv.getPublishedDate() != null ? pv.getPublishedDate() : null);
                })
                .collect(Collectors.toList());

            return new ProjectToolsImpl.ProjectDetailInfo(
                project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
                project.getCategoryId(), project.getWorkspaceId(), project.getTagIds(),
                projectVersionInfos, project.getCreatedDate(), project.getLastModifiedDate(),
                project.getLastPublishedDate() != null ? project.getLastPublishedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to get project {}", projectId, e);

            throw new RuntimeException("Failed to get project: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Full-text search across all projects. Returns a list of projects matching the search query in name or description.")
    public List<ProjectToolsImpl.ProjectInfo> searchProjects(
        @ToolParam(description = "The search query to match against project names and descriptions") String query) {

        try {
            List<Project> allProjects = projectService.getProjects();
            query = query.toLowerCase();

            String lowerQuery = query.trim();

            List<ProjectToolsImpl.ProjectInfo> matchingProjects = allProjects.stream()
                .filter(project -> {
                    String name = project.getName();

                    name = name != null ? name.toLowerCase() : "";

                    String description = project.getDescription();

                    description = description != null ? description.toLowerCase() : "";

                    return name.contains(lowerQuery) || description.contains(lowerQuery);
                })
                .map(ProjectToolsImpl::getProjectInfo)
                .collect(Collectors.toList());

            if (logger.isDebugEnabled()) {
                logger.debug("Found {} projects matching query '{}'", matchingProjects.size(), query);
            }

            return matchingProjects;
        } catch (Exception e) {
            logger.error("Failed to search projects with query '{}'", query, e);
            throw new RuntimeException("Failed to search projects: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Get project deployment and execution status. Returns detailed status information including deployment environments and their states.")
    public ProjectToolsImpl.ProjectStatusInfo getProjectStatus(
        @ToolParam(description = "The ID of the project to get status for") long projectId) {

        try {
            Project project = projectService.getProject(projectId);
            List<ProjectDeployment> deployments = projectDeploymentService.getProjectDeployments(projectId);

            List<ProjectToolsImpl.ProjectDeploymentStatusInfo> deploymentStatuses = deployments.stream()
                .map(deployment -> {
                    Environment environment = deployment.getEnvironment();

                    return new ProjectToolsImpl.ProjectDeploymentStatusInfo(
                        deployment.getId(), environment.name(), deployment.isEnabled(),
                        projectDeploymentService.isProjectDeploymentEnabled(deployment.getId()),
                        deployment.getCreatedDate() != null ? deployment.getCreatedDate() : null,
                        deployment.getLastModifiedDate() != null ? deployment.getLastModifiedDate() : null);
                })
                .collect(Collectors.toList());

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved status for project {} with {} deployments", projectId,
                    deploymentStatuses.size());
            }

            ProjectVersion.Status lastStatus = project.getLastStatus();
            return new ProjectToolsImpl.ProjectStatusInfo(
                project.getId(), project.getName(), lastStatus.name(), project.isPublished(),
                project.getLastProjectVersion(),
                project.getLastPublishedDate() != null ? project.getLastPublishedDate() : null, deploymentStatuses);
        } catch (Exception e) {
            logger.error("Failed to get status for project {}", projectId, e);
            throw new RuntimeException("Failed to get project status: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Create a new project with workflows. Returns the created project information including id, name, description, and status.")
    public ProjectInfo createProject(
        @ToolParam(description = "The name of the new project") String name,
        @ToolParam(required = false, description = "The description of the new project") String description,
        @ToolParam(required = false, description = "The category ID for the project") Long categoryId,
        @ToolParam(required = false, description = "The workspace ID for the project") Long workspaceId,
        @ToolParam(required = false, description = "The tag IDs to associate with the project") List<Long> tagIds) {

        try {
            Project.Builder projectBuilder = Project.builder()
                .name(name);

            if (description != null) {
                description = description.trim();

                if (!description.isEmpty()) {
                    projectBuilder.description(description);
                }
            }

            if (categoryId != null) {
                projectBuilder.categoryId(categoryId);
            }

            if (workspaceId != null) {
                projectBuilder.workspaceId(workspaceId);
            } else {
                projectBuilder.workspaceId(Workspace.DEFAULT_WORKSPACE_ID);
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                projectBuilder.tagIds(tagIds);
            }

            Project project = projectService.create(projectBuilder.build());

            if (logger.isDebugEnabled()) {
                logger.debug("Created project {} with name '{}'", project.getId(), project.getName());
            }

            Status lastStatus = project.getLastStatus();
            return new ProjectInfo(
                project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
                project.getCreatedDate(), project.getLastModifiedDate());
        } catch (Exception e) {
            logger.error("Failed to create project with name '{}'", name, e);

            throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Update project settings and metadata. Returns the updated project information including id, name, description, and status.")
    public ProjectInfo updateProject(
        @ToolParam(description = "The ID of the project to update") long projectId,
        @ToolParam(required = false, description = "The new name of the project") String name,
        @ToolParam(required = false, description = "The new description of the project") String description,
        @ToolParam(required = false, description = "The new category ID for the project") Long categoryId,
        @ToolParam(required = false, description = "The new tag IDs to associate with the project") List<Long> tagIds) {

        try {
            Project existingProject = projectService.getProject(projectId);

            if (name != null) {
                name = name.trim();
            }

            existingProject.setName(name != null && !name.isEmpty() ? name : existingProject.getName());
            existingProject.setDescription(description != null ? description : existingProject.getDescription());
            existingProject.setCategoryId(categoryId != null ? categoryId : existingProject.getCategoryId());

            if (tagIds != null) {
                existingProject.setTagIds(tagIds);
            }

            Project updatedProject = projectService.update(existingProject);

            if (logger.isDebugEnabled()) {
                logger.debug("Updated project {} with name '{}'", updatedProject.getId(), updatedProject.getName());
            }

            Status lastStatus = updatedProject.getLastStatus();

            return new ProjectInfo(
                updatedProject.getId(), updatedProject.getName(), updatedProject.getDescription(), lastStatus.name(),
                updatedProject.getCreatedDate(), updatedProject.getLastModifiedDate());
        } catch (Exception e) {
            logger.error("Failed to update project {}", projectId, e);

            throw new RuntimeException("Failed to update project: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Delete a project and all its workflows. Returns a confirmation message.")
    public String deleteProject(
        @ToolParam(description = "The ID of the project to delete") long projectId) {

        try {
            Project project = projectService.getProject(projectId);
            String projectName = project.getName();

            projectService.delete(projectId);

            if (logger.isDebugEnabled()) {
                logger.debug("Deleted project {} with name '{}'", projectId, projectName);
            }

            return "Project '" + projectName + "' (ID: " + projectId +
                ") has been successfully deleted along with all its workflows.";
        } catch (Exception e) {
            logger.error("Failed to delete project {}", projectId, e);

            throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
        }
    }

    @Override
    @Tool(
        description = "Publish a project version for deployment. Returns the published project version information.")
    public ProjectPublishInfo publishProject(
        @ToolParam(description = "The ID of the project to publish") long projectId,
        @ToolParam(required = false, description = "The description for this published version") String description) {

        try {
            int publishedVersion = projectService.publishProject(projectId, description, false);

            Project updatedProject = projectService.getProject(projectId);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Published project {} version {} with description '{}'", projectId, publishedVersion, description);
            }

            return new ProjectPublishInfo(
                updatedProject.getId(), updatedProject.getName(), publishedVersion, description,
                updatedProject.getLastPublishedDate() != null ? updatedProject.getLastPublishedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to publish project {}", projectId, e);
            throw new RuntimeException("Failed to publish project: " + e.getMessage(), e);
        }
    }

    private static ProjectInfo getProjectInfo(Project project) {
        Instant lastPublishedDate = project.getLastPublishedDate();

        Status lastStatus = Status.PUBLISHED;

        if (lastPublishedDate == null) {
            lastStatus = Status.DRAFT;
        }

        return new ProjectInfo(
            project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
            project.getCreatedDate(), project.getLastModifiedDate());
    }

    /**
     * Project information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
        @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the project") String description,
        @JsonProperty("status") @JsonPropertyDescription("The current status of the project") String status,
        @JsonProperty("created_date") @JsonPropertyDescription("When the project was created") Instant createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the project was last modified") Instant lastModifiedDate) {
    }

    /**
     * Detailed project information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectDetailInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
        @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the project") String description,
        @JsonProperty("status") @JsonPropertyDescription("The current status of the project") String status,
        @JsonProperty("category_id") @JsonPropertyDescription("The category ID of the project") Long categoryId,
        @JsonProperty("workspace_id") @JsonPropertyDescription("The workspace ID of the project") Long workspaceId,
        @JsonProperty("tag_ids") @JsonPropertyDescription("The tag IDs associated with the project") List<Long> tagIds,
        @JsonProperty("versions") @JsonPropertyDescription("The versions of the project") List<ProjectToolsImpl.ProjectVersionInfo> versions,
        @JsonProperty("created_date") @JsonPropertyDescription("When the project was created") Instant createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the project was last modified") Instant lastModifiedDate,
        @JsonProperty("last_published_date") @JsonPropertyDescription("When the project was last published") Instant lastPublishedDate) {
    }

    /**
     * Project version information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectVersionInfo(
        @JsonProperty("version") @JsonPropertyDescription("The version number") int version,
        @JsonProperty("status") @JsonPropertyDescription("The status of the version") String status,
        @JsonProperty("description") @JsonPropertyDescription("The description of the version") String description,
        @JsonProperty("published_date") @JsonPropertyDescription("When the version was published") Instant publishedDate) {
    }

    /**
     * Project status information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectStatusInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
        @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
        @JsonProperty("status") @JsonPropertyDescription("The current status of the project") String status,
        @JsonProperty("is_published") @JsonPropertyDescription("Whether the project is published") boolean isPublished,
        @JsonProperty("last_version") @JsonPropertyDescription("The last version number") int lastVersion,
        @JsonProperty("last_published_date") @JsonPropertyDescription("When the project was last published") Instant lastPublishedDate,
        @JsonProperty("deployments") @JsonPropertyDescription("The deployment status information") List<ProjectToolsImpl.ProjectDeploymentStatusInfo> deployments) {
    }

    /**
     * Project deployment status information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectDeploymentStatusInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the deployment") Long id,
        @JsonProperty("environment") @JsonPropertyDescription("The deployment environment") String environment,
        @JsonProperty("enabled") @JsonPropertyDescription("Whether the deployment is enabled") boolean enabled,
        @JsonProperty("is_deployment_enabled") @JsonPropertyDescription("Whether the deployment is currently enabled") boolean isDeploymentEnabled,
        @JsonProperty("created_date") @JsonPropertyDescription("When the deployment was created") Instant createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the deployment was last modified") Instant lastModifiedDate) {
    }

    /**
     * Project publish information record for the response.
     */
    @SuppressFBWarnings("EI")
    public record ProjectPublishInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
        @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
        @JsonProperty("published_version") @JsonPropertyDescription("The published version number") int publishedVersion,
        @JsonProperty("description") @JsonPropertyDescription("The description of the published version") String description,
        @JsonProperty("published_date") @JsonPropertyDescription("When the version was published") Instant publishedDate) {
    }
}
