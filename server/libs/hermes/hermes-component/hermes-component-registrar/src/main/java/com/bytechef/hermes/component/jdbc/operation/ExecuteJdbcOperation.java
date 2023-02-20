
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.jdbc.JdbcExecutor;
import com.bytechef.hermes.component.jdbc.constant.JdbcConstants;
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
    public Map<String, Integer> execute(Context context, ExecutionParameters executionParameters) {
        String executeStatement = executionParameters.getRequiredString(JdbcConstants.EXECUTE);
        Map<String, ?> paramMap = executionParameters.getMap(JdbcConstants.PARAMETERS, Map.of());

        int rowsAffected = jdbcExecutor.update(context.getConnectionParameters(), executeStatement, paramMap);

        return Map.of("rows", rowsAffected);
    }
}
