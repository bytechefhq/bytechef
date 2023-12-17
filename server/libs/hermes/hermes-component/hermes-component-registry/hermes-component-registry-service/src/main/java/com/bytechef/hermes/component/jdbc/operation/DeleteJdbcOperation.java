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

package com.bytechef.hermes.component.jdbc.operation;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
import com.bytechef.hermes.component.jdbc.executor.JdbcExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

/**
 * @author Ivica Cardic
 */
public class DeleteJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    private final JdbcExecutor jdbcExecutor;

    public DeleteJdbcOperation(JdbcExecutor jdbcExecutor) {
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public Map<String, Integer> execute(Map<String, ?> inputParameters, Map<String, ?> connectionParameters) {
        Map<String, Integer> result;

        String deleteKey = MapUtils.getString(inputParameters, JdbcConstants.DELETE_KEY, "id");
        List<Map<String, ?>> rows = MapUtils.getList(
            inputParameters, JdbcConstants.ROWS, new TypeReference<>() {}, Collections.emptyList());
        String schema = MapUtils.getString(inputParameters, JdbcConstants.SCHEMA, "public");
        String table = MapUtils.getRequiredString(inputParameters, JdbcConstants.TABLE);

        if (rows.isEmpty()) {
            result = Map.of("rows", 0);
        } else {
            int[] rowsAffected = jdbcExecutor.batchUpdate(
                connectionParameters,
                "DELETE FROM %s.%s WHERE %s=:%s".formatted(schema, table, deleteKey, deleteKey),
                SqlParameterSourceUtils.createBatch(rows.toArray()));

            result = Map.of("rows", Arrays.stream(rowsAffected)
                .sum());
        }

        return result;
    }
}
