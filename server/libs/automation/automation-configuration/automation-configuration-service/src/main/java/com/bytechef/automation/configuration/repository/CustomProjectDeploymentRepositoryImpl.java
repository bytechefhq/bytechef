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
import com.bytechef.automation.configuration.domain.SystemProjects;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomProjectDeploymentRepositoryImpl implements CustomProjectDeploymentRepository {

    private final JdbcClient jdbcClient;

    public CustomProjectDeploymentRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
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

        if (embedded != null) {
            query += "WHERE ";

            // Escaped via SystemProjects rather than spliced literally: the marker's own underscores are LIKE
            // single-character wildcards, so the unescaped '__EMBEDDED__%' this used to emit also matched any name
            // of the form <2 chars>EMBEDDED<2 chars>... -- '__EMBEDDED_AUTOMATION__x' and an ordinary user project
            // called 'MyEMBEDDEDxyz' alike. The former merely double-covered a row SystemProjects already hides;
            // the latter silently vanished from every non-embedded deployment listing.
            if (embedded) {
                query += SystemProjects.likeCondition(
                    "project.name", SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX);
            } else {
                query += SystemProjects.notLikeCondition(
                    "project.name", SystemProjects.EMBEDDED_DEPLOYMENT_NAME_PREFIX);
            }
        }

        if (environment != null || projectId != null || tagId != null || workspaceId != null) {
            if (embedded == null) {
                query += "WHERE ";
            } else {
                query += "AND ";
            }
        }

        if (workspaceId != null) {
            arguments.add(workspaceId);

            query += "workspace_id = ? ";
        }

        if (environment != null) {
            arguments.add(environment);

            if (workspaceId != null) {
                query += "AND ";
            }

            query += "environment = ? ";
        }

        if (projectId != null) {
            arguments.add(projectId);

            if (workspaceId != null || environment != null) {
                query += "AND ";
            }

            query += "project_id = ? ";
        }

        if (tagId != null) {
            arguments.add(tagId);

            if (workspaceId != null || environment != null || projectId != null) {
                query += "AND ";
            }

            query += "tag_id = ? ";
        }

        // project_deployment.name markers are a different namespace from project.name markers (see
        // ApiCollectionFacadeImpl / McpProjectFacadeImpl), so they are excluded individually rather than through
        // SystemProjects.projectNameNotLikePredicates.
        query += SystemProjects.notLikePredicate(
            "project_deployment.name", SystemProjects.API_COLLECTION_DEPLOYMENT_NAME_PREFIX);
        query += SystemProjects.notLikePredicate(
            "project_deployment.name", SystemProjects.MCP_SERVER_DEPLOYMENT_NAME_PREFIX);
        query += SystemProjects.notLikePredicate(
            "project_deployment.name", SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX);
        query += SystemProjects.projectNameNotLikePredicates("project.name");

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
