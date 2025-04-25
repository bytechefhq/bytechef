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

package com.bytechef.component.snowflake.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE_PROPERTY;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA_PROPERTY;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SQL_STATEMENT_RESPONSE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE_PROPERTY;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.VALUES;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.VALUES_DYNAMIC_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.snowflake.util.SnowflakeUtils;
import java.util.stream.Collectors;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeInsertRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("insertRow")
        .title("Insert Row")
        .description("Insert row into the table.")
        .properties(
            DATABASE_PROPERTY,
            SCHEMA_PROPERTY,
            TABLE_PROPERTY,
            VALUES_DYNAMIC_PROPERTY)
        .output(outputSchema(SQL_STATEMENT_RESPONSE))
        .perform(SnowflakeInsertRowAction::perform);

    private SnowflakeInsertRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String columns = inputParameters.getRequiredMap(VALUES)
            .keySet()
            .stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));

        String values = inputParameters.getRequiredMap(VALUES)
            .values()
            .stream()
            .map(value -> value instanceof String ? "'" + value + "'" : value.toString())
            .collect(Collectors.joining(","));

        String sqlStatement = "INSERT INTO %s.%s.%s(%s) VALUES(%s)".formatted(
            inputParameters.getRequiredString(DATABASE),
            inputParameters.getRequiredString(SCHEMA),
            inputParameters.getRequiredString(TABLE),
            columns,
            values);

        return SnowflakeUtils.executeStatement(context, sqlStatement);
    }
}
