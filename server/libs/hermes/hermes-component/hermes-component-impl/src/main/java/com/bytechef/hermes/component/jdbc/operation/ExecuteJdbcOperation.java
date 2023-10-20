
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
    public Map<String, Integer> execute(Context context, Map<String, ?> inputParameters) {
        String executeStatement = MapValueUtils.getRequiredString(inputParameters, JdbcConstants.EXECUTE);
        Map<String, ?> paramMap = MapValueUtils.getMap(inputParameters, JdbcConstants.PARAMETERS, Map.of());

        int rowsAffected = jdbcExecutor.update(context.getConnection(), executeStatement,
            paramMap);

        return Map.of("rows", rowsAffected);
    }
}
