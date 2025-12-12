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

package com.bytechef.ai.mcp.tool.automation;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectVersion;
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

public class ChatProjectTools {
    private static final Logger logger = LoggerFactory.getLogger(ChatProjectTools.class);

    protected final ProjectService projectService;
    protected final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ChatProjectTools(ProjectService projectService, ProjectDeploymentService projectDeploymentService) {
        this.projectService = projectService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Tool(
        description = "List all projects in ByteChef. Returns a list of projects with their basic information including id, name, description, and status.")
    public List<ProjectInfo> listProjects() {
        try {
            List<Project> projects = projectService.getProjects();

            List<ProjectInfo> projectInfos = projects.stream()
                .map(project -> {
                    Instant lastPublishedDate = project.getLastPublishedDate();

                    ProjectVersion.Status lastStatus = ProjectVersion.Status.PUBLISHED;

                    if (lastPublishedDate == null) {
                        lastStatus = ProjectVersion.Status.DRAFT;
                    }

                    return new ProjectInfo(
                        project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
                        project.getCreatedDate(), project.getLastModifiedDate());
                })
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

    @Tool(
        description = "Get comprehensive information about a specific project. Returns detailed project information including id, name, description, status, versions, and metadata.")
    public ProjectDetailInfo getProject(
        @ToolParam(description = "The ID of the project to retrieve") long projectId) {

        try {
            Project project = projectService.getProject(projectId);
            List<ProjectVersion> projectVersions = projectService.getProjectVersions(projectId);

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved project {} with {} versions", projectId, projectVersions.size());
            }

            ProjectVersion.Status lastStatus = project.getLastStatus();
            List<ProjectVersionInfo> projectVersionInfos = projectVersions.stream()
                .map(pv -> {
                    ProjectVersion.Status status = pv.getStatus();

                    return new ProjectVersionInfo(
                        pv.getVersion(), status.name(), pv.getDescription(),
                        pv.getPublishedDate() != null ? pv.getPublishedDate() : null);
                })
                .collect(Collectors.toList());

            return new ProjectDetailInfo(
                project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
                project.getCategoryId(), project.getWorkspaceId(), project.getTagIds(),
                projectVersionInfos, project.getCreatedDate(), project.getLastModifiedDate(),
                project.getLastPublishedDate() != null ? project.getLastPublishedDate() : null);
        } catch (Exception e) {
            logger.error("Failed to get project {}", projectId, e);

            throw new RuntimeException("Failed to get project: " + e.getMessage(), e);
        }
    }

    @Tool(
        description = "Full-text search across all projects. Returns a list of projects matching the search query in name or description.")
    public List<ProjectInfo> searchProjects(
        @ToolParam(description = "The search query to match against project names and descriptions") String query) {

        try {
            List<Project> allProjects = projectService.getProjects();
            query = query.toLowerCase();

            String lowerQuery = query.trim();

            List<ProjectInfo> matchingProjects = allProjects.stream()
                .filter(project -> {
                    String name = project.getName();

                    name = name != null ? name.toLowerCase() : "";

                    String description = project.getDescription();

                    description = description != null ? description.toLowerCase() : "";

                    return name.contains(lowerQuery) || description.contains(lowerQuery);
                })
                .map(project -> {
                    Instant lastPublishedDate = project.getLastPublishedDate();
                    ProjectVersion.Status lastStatus = ProjectVersion.Status.PUBLISHED;

                    if (lastPublishedDate == null) {
                        lastStatus = ProjectVersion.Status.DRAFT;
                    }

                    return new ProjectInfo(
                        project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
                        project.getCreatedDate(), project.getLastModifiedDate());
                })
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

    @Tool(
        description = "Get project deployment and execution status. Returns detailed status information including deployment environments and their states.")
    public ProjectStatusInfo getProjectStatus(
        @ToolParam(description = "The ID of the project to get status for") long projectId) {

        try {
            Project project = projectService.getProject(projectId);
            List<ProjectDeployment> deployments = projectDeploymentService.getProjectDeployments(projectId);

            List<ProjectDeploymentStatusInfo> deploymentStatuses = deployments.stream()
                .map(deployment -> {
                    Environment environment = deployment.getEnvironment();

                    return new ProjectDeploymentStatusInfo(
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
            return new ProjectStatusInfo(
                project.getId(), project.getName(), lastStatus.name(), project.isPublished(),
                project.getLastProjectVersion(),
                project.getLastPublishedDate() != null ? project.getLastPublishedDate() : null, deploymentStatuses);
        } catch (Exception e) {
            logger.error("Failed to get status for project {}", projectId, e);
            throw new RuntimeException("Failed to get project status: " + e.getMessage(), e);
        }
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
        @JsonProperty("versions") @JsonPropertyDescription("The versions of the project") List<ProjectTools.ProjectVersionInfo> versions,
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
        @JsonProperty("deployments") @JsonPropertyDescription("The deployment status information") List<ProjectTools.ProjectDeploymentStatusInfo> deployments) {
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
