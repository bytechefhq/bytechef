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

package com.bytechef.component.json.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.json.helper.constant.JsonHelperConstants.SOURCE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class JsonHelperParseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("parse")
        .title("Convert from JSON String")
        .description("Converts the JSON string to object/array.")
        .properties(
            string(SOURCE)
                .label("Source")
                .description("The JSON string to convert to the data.")
                .required(true))
        .output()
        .perform(JsonHelperParseAction::perform);

    private JsonHelperParseAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object input = inputParameters.getRequired(SOURCE);

        return context.json(json -> json.read((String) input));
    }
}
