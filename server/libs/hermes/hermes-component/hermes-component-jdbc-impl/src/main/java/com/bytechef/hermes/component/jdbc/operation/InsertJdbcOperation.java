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

package com.bytechef.hermes.component.jdbc.operation;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.jdbc.JdbcExecutor;
import com.bytechef.hermes.component.jdbc.constants.JdbcConstants;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

/**
 * @author Ivica Cardic
 */
public class InsertJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    private final JdbcExecutor jdbcExecutor;

    public InsertJdbcOperation(JdbcExecutor jdbcExecutor) {
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public Map<String, Integer> execute(Context context, ExecutionParameters executionParameters) {
        List<String> columns = executionParameters.getList(JdbcConstants.COLUMNS, List.of());
        List<Map<String, ?>> rows = executionParameters.getList(JdbcConstants.ROWS, List.of());
        String schema = executionParameters.getString(JdbcConstants.SCHEMA, "public");
        String table = executionParameters.getRequiredString(JdbcConstants.TABLE);

        int[] rowsAffected = jdbcExecutor.batchUpdate(
                context.getConnection(),
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
                SqlParameterSourceUtils.createBatch(rows.toArray()));

        return Map.of("rows", Arrays.stream(rowsAffected).sum());
    }
}
