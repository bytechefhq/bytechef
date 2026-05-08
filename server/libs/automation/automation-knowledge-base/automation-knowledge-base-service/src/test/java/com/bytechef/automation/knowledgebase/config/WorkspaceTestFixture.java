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

package com.bytechef.automation.knowledgebase.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Helper for seeding a {@code workspace} row in KB integration tests. Knowledge bases now require a non-null
 * {@code workspace_id} with FK to {@code workspace}, but this module does not depend on
 * {@code automation-configuration} where the {@code Workspace} entity and repository live. Inserting via raw
 * {@link JdbcTemplate} keeps the test fixture self-contained.
 *
 * <p>
 * The class-level {@link SuppressFBWarnings} suppresses {@code OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE} for the
 * lambda passed to {@link JdbcTemplate#update} — Spring's {@code JdbcTemplate} closes the {@code PreparedStatement}
 * returned from a {@code PreparedStatementCreator}, but SpotBugs cannot see across that contract.
 * </p>
 */
@SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
public final class WorkspaceTestFixture {

    private WorkspaceTestFixture() {
    }

    public static long seedWorkspace(JdbcTemplate jdbcTemplate, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO workspace " +
                    "(name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                    "VALUES (?, NOW(), 'test', NOW(), 'test', 0)",
                new String[] {
                    "id"
            });

            statement.setString(1, name);

            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException("Failed to seed workspace row");
        }

        return key.longValue();
    }

    public static void deleteAllWorkspaces(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DELETE FROM workspace");
    }
}
