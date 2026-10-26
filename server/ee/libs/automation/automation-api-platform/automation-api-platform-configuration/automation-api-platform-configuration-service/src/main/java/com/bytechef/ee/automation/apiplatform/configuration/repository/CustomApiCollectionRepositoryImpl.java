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

package com.bytechef.ee.automation.apiplatform.configuration.repository;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomApiCollectionRepositoryImpl implements CustomApiCollectionRepository {

    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomApiCollectionRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean existsByNameAndWorkspaceIdAndEnvironment(
        String name, long workspaceId, int environment, @Nullable Long excludeId) {

        List<Object> arguments = new ArrayList<>(List.of(name, workspaceId, environment));

        String query = """
            SELECT COUNT(api_collection.id) FROM api_collection
            JOIN project_deployment ON api_collection.project_deployment_id = project_deployment.id
            JOIN project ON project_deployment.project_id = project.id
            WHERE api_collection.name = ? AND project.workspace_id = ? AND project_deployment.environment = ?
            """;

        if (excludeId != null) {
            arguments.add(excludeId);

            query += " AND api_collection.id <> ?";
        }

        Long count = jdbcClient.sql(query)
            .params(arguments)
            .query(Long.class)
            .single();

        return count > 0;
    }

    @Override
    public List<ApiCollection> findAllApiCollections(
        Long workspaceId, Integer environment, Long projectId, Long tagId) {

        List<Object> arguments = new ArrayList<>();

        String query = "SELECT api_collection.* FROM api_collection ";

        query += "JOIN project_deployment ON api_collection.project_deployment_id = project_deployment.id ";

        if (workspaceId != null) {
            query += "JOIN project ON project_deployment.project_id = project.id ";
        }

        if (tagId != null) {
            query += "JOIN api_collection_tag ON api_collection.id = api_collection_tag.api_collection_id ";
        }

        if (workspaceId != null || environment != null || projectId != null || tagId != null) {
            query += "WHERE ";
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

        query += "ORDER BY LOWER(api_collection.name) ASC, project_deployment.project_version ASC, " +
            "project_deployment.environment ASC";

        List<ApiCollection> apiCollections = jdbcClient.sql(query)
            .params(arguments)
            .query(ApiCollection.class)
            .list();

        for (ApiCollection apiCollection : apiCollections) {
            apiCollection.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT api_collection_tag.tag_id FROM api_collection_tag WHERE api_collection_id = ?")
                    .param(apiCollection.getId())
                    .query(Long.class)
                    .list());
        }

        return apiCollections;
    }

    @Override
    public Optional<ApiCollection> findByUuidAndEnvironment(UUID uuid, int environment) {
        List<ApiCollection> apiCollections = jdbcClient
            .sql("""
                SELECT api_collection.* FROM api_collection
                JOIN project_deployment ON api_collection.project_deployment_id = project_deployment.id
                WHERE api_collection.uuid = ? AND project_deployment.environment = ?
                ORDER BY api_collection.id ASC
                """)
            .params(List.of(uuid, environment))
            .query(ApiCollection.class)
            .list();

        return apiCollections.isEmpty() ? Optional.empty() : Optional.of(apiCollections.getFirst());
    }
}
