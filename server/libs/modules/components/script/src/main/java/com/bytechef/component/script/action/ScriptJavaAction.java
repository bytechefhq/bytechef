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

package com.bytechef.component.script.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;
import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.component.script.constant.ScriptConstants.JAVA;
import static com.bytechef.component.script.constant.ScriptConstants.SCRIPT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OutputSchemaDataSource.ActionOutputSchemaFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.SampleOutputDataSource.ActionSampleOutputFunction;
import com.bytechef.component.definition.SampleOutputDataSource.SampleOutputResponse;
import com.bytechef.component.script.constant.ScriptConstants;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptJavaAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(JAVA)
        .title("Java")
        .description("Executes custom Java code.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("Initialize parameter values used in the custom code.")
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()),
            string(SCRIPT)
                .label("Java code")
                .description("Add your Java custom logic here.")
                .controlType(Property.ControlType.CODE_EDITOR)
                .required(true))
        .outputSchema(getOutputSchemaFunction())
        .sampleOutput(getSampleOutputFunction())
        .perform(ScriptJavaAction::perform);

    protected static ActionOutputSchemaFunction getOutputSchemaFunction() {
        return (inputParameters, connectionParameters, context) -> context.outputSchema(
            outputSchema -> outputSchema.get(perform(inputParameters, connectionParameters, context)));
    }

    protected static ActionSampleOutputFunction getSampleOutputFunction() {
        return (inputParameters, connectionParameters, context) -> new SampleOutputResponse(
            perform(inputParameters, connectionParameters, context));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return ScriptConstants.POLYGLOT_ENGINE.execute("java", inputParameters);
    }
}
