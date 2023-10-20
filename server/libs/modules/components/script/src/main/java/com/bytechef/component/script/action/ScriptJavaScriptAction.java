
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
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.definition.Property;

import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.component.script.constant.ScriptConstants.JAVASCRIPT;
import static com.bytechef.component.script.constant.ScriptConstants.SCRIPT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptJavaScriptAction {

    public static final ActionDefinition ACTION_DEFINITION = action(JAVASCRIPT)
        .display(display("JavaScript").description("Executes custom JavaScript code."))
        .properties(
            object(INPUT)
                .label("Input")
                .description("Initialize parameter values used in the custom code.")
                .additionalProperties(oneOf()),
            string(SCRIPT)
                .label("JavaScript code")
                .description("Add your JavaScript custom logic here.")
                .controlType(Property.ControlType.CODE_EDITOR))
        .outputSchema(oneOf())
        .perform(ScriptJavaScriptAction::performJavaScript);

    public static Object performJavaScript(Context context, Parameters parameters) {
        return ScriptConstants.POLYGLOT_ENGINE.execute("js", parameters);
    }
}
