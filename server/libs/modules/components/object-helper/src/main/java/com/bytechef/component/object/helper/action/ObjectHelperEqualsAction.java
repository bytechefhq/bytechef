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
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TARGET;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class ObjectHelperEqualsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("equals")
        .title("Equals")
        .description("Compares two objects and returns true if they are equal.")
        .properties(
            object(SOURCE)
                .label("Source")
                .description("The source object to compare.")
                .required(true),
            object(TARGET)
                .label("Target")
                .description("The target object to compare against.")
                .required(true))
        .output(outputSchema(bool().description("Indicates whether the two objects are equal.")))
        .help("", "https://docs.bytechef.io/reference/components/object-helper_v1#equals")
        .perform(ObjectHelperEqualsAction::perform);

    private ObjectHelperEqualsAction() {
    }

    protected static Boolean perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Object input = inputParameters.getRequired(SOURCE);
        Object target = inputParameters.getRequired(TARGET);

        return input.equals(target);
    }
}
