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

package com.bytechef.platform.component.jdbc.operation;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.util.SqlUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

/**
 * @author Ivica Cardic
 */
public class UpdateJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    @Override
    public Map<String, Integer> execute(Map<String, ?> inputParameters, DataSource dataSource) {
        List<String> columns = MapUtils.getList(inputParameters, JdbcConstants.COLUMNS, String.class, List.of());
        List<Map<String, Object>> rows = MapUtils.getList(
            inputParameters, JdbcConstants.ROWS, new TypeReference<>() {}, List.of());
        String schema = MapUtils.getString(inputParameters, JdbcConstants.SCHEMA, "public");
        String table = MapUtils.getRequiredString(inputParameters, JdbcConstants.TABLE);
        String updateKey = MapUtils.getString(inputParameters, JdbcConstants.UPDATE_KEY, "id");

        String set = String.join(
            " AND ",
            columns.stream()
                .map(column -> column + "=:" + column)
                .toList());

        SqlUtils.checkColumnTypes(schema, table, rows, dataSource);

        int[] rowsAffected = JdbcExecutor.batchUpdate(
            "UPDATE " + schema + "." + table + " SET " + set + " WHERE " + updateKey + "=:" + updateKey,
            SqlParameterSourceUtils.createBatch(rows.toArray()), dataSource);

        IntStream stream = Arrays.stream(rowsAffected);

        return Map.of("rows", stream.sum());
    }
}
