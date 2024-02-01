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
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.object.helper.constant.ObjectHelperConstants;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperParseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ObjectHelperConstants.PARSE)
        .title("Convert from JSON string")
        .description("Converts the JSON string to object/array.")
        .properties(string(ObjectHelperConstants.SOURCE)
            .label("Source")
            .description("The JSON string to convert to the data.")
            .required(true))
        .output()
        .perform(ObjectHelperParseAction::perform);

    protected static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object input = inputParameters.getRequired(ObjectHelperConstants.SOURCE);

        return Map.of("result", context.json(json -> json.read((String) input)));
    }
}
