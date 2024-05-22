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

package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomProjectInstanceRepositoryImpl implements CustomProjectInstanceRepository {

    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomProjectInstanceRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<ProjectInstance> findAllProjectInstances(
        Long workspaceId, Integer environment, Long projectId, Long tagId) {

        List<Object> arguments = new ArrayList<>();

        String query = "SELECT project_instance.* FROM project_instance ";

        if (workspaceId != null) {
            query += "JOIN project ON project_instance.project_id = project.id ";
        }

        if (tagId != null) {
            query += "JOIN project_instance_tag ON project_instance.id = project_instance_tag.project_instance_id ";
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

        query += "ORDER BY LOWER(project_instance.name) ASC, project_instance.enabled DESC";

        List<ProjectInstance> projectInstances = jdbcClient.sql(query)
            .params(arguments)
            .query(ProjectInstance.class)
            .list();

        for (ProjectInstance projectInstance : projectInstances) {
            projectInstance.setTagIds(
                jdbcClient
                    .sql(
                        "SELECT project_instance_tag.tag_id FROM project_instance_tag WHERE project_instance_id = ?")
                    .param(projectInstance.getId())
                    .query(Long.class)
                    .list());
        }

        return projectInstances;
    }
}
