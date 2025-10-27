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

package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.Project;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomProjectRepositoryImpl implements CustomProjectRepository {

    private final JdbcClient jdbcClient;

    public CustomProjectRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Project> findAllProjects(
        Boolean apiCollections, Long categoryId, Boolean projectDeployments, Long tagId, Integer status,
        Long workspaceId) {

        List<Object> arguments = new ArrayList<>();
        String query = "SELECT DISTINCT project.*, LOWER(project.name) AS lower_name FROM project ";

        if (apiCollections != null) {
            query += """
                JOIN project_version ON project.id = project_version.project_id
                JOIN project_workflow ON project.id = project_workflow.project_id
                JOIN workflow ON project_workflow.workflow_id = workflow.id
                """;
        }

        if (projectDeployments != null && projectDeployments) {
            query += "JOIN project_deployment ON project.id = project_deployment.project_id ";
        }

        if (tagId != null) {
            query += "JOIN project_tag ON project.id = project_tag.project_id ";
        }

        query += "WHERE project.name NOT LIKE '__EMBEDDED__%' ";

        if (apiCollections != null) {
            if (apiCollections) {
                query += "AND workflow.definition LIKE '%newApiRequest%' ";
            } else {
                query += "AND workflow.definition NOT LIKE '%newApiRequest%' ";
            }
        }

        if (projectDeployments != null && !projectDeployments) {
            query += "AND project.id NOT IN (SELECT project_deployment.project_id FROM project_deployment) ";
        }

        if (workspaceId != null || categoryId != null || tagId != null || status != null) {
            query += "AND ";
        }

        if (workspaceId != null) {
            arguments.add(workspaceId);

            query += "workspace_id = ? ";
        }

        if (categoryId != null) {
            arguments.add(categoryId);

            if (workspaceId != null) {
                query += "AND ";
            }

            query += "category_id = ? ";
        }

        if (tagId != null) {
            arguments.add(tagId);

            if (workspaceId != null || categoryId != null) {
                query += "AND ";
            }

            query += "tag_id = ? ";
        }

        if (status != null) {
            arguments.add(status);

            if (workspaceId != null || categoryId != null || tagId != null) {
                query += "AND ";
            }

            query += "project_version.status = ? ";
        }

        query += "ORDER BY lower_name ASC";

        List<Project> projects = jdbcClient.sql(query)
            .params(arguments)
            .query(Project.class)
            .list();

        for (Project project : projects) {
            project.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT project_tag.tag_id FROM project_tag WHERE project_id = ?")
                    .param(project.getId())
                    .query(Long.class)
                    .list());

            project.setProjectVersions(
                jdbcClient
                    .sql(
                        "SELECT project_version.version, project_version.status, project_version.published_date, project_version.description FROM project_version WHERE project_id = ?")
                    .param(project.getId())
                    .query(ProjectVersion.class)
                    .list()
                    .stream()
                    .map(projectVersion -> new com.bytechef.automation.configuration.domain.ProjectVersion(
                        projectVersion.version, projectVersion.status, projectVersion.publishedDate,
                        projectVersion.description))
                    .toList());
        }

        return projects;
    }

    private record ProjectVersion(int version, int status, Instant publishedDate, String description) {
    }
}
