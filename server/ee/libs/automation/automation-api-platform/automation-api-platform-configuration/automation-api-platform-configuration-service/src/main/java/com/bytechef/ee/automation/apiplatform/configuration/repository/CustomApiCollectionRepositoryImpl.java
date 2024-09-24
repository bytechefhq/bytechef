/*
 * Copyright 2023-present ByteChef Inc.
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
    public List<ApiCollection> findAllApiCollections(
        Long workspaceId, Integer environment, Long projectId, Long tagId) {

        List<Object> arguments = new ArrayList<>();

        String query = "SELECT api_collection.* FROM api_collection ";

        query += "JOIN project_instance ON api_collection.project_instance_id = project_instance.id ";

        if (workspaceId != null) {
            query += "JOIN project ON project_instance.project_id = project.id ";
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

        query += "ORDER BY LOWER(api_collection.name) ASC, project_instance.project_version ASC, " +
            "project_instance.environment ASC";

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
}
