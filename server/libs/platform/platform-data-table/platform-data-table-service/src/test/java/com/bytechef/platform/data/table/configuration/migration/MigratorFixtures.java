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

package com.bytechef.platform.data.table.configuration.migration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "SQL_INJECTION_SPRING_JDBC", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"
})
final class MigratorFixtures {

    private MigratorFixtures() {
    }

    static void createLinkTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS workspace_data_table (id BIGSERIAL PRIMARY KEY, data_table_id BIGINT NOT NULL, "
                + "workspace_id BIGINT NOT NULL, created_date TIMESTAMP NOT NULL, created_by VARCHAR(50) NOT NULL, "
                + "last_modified_date TIMESTAMP NOT NULL, last_modified_by VARCHAR(50) NOT NULL, version BIGINT NOT NULL)");
    }

    static void dropLinkTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS workspace_data_table");
    }

    static void dropAll(JdbcTemplate jdbcTemplate) {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema() "
                + "AND table_name LIKE 'dt\\_%'",
            String.class);

        for (String tableName : tableNames) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
        }

        dropLinkTable(jdbcTemplate);

        List<Long> tagIds = jdbcTemplate.queryForList("SELECT DISTINCT tag_id FROM data_table_tag", Long.class);

        jdbcTemplate.update("DELETE FROM data_table_webhook");
        jdbcTemplate.update("DELETE FROM data_table_tag");
        jdbcTemplate.update("DELETE FROM data_table");

        for (Long tagId : tagIds) {
            jdbcTemplate.update("DELETE FROM tag WHERE id = ?", tagId);
        }
    }

    static long insertDataTable(JdbcTemplate jdbcTemplate, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO data_table (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                    + "VALUES (?, ?, 'test', ?, 'test', 0)",
                new String[] {
                    "id"
            });

            preparedStatement.setString(1, name);
            preparedStatement.setTimestamp(2, Timestamp.from(Instant.now()));
            preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));

            return preparedStatement;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey())
            .longValue();
    }

    static void setWorkspaceId(JdbcTemplate jdbcTemplate, long dataTableId, long workspaceId) {
        jdbcTemplate.update("UPDATE data_table SET workspace_id = ? WHERE id = ?", workspaceId, dataTableId);
    }

    static void linkToWorkspace(JdbcTemplate jdbcTemplate, long dataTableId, long workspaceId) {
        jdbcTemplate.update(
            "INSERT INTO workspace_data_table (data_table_id, workspace_id, created_date, created_by, "
                + "last_modified_date, last_modified_by, version) VALUES (?, ?, CURRENT_TIMESTAMP, 'test', "
                + "CURRENT_TIMESTAMP, 'test', 0)",
            dataTableId, workspaceId);
    }

    static void createLegacyTable(JdbcTemplate jdbcTemplate, String physicalName, String title) {
        jdbcTemplate.execute(
            "CREATE TABLE \"" + physicalName + "\" (\"id\" BIGSERIAL PRIMARY KEY, \"external_id\" VARCHAR(255), "
                + "\"title\" VARCHAR(255))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX ON \"" + physicalName + "\" (\"external_id\")");
        jdbcTemplate.update("INSERT INTO \"" + physicalName + "\" (\"title\") VALUES (?)", title);
    }

    static void addTag(JdbcTemplate jdbcTemplate, long dataTableId, long tagId) {
        jdbcTemplate.update(
            "INSERT INTO tag (id, name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
            tagId, "tag-" + tagId);

        jdbcTemplate.update("INSERT INTO data_table_tag (data_table_id, tag_id) VALUES (?, ?)", dataTableId, tagId);
    }

    static void addWebhook(JdbcTemplate jdbcTemplate, long dataTableId, String url) {
        jdbcTemplate.update(
            "INSERT INTO data_table_webhook (data_table_id, url, type, environment, created_date, created_by, "
                + "last_modified_date, last_modified_by, version) VALUES (?, ?, 0, 0, CURRENT_TIMESTAMP, 'test', "
                + "CURRENT_TIMESTAMP, 'test', 0)",
            dataTableId, url);
    }

    static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?",
            Integer.class, tableName);

        return count != null && count > 0;
    }

    static List<String> titles(JdbcTemplate jdbcTemplate, String physicalName) {
        return jdbcTemplate.queryForList("SELECT \"title\" FROM \"" + physicalName + "\"", String.class);
    }

    static Long workspaceIdOf(JdbcTemplate jdbcTemplate, long dataTableId) {
        return jdbcTemplate.queryForObject("SELECT workspace_id FROM data_table WHERE id = ?", Long.class, dataTableId);
    }

    static List<Long> tagIds(JdbcTemplate jdbcTemplate, long dataTableId) {
        return jdbcTemplate.queryForList(
            "SELECT tag_id FROM data_table_tag WHERE data_table_id = ?", Long.class, dataTableId);
    }

    static List<String> webhookUrls(JdbcTemplate jdbcTemplate, long dataTableId) {
        return jdbcTemplate.queryForList(
            "SELECT url FROM data_table_webhook WHERE data_table_id = ?", String.class, dataTableId);
    }
}
