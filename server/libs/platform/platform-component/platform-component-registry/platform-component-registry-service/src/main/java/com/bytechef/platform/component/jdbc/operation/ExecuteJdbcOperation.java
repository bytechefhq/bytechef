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
import com.bytechef.platform.component.jdbc.constant.JdbcConstants;
import com.bytechef.platform.component.jdbc.executor.JdbcExecutor;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ExecuteJdbcOperation implements JdbcOperation<Map<String, Integer>> {

    private final JdbcExecutor jdbcExecutor;

    public ExecuteJdbcOperation(JdbcExecutor jdbcExecutor) {
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public Map<String, Integer> execute(Map<String, ?> inputParameters, Map<String, ?> connectionParameters) {
        String executeStatement = MapUtils.getRequiredString(inputParameters, JdbcConstants.EXECUTE);
        Map<String, ?> parameterMap = MapUtils.getMap(inputParameters, JdbcConstants.PARAMETERS, Map.of());

        int rowsAffected = jdbcExecutor.update(connectionParameters, executeStatement, parameterMap);

        return Map.of("rows", rowsAffected);
    }
}
