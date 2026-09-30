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

package com.bytechef.platform.data.table.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Which engine the data tables live on. Three statements in this module are Postgres-only -- the RETURNING clause the
 * row service builds, the {@code NULLS NOT DISTINCT} partial index {@code createTable} adds, and the
 * {@code pg_total_relation_size} usage roll-up -- and each needs the same question answered.
 *
 * <p>
 * Asked once per data source and cached: the answer cannot change for the life of one. Failing to answer resolves to
 * "not H2", so an unreadable data source keeps the Postgres behaviour that production runs on rather than silently
 * degrading it.
 *
 * @author Ivica Cardic
 */
public final class DataTableDialect {

    private static final Logger log = LoggerFactory.getLogger(DataTableDialect.class);

    private Boolean h2;

    /**
     * True when the statements above have to fall back, i.e. the engine is H2.
     */
    public boolean isH2(JdbcTemplate jdbcTemplate) {
        if (h2 == null) {
            h2 = resolveH2(jdbcTemplate);
        }

        return h2;
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
