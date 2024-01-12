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

package com.bytechef.platform.component.jdbc.executor;

import com.bytechef.platform.component.jdbc.sql.DataSourceFactory;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
public class JdbcExecutor {

    private final String databaseJdbcName;
    private final DataSourceFactory dataSourceFactory;
    private final String jdbcDriverClassName;

    public JdbcExecutor(String databaseJdbcName, DataSourceFactory dataSourceFactory, String jdbcDriverClassName) {
        this.databaseJdbcName = databaseJdbcName;
        this.dataSourceFactory = dataSourceFactory;
        this.jdbcDriverClassName = jdbcDriverClassName;
    }

    public int[] batchUpdate(Map<String, ?> connectionParameters, String sql, SqlParameterSource[] batchArgs) {
        DataSource dataSource = dataSourceFactory.getDataSource(
            connectionParameters, databaseJdbcName, jdbcDriverClassName);

        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        return transactionTemplate.execute(status -> jdbcTemplate.batchUpdate(sql, batchArgs));
    }

    public <T> List<T> query(
        Map<String, ?> connectionParameters, String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper)
        throws DataAccessException {

        NamedParameterJdbcTemplate jdbcTemplate = getJdbcTemplate(connectionParameters);

        return jdbcTemplate.query(sql, paramMap, rowMapper);
    }

    public int update(
        Map<String, ?> connectionParameters, String sql, Map<String, ?> paramMap) throws DataAccessException {

        NamedParameterJdbcTemplate jdbcTemplate = getJdbcTemplate(connectionParameters);

        return jdbcTemplate.update(sql, paramMap);
    }

    private NamedParameterJdbcTemplate getJdbcTemplate(Map<String, ?> connectionParameters) {
        return new NamedParameterJdbcTemplate(
            dataSourceFactory.getDataSource(connectionParameters, databaseJdbcName, jdbcDriverClassName));
    }
}
