
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

package com.bytechef.component.script.action;

import com.bytechef.component.script.constant.ScriptConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.definition.Property;

import java.util.Map;

import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.component.script.constant.ScriptConstants.PYTHON;
import static com.bytechef.component.script.constant.ScriptConstants.SCRIPT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptPythonAction {

    public static final ActionDefinition ACTION_DEFINITION = action(PYTHON)
        .title("Python")
        .description("Executes custom Python code.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("Initialize parameter values used in the custom code.")
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()),
            string(SCRIPT)
                .label("Python code")
                .description("Add your Python custom logic here.")
                .controlType(Property.ControlType.CODE_EDITOR)
                .required(true))
        .outputSchema(getOutputSchemaFunction())
        .execute(ScriptPythonAction::executePython);

    protected static Object executePython(Context context, Map<String, ?> inputParameters) {
        return ScriptConstants.POLYGLOT_ENGINE.execute("python", inputParameters);
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
