/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.COLUMNS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.NAME;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ROWS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TABLE;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.VALUES;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import com.bytechef.platform.component.util.SqlUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class InsertJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    @Override
    public Map<String, Integer> execute(Map<String, ?> inputParameters, DataSource dataSource) {
        List<Map<String, ?>> columns = MapUtils.getList(inputParameters, COLUMNS, new TypeReference<>() {}, List.of());
        Map<Object, ?> valuesMap = MapUtils.getMap(inputParameters, VALUES);

        List<Map<String, Object>> rows = MapUtils.getList(valuesMap, ROWS, new TypeReference<>() {}, List.of());

        String schema = MapUtils.getString(inputParameters, SCHEMA, "public");
        String table = MapUtils.getRequiredString(inputParameters, TABLE);

        List<String> columnNames = columns.stream()
            .map(columnMap -> (String) columnMap.get(NAME))
            .collect(Collectors.toList());

        String values = columnNames.stream()
            .map(column -> ":" + column)
            .collect(Collectors.joining(","));

        SqlUtils.checkColumnTypes(schema, table, rows, dataSource);

        int[] rowsAffected = JdbcExecutor.batchUpdate(
            String.format("INSERT INTO %s.%s (%s) VALUES(%s)", schema, table, String.join(",", columnNames), values),
            SqlParameterSourceUtils.createBatch(rows.toArray()), dataSource);

        IntStream stream = Arrays.stream(rowsAffected);

        return Map.of(ROWS, stream.sum());
    }
}
