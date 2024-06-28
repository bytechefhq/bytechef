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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_KEY;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE_KEY;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapObjectsToArrayAction {

    private DataMapperMapObjectsToArrayAction() {
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapObjectsToArray")
        .title("Map objects to array")
        .description("Transform an object or array of objects into an array of key-value pairs.")
        .properties(
            integer(INPUT_TYPE)
                .label("Input type")
                .description("Type of the input. Cam be an object or an array of objects.")
                .options(
                    option("Object", 1),
                    option("Array", 2))
                .required(true),
            object(INPUT)
                .label("Input")
                .description("An input object containing one or more properties.")
                .displayCondition("inputType == 1")
                .required(true),
            array(INPUT)
                .label("Input")
                .description("An input array containing one or more objects.")
                .displayCondition("inputType == 2")
                .items(object())
                .required(true),
            string(FIELD_KEY)
                .label("Field key")
                .description(
                    "Property key of each newly created object in the array. Its property value will be a property key from the input.")
                .required(true),
            string(VALUE_KEY)
                .label("Value key")
                .description(
                    "Property key of each newly created object in the array. Its property value will be a property value from the input.")
                .required(true))
        .output()
        .perform(DataMapperMapObjectsToArrayAction::perform);

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        List<Map<String, Object>> output = new ArrayList<>();

        if (inputParameters.getInteger(INPUT_TYPE).equals(1)) {
            Map<String, Object> input = inputParameters.getMap(INPUT, Object.class, new HashMap<>());

            fillOutput(inputParameters, input, output);
        } else {
            List<Object> input = inputParameters.getList(INPUT, Object.class, List.of());

            for (Object object : input) {
                fillOutput(inputParameters, ((Map<String, Object>) object), output);
            }
        }

        return output;
    }

    private static void
        fillOutput(Parameters inputParameters, Map<String, Object> input, List<Map<String, Object>> output) {
        String fieldKey = inputParameters.getRequiredString(FIELD_KEY);
        String valueKey = inputParameters.getRequiredString(VALUE_KEY);

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Map<String, Object> objectHashMap = new HashMap<>();
            objectHashMap.put(fieldKey, entry.getKey());
            objectHashMap.put(valueKey, entry.getValue());
            output.add(objectHashMap);
        }
    }
}
