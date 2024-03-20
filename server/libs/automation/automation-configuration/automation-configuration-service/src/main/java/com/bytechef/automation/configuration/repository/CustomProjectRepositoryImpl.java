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

import com.bytechef.automation.configuration.domain.Project;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
public class CustomProjectRepositoryImpl implements CustomProjectRepository {

    private final JdbcClient jdbcClient;

    @SuppressFBWarnings("EI")
    public CustomProjectRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Project> findAllProjects(Long categoryId, List<Long> ids, Long tagId, Integer status) {
        List<Object> arguments = new ArrayList<>();
        String query = "SELECT project.* FROM project ";

        if (tagId != null) {
            query += "JOIN project_tag ON project.id = project_tag.project_id ";
        }

        if (categoryId != null || !ids.isEmpty() || tagId != null || status != null) {
            query += "WHERE ";
        }

        if (categoryId != null) {
            arguments.add(categoryId);

            query += "category_id = ? ";
        }

        if (!ids.isEmpty()) {
            arguments.addAll(ids);

            if (categoryId != null) {
                query += "AND ";
            }

            StringBuilder idsString = new StringBuilder();

            for (int i = 0; i < ids.size(); i++) {
                idsString.append("?");

                if (i < ids.size() - 1) {
                    idsString.append(",");
                }
            }

            query += "project.id in (%s) ".formatted(idsString.toString());
        }

        if (tagId != null) {
            arguments.add(tagId);

            if (categoryId != null || !ids.isEmpty()) {
                query += "AND ";
            }

            query += "tag_id = ? ";
        }

        if (status != null) {
            arguments.add(status);

            if (categoryId != null || !ids.isEmpty() || tagId != null) {
                query += "AND ";
            }

            query += "status = ? ";
        }

        query += "ORDER BY project.name ASC, project.status DESC";

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
            project.setWorkflowIds(
                jdbcClient
                    .sql(
                        "SELECT project_workflow.workflow_id FROM project_workflow WHERE project_id = ?")
                    .param(project.getId())
                    .query(String.class)
                    .list());
        }

        return projects;
    }
}
