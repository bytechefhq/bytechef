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

package com.bytechef.component.json.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.json.helper.constant.JsonHelperConstants.SOURCE;
import static com.bytechef.component.json.helper.constant.JsonHelperConstants.TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class JsonHelperStringifyAction {

    private enum ValueType {

        OBJECT, ARRAY;
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("stringify")
        .title("Convert to JSON String")
        .description("Writes the object/array to a JSON string.")
        .properties(
            string(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", ValueType.OBJECT.name()),
                    option("Array", ValueType.ARRAY.name())),
            object(SOURCE)
                .label("Source")
                .description("The data to convert to JSON string.")
                .displayCondition("type == '%s'".formatted(ValueType.OBJECT.name()))
                .required(true),
            array(SOURCE)
                .label("Source")
                .description("The data to convert to JSON string.")
                .displayCondition("type == '%s'".formatted(ValueType.ARRAY.name()))
                .required(true))
        .output()
        .perform(JsonHelperStringifyAction::perform);

    private JsonHelperStringifyAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object input = inputParameters.getRequired(SOURCE);

        return context.json(json -> json.write(input));
    }
}
