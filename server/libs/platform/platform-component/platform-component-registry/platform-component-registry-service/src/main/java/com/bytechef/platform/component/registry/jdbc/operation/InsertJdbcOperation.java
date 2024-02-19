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

package com.bytechef.platform.component.registry.jdbc.operation;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.registry.jdbc.JdbcExecutor;
import com.bytechef.platform.component.registry.jdbc.constant.JdbcConstants;
import com.fasterxml.jackson.core.type.TypeReference;
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
    public Map<String, Integer> execute(Map<String, ?> inputParameters, Map<String, ?> connectionParameters) {
        List<String> columns = MapUtils.getList(inputParameters, JdbcConstants.COLUMNS, String.class, List.of());
        List<Map<String, ?>> rows = MapUtils.getList(
            inputParameters, JdbcConstants.ROWS, new TypeReference<>() {}, List.of());
        String schema = MapUtils.getString(inputParameters, JdbcConstants.SCHEMA, "public");
        String table = MapUtils.getRequiredString(inputParameters, JdbcConstants.TABLE);

        int[] rowsAffected = jdbcExecutor.batchUpdate(
            connectionParameters,
            "INSERT INTO "
                + schema
                + "."
                + table
                + " ("
                + String.join(",", columns)
                + ") VALUES( "
                + String.join(
                    ",",
                    columns.stream()
                        .map(column -> ":" + column)
                        .toList())
                + ")",
            SqlParameterSourceUtils.createBatch(rows.toArray()));

        return Map.of("rows", Arrays.stream(rowsAffected)
            .sum());
    }
}
