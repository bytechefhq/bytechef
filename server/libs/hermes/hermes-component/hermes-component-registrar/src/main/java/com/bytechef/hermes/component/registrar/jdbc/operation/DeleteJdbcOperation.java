
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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.registrar.jdbc.JdbcExecutor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bytechef.hermes.component.registrar.jdbc.constant.JdbcConstants;
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
    public Map<String, Integer> execute(Context context, Parameters parameters) {
        Map<String, Integer> result;

        String deleteKey = parameters.getString(JdbcConstants.DELETE_KEY, "id");
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> rows = (List) parameters.getList(JdbcConstants.ROWS, Map.class, Collections.emptyList());
        String schema = parameters.getString(JdbcConstants.SCHEMA, "public");
        String table = parameters.getRequiredString(JdbcConstants.TABLE);

        if (rows.isEmpty()) {
            result = Map.of("rows", 0);
        } else {
            int[] rowsAffected = jdbcExecutor.batchUpdate(
                OptionalUtils.get(context.fetchConnection()),
                "DELETE FROM %s.%s WHERE %s=:%s".formatted(schema, table, deleteKey, deleteKey),
                SqlParameterSourceUtils.createBatch(rows.toArray()));

            result = Map.of("rows", Arrays.stream(rowsAffected)
                .sum());
        }

        return result;
    }
}
