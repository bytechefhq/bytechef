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
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_VALUE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperMergeAndPivotByKeyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("mergeAndPivotPropertiesByKey")
        .title("Merge and Pivot Properties by Key")
        .description(
            "Creates a new object out of all objects that have the same key as the specified field kay and an object " +
                "as value. That value of the new object contains values of all properties that share the specified " +
                "field key as keys and the they all have the specified field value as a value.")
        .properties(
            array(INPUT)
                .label("Input")
                .description("An array that contains objects with key-value properties that need do be merged.")
                .items(object())
                .required(true),
            string(FIELD_KEY)
                .label("Field Key")
                .description("The key of the newly created object.")
                .required(true),
            string(FIELD_VALUE)
                .label("Field Value")
                .description("The value of each property in the newly created objects value.")
                .required(true))
        .output()
        .perform(DataMapperMergeAndPivotByKeyAction::perform);

    private DataMapperMergeAndPivotByKeyAction() {
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Map<Object, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Object> input = inputParameters.getList(INPUT, Object.class, List.of());
        String fieldKey = inputParameters.getRequiredString(FIELD_KEY);
        String fieldValue = inputParameters.getRequiredString(FIELD_VALUE);

        Map<Object, Object> valueMap = new HashMap<>();

        for (Object object : input) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) object).entrySet()) {
                String key = entry.getKey();

                if (key.equals(fieldKey)) {
                    valueMap.put(entry.getValue(), fieldValue);
                }
            }
        }

        Map<String, Map<Object, Object>> output = new HashMap<>();

        output.put(fieldKey, valueMap);

        return output;
    }
}
