/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.commons.jdbc;

import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PARAMETERS;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.QUERY;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
public class QueryJdbcTaskHandler extends AbstractJdbcTaskHandler implements TaskHandler<List<Map<String, ?>>> {

    public QueryJdbcTaskHandler(DataSourceFactory dataSourceFactory, PlatformTransactionManager transactionManager) {
        super(dataSourceFactory, transactionManager);
    }

    @Override
    public List<Map<String, ?>> handle(TaskExecution taskExecution) throws Exception {
        String queryStatement = taskExecution.getRequiredString(QUERY);
        Map<String, ?> paramMap = taskExecution.getMap(PARAMETERS, Map.of());

        NamedParameterJdbcTemplate jdbcTemplate = createNamedParameterJdbcTemplate(taskExecution);

        return jdbcTemplate.query(
            queryStatement,
            paramMap,
            (ResultSet rs, int rowNum) -> {
                Map<String, Object> row = new HashMap<>();

                ResultSetMetaData rsMetaData = rs.getMetaData();
                int columnCount = rsMetaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsMetaData.getColumnName(i);

                    row.put(columnName, rs.getObject(i));
                }

                return row;
            }
        );
    }
}
