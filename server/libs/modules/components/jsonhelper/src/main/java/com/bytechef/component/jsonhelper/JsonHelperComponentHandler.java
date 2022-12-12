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

package com.bytechef.component.jsonhelper;

import static com.bytechef.component.jsonhelper.constants.JsonHelperConstants.JSON_HELPER;
import static com.bytechef.component.jsonhelper.constants.JsonHelperConstants.JSON_PARSE;
import static com.bytechef.component.jsonhelper.constants.JsonHelperConstants.JSON_STRINGIFY;
import static com.bytechef.component.jsonhelper.constants.JsonHelperConstants.SOURCE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.utils.JsonUtils;

/**
 * @author Ivica Cardic
 */
public class JsonHelperComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(JSON_HELPER)
            .display(display("JSON Helper").description("JSON Helper allows you to do various operations on objects."))
            .actions(
                    action(JSON_PARSE)
                            .display(display("Convert from JSON string")
                                    .description("Converts the JSON string to object/array."))
                            .properties(string(SOURCE)
                                    .label("Source")
                                    .description("The JSON string to convert to the data.")
                                    .required(true))
                            .output(oneOf())
                            .perform(this::performParse),
                    action(JSON_STRINGIFY)
                            .display(display("Convert to JSON string")
                                    .description("Writes the object/array to a JSON string."))
                            .properties(oneOf(SOURCE)
                                    .label("Source")
                                    .description("The data to convert to JSON string.")
                                    .required(true))
                            .output(string())
                            .perform(this::performStringify));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performParse(Context context, ExecutionParameters executionParameters) {
        Object input = executionParameters.getRequired(SOURCE);

        return JsonUtils.read((String) input);
    }

    protected String performStringify(Context context, ExecutionParameters executionParameters) {
        Object input = executionParameters.getRequired(SOURCE);

        return JsonUtils.write(input);
    }
}
