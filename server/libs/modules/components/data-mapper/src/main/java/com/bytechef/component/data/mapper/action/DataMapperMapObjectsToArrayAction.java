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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_KEY;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT_TYPE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE_KEY;
import static com.bytechef.component.data.mapper.constant.InputType.ARRAY;
import static com.bytechef.component.data.mapper.constant.InputType.OBJECT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.data.mapper.constant.InputType;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperMapObjectsToArrayAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("mapObjectsToArray")
        .title("Map Objects to Array")
        .description("Transform an object or array of objects into an array of key-value pairs.")
        .properties(
            string(INPUT_TYPE)
                .label("Input Type")
                .description("Type of the input. Cam be an object or an array of objects.")
                .options(
                    option("Object", OBJECT.name()),
                    option("Array", ARRAY.name()))
                .required(true),
            object(INPUT)
                .label("Input")
                .description("An input object containing one or more properties.")
                .displayCondition("inputType == '%s'".formatted(OBJECT.name()))
                .required(true),
            array(INPUT)
                .label("Input")
                .description("An input array containing one or more objects.")
                .displayCondition("inputType == '%s'".formatted(ARRAY.name()))
                .items(object())
                .required(true),
            string(FIELD_KEY)
                .label("Field Key")
                .description(
                    "Property key of each newly created object in the array. Its property value will be a property key from the input.")
                .required(true),
            string(VALUE_KEY)
                .label("Value Key")
                .description(
                    "Property key of each newly created object in the array. Its property value will be a property value from the input.")
                .required(true))
        .output()
        .perform(DataMapperMapObjectsToArrayAction::perform);

    private DataMapperMapObjectsToArrayAction() {
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> output = new ArrayList<>();
        InputType inputType = inputParameters.get(INPUT_TYPE, InputType.class);

        if (inputType == OBJECT) {
            Map<String, Object> input = inputParameters.getMap(INPUT, Object.class, new HashMap<>());

            fillOutput(inputParameters, input, output);
        } else {
            List<Object> input = inputParameters.getList(INPUT, Object.class, List.of());

            for (Object object : input) {
                fillOutput(inputParameters, (Map<String, Object>) object, output);
            }
        }

        return output;
    }

    private static void fillOutput(
        Parameters inputParameters, Map<String, Object> input, List<Map<String, Object>> output) {

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
