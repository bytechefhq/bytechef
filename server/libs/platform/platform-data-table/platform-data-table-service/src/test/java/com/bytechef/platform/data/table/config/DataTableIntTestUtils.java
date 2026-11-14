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

package com.bytechef.platform.data.table.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
public final class DataTableIntTestUtils {

    private DataTableIntTestUtils() {
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public static void dropAll(JdbcTemplate jdbcTemplate) {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema() "
                + "AND table_name LIKE 'dt\\_%'",
            String.class);

        for (String tableName : tableNames) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
        }

        jdbcTemplate.update("DELETE FROM data_table_webhook");
        jdbcTemplate.update("DELETE FROM data_table_tag");
        jdbcTemplate.update("DELETE FROM data_table");
    }
}
