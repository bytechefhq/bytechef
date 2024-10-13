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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.ADD_KEY_VALUE_PAIRS;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE_TYPE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author J. Iamsamang
 */
public class ObjectHelperAddKeyValuePairsAction {

    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action(ADD_KEY_VALUE_PAIRS)
        .title("Add Key-Value pairs to object or array")
        .description("Add values from list to object or array. If the source is object, the items in the list will " +
            "be treated as Key-value pairs. If the value is array of objects, key-value pairs will be added to every object in the array.")
        .properties(
            integer(SOURCE_TYPE)
                .label("Type of initial object")
                .options(
                    option("Array", 1),
                    option("Object", 2))
                .description("Type of initial object to be added or updated.")
                .required(true),
            array(SOURCE)
                .label("Source")
                .description("Source object to be added or updated")
                .displayCondition("sourceType == 1")
                .items(object())
                .required(true),
            object(SOURCE)
                .label("Source")
                .description("Source object to be added or updated")
                .displayCondition("sourceType == 2")
                .required(true),
            array(VALUE)
                .label("Array of Key-Value pairs")
                .description("Array of Key-Value pairs to be added or updated.")
                .items(
                    array()
                        .label("Key-Value pair")
                        .maxItems(2)
                        .minItems(2))
                .required(true))
        .perform(ObjectHelperAddKeyValuePairsAction::perform)
        .output();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected static Object
        perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        int sourceType = inputParameters.getRequired(SOURCE_TYPE, int.class);
        // Case 1: the initial object is an array
        // add all items to the initial array
        if (sourceType == 1) {
            List<Object> modifiedArray = new ArrayList<>(Arrays.asList(inputParameters.getArray(SOURCE)));
            List<Object> resultArray = new ArrayList<Object>();
            Object[] valueArray = inputParameters.getArray(VALUE);
            for (Object sourceObject : modifiedArray) {
                var sourceMap = objectMapper.convertValue(sourceObject, new TypeReference<Map<String, Object>>() {});
                resultArray.add(addKeyValuePairsToObject(sourceMap, valueArray));
            }
            return resultArray;
        }
        // Case 2: the initial object is a map
        // add all key-value pairs in the list to the initial map
        else {
            Map<String, Object> modifiedObject =
                new HashMap<>(Map.copyOf(inputParameters.getRequiredMap(SOURCE, Object.class)));

            Object[] keyValuePairs = inputParameters.getArray(VALUE);
            return addKeyValuePairsToObject(modifiedObject, keyValuePairs);
        }
    }

    private static Object addKeyValuePairsToObject(Map<String, Object> sourceObject, Object[] keyValuePairs) {
        for (Object objectPair : keyValuePairs) {
            JsonNode node = objectMapper.convertValue(objectPair, JsonNode.class);
            // If an item in array is a pair of key and value.
            // Therefore, size of the item must be two and the first value must be string
            if (node.isArray() && node.size() == 2 && node.get(0)
                .isTextual()) {
                String key = node.get(0)
                    .asText();
                Object val = objectMapper.convertValue(node.get(1), Object.class);
                sourceObject.put(key, val);
            }
        }
        return sourceObject;
    }
}
