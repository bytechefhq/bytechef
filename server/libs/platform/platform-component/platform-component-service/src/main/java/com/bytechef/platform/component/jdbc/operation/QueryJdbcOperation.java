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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * @author Ivica Cardic
 */
public class QueryJdbcOperation implements JdbcOperation<List<Map<String, Object>>> {

    public record QueryColumn(String name, int type) {
    }

    public record QueryResult(List<QueryColumn> columns, List<Map<String, Object>> rows) {

        public QueryResult {
            columns = List.copyOf(columns);
            rows = List.copyOf(rows);
        }
    }

    public QueryResult executeWithColumns(Map<String, ?> inputParameters, DataSource dataSource) {
        Map<String, ?> paramMap = MapUtils.getMap(inputParameters, JdbcConstants.PARAMETERS, Map.of());
        String queryStatement = MapUtils.getRequiredString(inputParameters, JdbcConstants.QUERY);

        return JdbcExecutor.query(
            queryStatement, paramMap, (ResultSet rs) -> {
                ResultSetMetaData rsMetaData = rs.getMetaData();

                int columnCount = rsMetaData.getColumnCount();

                List<QueryColumn> columns = new ArrayList<>();

                for (int i = 1; i <= columnCount; i++) {
                    columns.add(new QueryColumn(rsMetaData.getColumnName(i), rsMetaData.getColumnType(i)));
                }

                List<Map<String, Object>> rows = new ArrayList<>();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        QueryColumn column = columns.get(i - 1);

                        row.put(column.name(), rs.getObject(i));
                    }

                    rows.add(row);
                }

                return new QueryResult(columns, rows);
            }, dataSource);
    }

    @Override
    public List<Map<String, Object>> execute(Map<String, ?> inputParameters, DataSource dataSource) {
        Map<String, ?> paramMap = MapUtils.getMap(inputParameters, JdbcConstants.PARAMETERS, Map.of());
        String queryStatement = MapUtils.getRequiredString(inputParameters, JdbcConstants.QUERY);

        return JdbcExecutor.query(
            queryStatement, paramMap, (ResultSet rs, int rowNum) -> {
                Map<String, Object> row = new HashMap<>();

                ResultSetMetaData rsMetaData = rs.getMetaData();

                int columnCount = rsMetaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsMetaData.getColumnName(i);

                    row.put(columnName, rs.getObject(i));
                }

                return row;
            }, dataSource);
    }
}
