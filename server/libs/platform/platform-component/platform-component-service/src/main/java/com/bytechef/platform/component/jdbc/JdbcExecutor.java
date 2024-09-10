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

package com.bytechef.platform.component.jdbc;

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

    public static int[] batchUpdate(String sql, SqlParameterSource[] batchArgs, DataSource dataSource) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        return transactionTemplate.execute(status -> jdbcTemplate.batchUpdate(sql, batchArgs));
    }

    public static <T> List<T> query(
        String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper, DataSource dataSource)
        throws DataAccessException {

        NamedParameterJdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);

        return jdbcTemplate.query(sql, paramMap, rowMapper);
    }

    public static int update(String sql, Map<String, ?> paramMap, DataSource dataSource) throws DataAccessException {
        NamedParameterJdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);

        return jdbcTemplate.update(sql, paramMap);
    }

    private static NamedParameterJdbcTemplate getJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
