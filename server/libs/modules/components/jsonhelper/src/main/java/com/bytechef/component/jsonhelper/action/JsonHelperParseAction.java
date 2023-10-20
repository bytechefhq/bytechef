
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

package com.bytechef.component.jsonhelper.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.utils.JsonUtils;

import static com.bytechef.component.jsonhelper.constant.JsonHelperConstants.PARSE;
import static com.bytechef.component.jsonhelper.constant.JsonHelperConstants.SOURCE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class JsonHelperParseAction {

    public static final ActionDefinition ACTION_DEFINITION = action(PARSE)
        .display(display("Convert from JSON string")
            .description("Converts the JSON string to object/array."))
        .properties(string(SOURCE)
            .label("Source")
            .description("The JSON string to convert to the data.")
            .required(true))
        .output(oneOf())
        .perform(JsonHelperParseAction::performParse);

    public static Object performParse(Context context, ExecutionParameters executionParameters) {
        Object input = executionParameters.getRequired(SOURCE);

        return JsonUtils.read((String) input);
    }
}
