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
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.COLUMN;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.CONDITION;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.STATEMENT;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.sqlStatementResponse;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.snowflake.util.SnowflakeUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRow")
        .title("Delete Row")
        .description("Delete row from the table.")
        .properties(
            string(DATABASE)
                .label("Database")
                .options((ActionOptionsFunction<String>) SnowflakeUtils::getDatabaseNameOptions)
                .required(true),
            string(SCHEMA)
                .label("Schema")
                .options((ActionOptionsFunction<String>) SnowflakeUtils::getSchemaNameOptions)
                .optionsLookupDependsOn(DATABASE)
                .required(true),
            string(TABLE)
                .label("Table")
                .options((ActionOptionsFunction<String>) SnowflakeUtils::getTableNameOptions)
                .optionsLookupDependsOn(SCHEMA)
                .required(true),
            string(COLUMN)
                .label("Column")
                .description("Column name that will be checked for condition.")
                .options((ActionOptionsFunction<String>) SnowflakeUtils::getColumnOptions)
                .required(true),
            string(CONDITION)
                .label("Condition")
                .description("Condition that will be checked in the column.")
                .required(true))
        .output(outputSchema(sqlStatementResponse))

        .perform(SnowflakeDeleteRowAction::perform);

    private SnowflakeDeleteRowAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {
        String sqlStatement = "DELETE FROM %s.%s.%s WHERE %s = %s".formatted(
            inputParameters.getRequiredString(DATABASE),
            inputParameters.getRequiredString(SCHEMA),
            inputParameters.getRequiredString(TABLE),
            inputParameters.getRequiredString(COLUMN),
            inputParameters.getRequiredString(CONDITION));

        return context.http(http -> http.post("/api/v2/statements"))
            .body(
                Body.of(
                    Map.of(
                        STATEMENT, sqlStatement)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
