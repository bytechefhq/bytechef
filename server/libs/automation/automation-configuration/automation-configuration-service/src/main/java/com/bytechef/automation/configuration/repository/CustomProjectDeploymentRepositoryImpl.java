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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomProjectDeploymentRepositoryImpl implements CustomProjectDeploymentRepository {

    private final JdbcClient jdbcClient;
    private final ProjectOwnershipService projectOwnershipService;

    public CustomProjectDeploymentRepositoryImpl(
        JdbcClient jdbcClient, ProjectOwnershipService projectOwnershipService) {

        this.jdbcClient = jdbcClient;
        this.projectOwnershipService = projectOwnershipService;
    }

    @Override
    public List<ProjectDeployment> findAllProjectDeployments(
        Boolean embedded, Integer environment, Long projectId, Long tagId, Long workspaceId) {

        List<Object> arguments = new ArrayList<>();

        String query = """
            SELECT project_deployment.* FROM project_deployment
            JOIN project ON project_deployment.project_id = project.id
            """;

        if (tagId != null) {
            query +=
                "JOIN project_deployment_tag ON project_deployment.id = project_deployment_tag.project_deployment_id ";
        }

        List<String> conditions = new ArrayList<>();

        conditions.add(
            projectOwnershipService.getExcludeProjectDeploymentsCondition(
                EnumSet.complementOf(EnumSet.of(ProjectDeploymentOwner.EMBEDDED)),
                "project_deployment.id"));

        if (embedded != null) {
            if (embedded) {
                conditions.add(
                    projectOwnershipService.getSelectProjectDeploymentsCondition(
                        ProjectDeploymentOwner.EMBEDDED, "project_deployment.id"));
            } else {
                conditions.add(
                    projectOwnershipService.getExcludeProjectDeploymentsCondition(
                        EnumSet.of(ProjectDeploymentOwner.EMBEDDED), "project_deployment.id"));
                conditions.add(
                    projectOwnershipService.getExcludeProjectsCondition(
                        EnumSet.allOf(ProjectOwner.class), "project_deployment.project_id"));
            }
        }

        if (workspaceId != null) {
            arguments.add(workspaceId);

            conditions.add("workspace_id = ?");
        }

        if (environment != null) {
            arguments.add(environment);

            conditions.add("environment = ?");
        }

        if (projectId != null) {
            arguments.add(projectId);

            conditions.add("project_id = ?");
        }

        if (tagId != null) {
            arguments.add(tagId);

            conditions.add("tag_id = ?");
        }

        query += "WHERE " + String.join(" AND ", conditions) + " ";

        query += "ORDER BY LOWER(project_deployment.name) ASC, project_deployment.project_version ASC, " +
            "project_deployment.environment ASC";

        List<ProjectDeployment> projectDeployments = jdbcClient.sql(query)
            .params(arguments)
            .query(ProjectDeployment.class)
            .list();

        for (ProjectDeployment projectDeployment : projectDeployments) {
            projectDeployment.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT project_deployment_tag.tag_id FROM project_deployment_tag WHERE project_deployment_id = ?")
                    .param(projectDeployment.getId())
                    .query(Long.class)
                    .list());
        }

        return projectDeployments;
    }
}
