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

package com.bytechef.mcp.tool;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool for listing all projects in ByteChef.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectTools {

    private static final Logger logger = LoggerFactory.getLogger(ProjectTools.class);

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectTools(ProjectService projectService, ProjectWorkflowService projectWorkflowService) {
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Tool(
        description = "List all projects in ByteChef. Returns a list of projects with their basic information including id, name, description, and status.")
    public List<ProjectInfo> listProjects() {
        try {
            List<Project> projects = projectService.getProjects();

            List<ProjectInfo> projectInfos = projects.stream()
                .map(project -> {
                    Instant lastPublishedDate = project.getLastPublishedDate();

                    Status lastStatus = Status.PUBLISHED;

                    if (lastPublishedDate == null) {
                        lastStatus = Status.DRAFT;
                    }

                    return new ProjectInfo(
                        project.getId(), project.getName(), project.getDescription(), lastStatus.name(),
                        project.getCreatedDate()
                            .toString(),
                        project.getLastModifiedDate()
                            .toString());
                })
                .collect(Collectors.toList());

            logger.info("Found {} projects", projectInfos.size());

            return projectInfos;
        } catch (Exception e) {
            logger.error("Failed to list projects", e);
        }

        return List.of();
    }

    @Tool(
        description = "Create a new workflow in a ByteChef project. Returns the created workflow information including id, project id, workflow id, and reference code.")
    public ProjectWorkflowInfo createProjectWorkflow(
        @JsonProperty("projectId") @JsonPropertyDescription("The ID of the project to add the workflow to") long projectId,
        @JsonProperty("projectVersion") @JsonPropertyDescription("The version of the project (typically 1 for new projects)") int projectVersion,
        @JsonProperty("workflowId") @JsonPropertyDescription("The unique identifier for the workflow") String workflowId,
        @JsonProperty("workflowReferenceCode") @JsonPropertyDescription("The reference code for the workflow (optional)") String workflowReferenceCode) {

        try {
            ProjectWorkflow projectWorkflow;

            if (workflowReferenceCode != null && !workflowReferenceCode.trim()
                .isEmpty()) {
                projectWorkflow =
                    projectWorkflowService.addWorkflow(projectId, projectVersion, workflowId, workflowReferenceCode);
            } else {
                projectWorkflow = projectWorkflowService.addWorkflow(projectId, projectVersion, workflowId);
            }

            logger.info("Created workflow {} for project {} version {}", workflowId, projectId, projectVersion);

            return new ProjectWorkflowInfo(
                projectWorkflow.getId(),
                projectWorkflow.getProjectId(),
                projectWorkflow.getProjectVersion(),
                projectWorkflow.getWorkflowId(),
                projectWorkflow.getWorkflowReferenceCode(),
                projectWorkflow.getCreatedDate() != null ? projectWorkflow.getCreatedDate()
                    .toString() : null,
                projectWorkflow.getLastModifiedDate() != null ? projectWorkflow.getLastModifiedDate()
                    .toString() : null);
        } catch (Exception e) {
            logger.error("Failed to create workflow {} for project {} version {}", workflowId, projectId,
                projectVersion, e);
            throw new RuntimeException("Failed to create project workflow: " + e.getMessage(), e);
        }
    }

    /**
     * Project workflow information record for the response.
     */
    public record ProjectWorkflowInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project workflow") Long id,
        @JsonProperty("project_id") @JsonPropertyDescription("The ID of the project this workflow belongs to") long projectId,
        @JsonProperty("project_version") @JsonPropertyDescription("The version of the project") int projectVersion,
        @JsonProperty("workflow_id") @JsonPropertyDescription("The unique identifier of the workflow") String workflowId,
        @JsonProperty("workflow_reference_code") @JsonPropertyDescription("The reference code of the workflow") String workflowReferenceCode,
        @JsonProperty("created_date") @JsonPropertyDescription("When the workflow was created") String createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the workflow was last modified") String lastModifiedDate) {
    }

    /**
     * Project information record for the response.
     */
    public record ProjectInfo(
        @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the project") Long id,
        @JsonProperty("name") @JsonPropertyDescription("The name of the project") String name,
        @JsonProperty("description") @JsonPropertyDescription("The description of the project") String description,
        @JsonProperty("status") @JsonPropertyDescription("The current status of the project") String status,
        @JsonProperty("created_date") @JsonPropertyDescription("When the project was created") String createdDate,
        @JsonProperty("last_modified_date") @JsonPropertyDescription("When the project was last modified") String lastModifiedDate) {
    }
}
