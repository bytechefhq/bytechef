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

package com.bytechef.task.jdbc;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
public class InsertJdbcTaskHandler extends AbstractJdbcTaskHandler implements TaskHandler<Map<String, Integer>> {

    public InsertJdbcTaskHandler(DataSourceFactory dataSourceFactory, PlatformTransactionManager transactionManager) {
        super(dataSourceFactory, transactionManager);
    }

    @Override
    public Map<String, Integer> handle(TaskExecution taskExecution) {
        List<String> columns = taskExecution.getList(JdbcTaskConstants.COLUMNS, String.class, List.of());
        List<Map<String, ?>> rows = taskExecution.get(JdbcTaskConstants.ROWS, List.class, List.of());
        String schema = taskExecution.getString(JdbcTaskConstants.SCHEMA, "public");
        String table = taskExecution.getRequiredString(JdbcTaskConstants.TABLE);

        NamedParameterJdbcTemplate jdbcTemplate = createNamedParameterJdbcTemplate(taskExecution);

        int[] rowsAffected = transactionTemplate.execute(status -> jdbcTemplate.batchUpdate(
                "INSERT INTO "
                        + schema
                        + "."
                        + table
                        + " ("
                        + String.join(",", columns)
                        + ") VALUES( "
                        + String.join(
                                ",",
                                columns.stream().map(column -> ":" + column).toList())
                        + ")",
                SqlParameterSourceUtils.createBatch(rows.toArray())));

        return Map.of("rows", Arrays.stream(rowsAffected).sum());
    }
}
