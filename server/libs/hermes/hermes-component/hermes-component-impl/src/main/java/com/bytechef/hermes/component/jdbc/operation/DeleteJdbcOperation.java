
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

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.jdbc.executor.JdbcExecutor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
import org.springframework.core.ParameterizedTypeReference;
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
    public Map<String, Integer> execute(Context context, Map<String, ?> inputParameters) {
        Map<String, Integer> result;

        String deleteKey = MapValueUtils.getString(inputParameters, JdbcConstants.DELETE_KEY, "id");
        List<Map<String, ?>> rows = MapValueUtils.getList(
            inputParameters, JdbcConstants.ROWS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        String schema = MapValueUtils.getString(inputParameters, JdbcConstants.SCHEMA, "public");
        String table = MapValueUtils.getRequiredString(inputParameters, JdbcConstants.TABLE);

        if (rows.isEmpty()) {
            result = Map.of("rows", 0);
        } else {
            int[] rowsAffected = jdbcExecutor.batchUpdate(
                context.getConnection(),
                "DELETE FROM %s.%s WHERE %s=:%s".formatted(schema, table, deleteKey, deleteKey),
                SqlParameterSourceUtils.createBatch(rows.toArray()));

            result = Map.of("rows", Arrays.stream(rowsAffected)
                .sum());
        }

        return result;
    }
}
