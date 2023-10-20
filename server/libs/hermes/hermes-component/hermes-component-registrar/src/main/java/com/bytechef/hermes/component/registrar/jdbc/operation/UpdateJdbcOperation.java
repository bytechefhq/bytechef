
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

package com.bytechef.hermes.component.registrar.jdbc.operation;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.registrar.jdbc.JdbcExecutor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

/**
 * @author Ivica Cardic
 */
public class UpdateJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    private final JdbcExecutor jdbcExecutor;

    public UpdateJdbcOperation(JdbcExecutor jdbcExecutor) {
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public Map<String, Integer> execute(Context context, InputParameters inputParameters) {
        List<String> columns = inputParameters.getList(JdbcConstants.COLUMNS, String.class, List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> rows = (List) inputParameters.getList(JdbcConstants.ROWS, Map.class, List.of());
        String schema = inputParameters.getString(JdbcConstants.SCHEMA, "public");
        String table = inputParameters.getRequiredString(JdbcConstants.TABLE);
        String updateKey = inputParameters.getString(JdbcConstants.UPDATE_KEY, "id");

        int[] rowsAffected = jdbcExecutor.batchUpdate(
            context.getConnection(),
            "UPDATE "
                + schema
                + "."
                + table
                + " SET "
                + String.join(
                    " AND ",
                    columns.stream()
                        .map(column -> column + "=:" + column)
                        .toList())
                + " WHERE "
                + updateKey
                + "=:"
                + updateKey,
            SqlParameterSourceUtils.createBatch(rows.toArray()));

        return Map.of("rows", Arrays.stream(rowsAffected)
            .sum());
    }
}
