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

import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.EXECUTE;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PARAMETERS;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
public class ExecuteJdbcTaskHandler extends AbstractJdbcTaskHandler implements TaskHandler<Map<String, Integer>> {

    public ExecuteJdbcTaskHandler(DataSourceFactory dataSourceFactory, PlatformTransactionManager transactionManager) {
        super(dataSourceFactory, transactionManager);
    }

    @Override
    public Map<String, Integer> handle(TaskExecution taskExecution) throws Exception {
        String executeStatement = taskExecution.getRequiredString(EXECUTE);
        Map<String, ?> paramMap = taskExecution.getMap(PARAMETERS, Map.of());

        NamedParameterJdbcTemplate jdbcTemplate = createNamedParameterJdbcTemplate(taskExecution);

        int rowsAffected = jdbcTemplate.update(executeStatement, paramMap);

        return Map.of("rows", rowsAffected);
    }
}
