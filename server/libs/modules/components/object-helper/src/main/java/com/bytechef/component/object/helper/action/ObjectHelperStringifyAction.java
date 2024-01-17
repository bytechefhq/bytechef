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

package com.bytechef.component.object.helper.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.STRINGIFY;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperStringifyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(STRINGIFY)
        .title("Convert to JSON string")
        .description("Writes the object/array to a JSON string.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(SOURCE)
                .label("Source")
                .description("The data to convert to JSON string.")
                .displayCondition("type === 1")
                .required(true),
            array(SOURCE)
                .label("Source")
                .description("The data to convert to JSON string.")
                .displayCondition("type === 2")
                .required(true))
        .outputSchema()
        .perform(ObjectHelperStringifyAction::perform);

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Object input = inputParameters.getRequired(SOURCE);

        return context.json(json -> json.write(input));
    }
}
