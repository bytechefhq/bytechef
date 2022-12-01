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

package com.bytechef.hermes.component.registrar.jdbc.executor.operation;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.registrar.jdbc.executor.JdbcExecutor;
import com.bytechef.hermes.component.registrar.jdbc.executor.constants.JdbcConstants;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class QueryJdbcOperation implements JdbcOperation<List<Map<String, Object>>> {

    private final JdbcExecutor jdbcExecutor;

    public QueryJdbcOperation(JdbcExecutor jdbcExecutor) {
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public List<Map<String, Object>> execute(Context context, ExecutionParameters executionParameters) {
        String queryStatement = executionParameters.getRequiredString(JdbcConstants.QUERY);
        Map<String, ?> paramMap = executionParameters.getMap(JdbcConstants.PARAMETERS, Map.of());

        return jdbcExecutor.query(
                context.getConnectionParameters(), queryStatement, paramMap, (ResultSet rs, int rowNum) -> {
                    Map<String, Object> row = new HashMap<>();

                    ResultSetMetaData rsMetaData = rs.getMetaData();
                    int columnCount = rsMetaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsMetaData.getColumnName(i);

                        row.put(columnName, rs.getObject(i));
                    }

                    return row;
                });
    }
}
