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

package com.integri.atlas.task.handler.commons.jdbc;

import static com.integri.atlas.task.handler.commons.jdbc.JdbcTaskConstants.PROPERTY_DELETE_KEY;
import static com.integri.atlas.task.handler.commons.jdbc.JdbcTaskConstants.PROPERTY_ROWS;
import static com.integri.atlas.task.handler.commons.jdbc.JdbcTaskConstants.PROPERTY_SCHEMA;
import static com.integri.atlas.task.handler.commons.jdbc.JdbcTaskConstants.PROPERTY_TABLE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
public class DeleteJdbcTaskHandler extends AbstractJdbcTaskHandler implements TaskHandler<Map<String, Integer>> {

    public DeleteJdbcTaskHandler(DataSourceFactory dataSourceFactory, PlatformTransactionManager transactionManager) {
        super(dataSourceFactory, transactionManager);
    }

    @Override
    public Map<String, Integer> handle(TaskExecution taskExecution) throws Exception {
        Map<String, Integer> result;

        String deleteKey = taskExecution.getString(PROPERTY_DELETE_KEY, "id");
        List<Map<String, ?>> rows = taskExecution.get(PROPERTY_ROWS, List.class, Collections.emptyList());
        String schema = taskExecution.getString(PROPERTY_SCHEMA, "public");
        String table = taskExecution.getRequiredString(PROPERTY_TABLE);

        NamedParameterJdbcTemplate jdbcTemplate = createNamedParameterJdbcTemplate(taskExecution);

        if (rows.isEmpty()) {
            result = Map.of("rows", 0);
        } else {
            int[] rowsAffected = transactionTemplate.execute(status ->
                jdbcTemplate.batchUpdate(
                    "DELETE FROM %s.%s WHERE %s=:%s".formatted(schema, table, deleteKey, deleteKey),
                    SqlParameterSourceUtils.createBatch(rows.toArray())
                )
            );

            result = Map.of("rows", Arrays.stream(rowsAffected).sum());
        }

        return result;
    }
}
