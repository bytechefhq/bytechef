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
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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
public class ProjectTools extends ChatProjectTools {

    private static final Logger logger = LoggerFactory.getLogger(ProjectTools.class);

    @SuppressFBWarnings("EI")
    public ProjectTools(ProjectService projectService, ProjectDeploymentService projectDeploymentService) {
        super(projectService, projectDeploymentService);
    }

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
}
