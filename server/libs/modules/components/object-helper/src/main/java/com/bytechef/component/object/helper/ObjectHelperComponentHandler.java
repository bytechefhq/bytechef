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

package com.bytechef.component.object.helper;

import static com.bytechef.component.object.helper.constants.ObjectHelperConstants.JSON_PARSE;
import static com.bytechef.component.object.helper.constants.ObjectHelperConstants.JSON_STRINGIFY;
import static com.bytechef.component.object.helper.constants.ObjectHelperConstants.OBJECT_HELPER;
import static com.bytechef.component.object.helper.constants.ObjectHelperConstants.SOURCE;
import static com.bytechef.hermes.component.ComponentDSL.action;
import static com.bytechef.hermes.component.ComponentDSL.any;
import static com.bytechef.hermes.component.ComponentDSL.createComponent;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.string;

import com.bytechef.commons.json.JsonUtils;
import com.bytechef.hermes.component.ComponentDSL;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * @author Ivica Cardic
 */
public class ObjectHelperComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = createComponent(OBJECT_HELPER)
            .display(display("Object Helper")
                    .description("The Object Helper allows you to do various operations on objects."))
            .actions(
                    action(JSON_PARSE)
                            .display(display("Convert from JSON string")
                                    .description("Converts the JSON string to object/array."))
                            .inputs(string(SOURCE)
                                    .label("Source")
                                    .description("The JSON string to convert to the data.")
                                    .required(true))
                            .outputSchema(
                                    ComponentDSL.array(),
                                    ComponentDSL.bool(),
                                    ComponentDSL.number(),
                                    ComponentDSL.object())
                            .performFunction(this::performParse),
                    action(JSON_STRINGIFY)
                            .display(display("Convert to JSON string")
                                    .description("Writes the object/array to a JSON string."))
                            .inputs(any(SOURCE)
                                    .label("Source")
                                    .description("The data to convert to JSON string.")
                                    .types(
                                            ComponentDSL.array(),
                                            ComponentDSL.bool(),
                                            ComponentDSL.number(),
                                            ComponentDSL.object(),
                                            ComponentDSL.string())
                                    .required(true))
                            .outputSchema(ComponentDSL.string())
                            .performFunction(this::performStringify));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performParse(Context context, ExecutionParameters executionParameters) {
        Object input = executionParameters.getRequiredObject("input");

        return JsonUtils.read((String) input);
    }

    protected String performStringify(Context context, ExecutionParameters executionParameters) {
        Object input = executionParameters.getRequiredObject("input");

        return JsonUtils.write(input);
    }
}
