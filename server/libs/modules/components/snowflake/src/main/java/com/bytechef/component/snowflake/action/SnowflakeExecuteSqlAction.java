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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SQL_STATEMENT_RESPONSE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.STATEMENT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.snowflake.util.SnowflakeUtils;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeExecuteSqlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("executeSql")
        .title("Execute SQL")
        .description("Execute SQL statement.")
        .properties(
            string(STATEMENT)
                .label("Statement")
                .description("SQL statement that will be executed.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(outputSchema(SQL_STATEMENT_RESPONSE))
        .perform(SnowflakeExecuteSqlAction::perform);

    private SnowflakeExecuteSqlAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return SnowflakeUtils.executeStatement(context, inputParameters.getRequiredString(STATEMENT));
    }
}
