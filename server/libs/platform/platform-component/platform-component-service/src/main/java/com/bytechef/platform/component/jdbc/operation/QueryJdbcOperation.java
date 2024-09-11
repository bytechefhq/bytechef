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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * @author Ivica Cardic
 */
public class QueryJdbcOperation implements JdbcOperation<List<Map<String, Object>>> {

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
