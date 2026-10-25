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

package com.bytechef.commons.data.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
public final class DatabaseDialect {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDialect.class);

    private @Nullable Boolean h2;

    public boolean isH2(JdbcTemplate jdbcTemplate) {
        Boolean resolvedH2 = h2;

        if (resolvedH2 == null) {
            resolvedH2 = resolveH2(jdbcTemplate);

            h2 = resolvedH2;
        }

        return resolvedH2;
    }

    private static boolean resolveH2(JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate.getDataSource();

        if (dataSource == null) {
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            String databaseProductName = databaseMetaData.getDatabaseProductName();

            databaseProductName = databaseProductName.toLowerCase(Locale.ROOT);

            return databaseProductName.contains("h2");
        } catch (SQLException sqlException) {
            log.warn("Unable to determine the database product name, assuming it is not H2", sqlException);

            return false;
        }
    }
}
