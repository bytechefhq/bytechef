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

package com.bytechef.component.snowflake.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.COLUMN;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.CONDITION;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE_PROPERTY;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA_PROPERTY;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SQL_STATEMENT_RESPONSE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE_PROPERTY;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.VALUES;
import static com.bytechef.component.snowflake.util.SnowflakeUtils.getColumnUpdateStatement;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.snowflake.util.SnowflakeUtils;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeUpdateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRow")
        .title("Update Row")
        .description("Update row from the table.")
        .properties(
            DATABASE_PROPERTY,
            SCHEMA_PROPERTY,
            TABLE_PROPERTY,
            string(COLUMN)
                .label("Column")
                .description("Column name that will be checked for condition.")
                .options((ActionOptionsFunction<String>) SnowflakeUtils::getColumnOptions)
                .required(true),
            string(CONDITION)
                .label("Condition")
                .description("Condition that will be checked in the column.")
                .required(true),
            string(VALUES)
                .label("Values")
                .description("Updated values of the table. Seperated by comma.")
                .required(true))
        .output(outputSchema(SQL_STATEMENT_RESPONSE))
        .perform(SnowflakeUpdateRowAction::perform);

    private SnowflakeUpdateRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String values = inputParameters.getRequiredString(VALUES);

        String sqlStatement = "UPDATE %s.%s.%s SET %s WHERE %s = %s".formatted(
            inputParameters.getRequiredString(DATABASE),
            inputParameters.getRequiredString(SCHEMA),
            inputParameters.getRequiredString(TABLE),
            getColumnUpdateStatement(SnowflakeUtils.getTableColumns(inputParameters, context), values),
            inputParameters.getRequiredString(COLUMN),
            inputParameters.getRequiredString(CONDITION));

        return SnowflakeUtils.executeStatement(context, sqlStatement);
    }
}
