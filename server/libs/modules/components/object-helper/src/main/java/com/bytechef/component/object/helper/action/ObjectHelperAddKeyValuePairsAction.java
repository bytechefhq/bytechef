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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE_TYPE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author J. Iamsamang
 */
public class ObjectHelperAddKeyValuePairsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addKeyValuePairs")
        .title("Add Key-Value pairs to object or array")
        .description(
            "Add values from list to object or array. If the source is object, the items in the list will be " +
                "treated as Key-value pairs. If the value is array of objects, key-value pairs will be added to " +
                "every object in the array.")
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
                .items(
                    object()
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), number(), nullable(), object(), string(),
                            time()))
                .required(true),
            object(SOURCE)
                .label("Source")
                .description("Source object to be added or updated")
                .displayCondition("sourceType == 2")
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), number(), nullable(), object(), string(), time())
                .required(true),
            object(VALUE)
                .label("Array of Key-Value pairs")
                .description("Array of Key-Value pairs to be added or updated.")
                .additionalProperties(bool(), string(), number(), object(), array(), dateTime(), date(), time())
                .required(true))
        .perform(ObjectHelperAddKeyValuePairsAction::perform)
        .output(getOutputSchema());

    private static ActionDefinition.SingleConnectionOutputFunction getOutputSchema() {
        return (inputParameters, connectionParameters, context) -> {
            int sourceType = inputParameters.getRequiredInteger(SOURCE_TYPE);

            if (sourceType == 1) {
                return new OutputResponse(
                    array()
                        .items(
                            object()
                                .additionalProperties(
                                    array(), bool(), date(), dateTime(), integer(), number(), nullable(), object(),
                                    string(), time())));
            } else {
                return new OutputResponse(
                    object()
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), number(), nullable(), object(), string(),
                            time()));
            }
        };
    }

    private ObjectHelperAddKeyValuePairsAction() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Integer sourceType = inputParameters.getRequiredInteger(SOURCE_TYPE);
        Map<String, Object> keyValuePairs = inputParameters.getRequiredMap(VALUE, Object.class);

        if (sourceType.equals(1)) {
            List<Object> modifiedArray = inputParameters.getRequiredList(SOURCE, Object.class);

            for (Object sourceObject : modifiedArray) {
                var sourceMap = objectMapper.convertValue(sourceObject, new TypeReference<Map<String, Object>>() {});

                return addKeyValuePairsToObject(sourceMap, keyValuePairs);
            }

        } else {
            Map<String, Object> modifiedObject = inputParameters.getRequiredMap(SOURCE, Object.class);

            return addKeyValuePairsToObject(new HashMap<>(modifiedObject), keyValuePairs);
        }

        return null;
    }

    private static Object addKeyValuePairsToObject(
        Map<String, Object> sourceObject, Map<String, Object> keyValuePairs) {

        sourceObject.putAll(keyValuePairs);

        return sourceObject;
    }
}
