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

package com.bytechef.component.object.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.KEY;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ObjectHelperContainsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("contains")
        .title("Contains")
        .description("Checks if the given key exists in the given object.")
        .properties(
            object(SOURCE)
                .label("Source")
                .description("Object that you'd like to check.")
                .required(true),
            string(KEY)
                .label("Key")
                .description("Key to check for existence.")
                .required(true))
        .output(outputSchema(bool().description("Indicator of whether the key exists in the object.")))
        .help("", "https://docs.bytechef.io/reference/components/object-helper#contains")
        .perform(ObjectHelperContainsAction::perform);

    private ObjectHelperContainsAction() {
    }

    protected static Boolean perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> modifiedObject = inputParameters.getRequiredMap(SOURCE, Object.class);

        return modifiedObject.containsKey(inputParameters.getRequiredString(KEY));
    }
}
