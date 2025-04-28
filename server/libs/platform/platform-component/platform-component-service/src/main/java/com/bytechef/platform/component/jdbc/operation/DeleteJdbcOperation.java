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

import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.CONDITION;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.ROWS;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.SCHEMA;
import static com.bytechef.platform.component.jdbc.constant.JdbcConstants.TABLE;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.jdbc.JdbcExecutor;
import java.util.Map;
import javax.sql.DataSource;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class DeleteJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    @Override
    public Map<String, Integer> execute(Map<String, ?> inputParameters, DataSource dataSource) {
        String schema = MapUtils.getString(inputParameters, SCHEMA, "public");
        String table = MapUtils.getRequiredString(inputParameters, TABLE);
        String condition = MapUtils.getRequiredString(inputParameters, CONDITION);

        int rowsAffected = JdbcExecutor.update(
            "DELETE FROM %s.%s WHERE %s".formatted(schema, table, condition), Map.of(), dataSource);

        return Map.of(ROWS, rowsAffected);
    }
}
