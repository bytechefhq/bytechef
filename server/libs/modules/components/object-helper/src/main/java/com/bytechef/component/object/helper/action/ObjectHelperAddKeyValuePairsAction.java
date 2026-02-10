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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.LIST;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TYPE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TYPE_OPTIONS;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.object.helper.constant.ValueType;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author J. Iamsamang
 */
public class ObjectHelperAddKeyValuePairsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addKeyValuePairs")
        .title("Add Key-Value Pairs")
        .description(
            "Add values from list to object. The source object can either be empty or populated with properties. " +
                "The items in the list will be treated as Key-value pairs.")
        .properties(
            object(SOURCE)
                .label("Source")
                .description("Source object to be added or updated")
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), number(), nullable(), object(), string(), time()),
            array(LIST)
                .label("Key-Value Pairs")
                .description("Key-Value pairs to be added or updated.")
                .items(
                    object()
                        .label("Key-Value Pair")
                        .properties(
                            string("key")
                                .label("Key")
                                .description("Key of the value to be added or updated.")
                                .required(true),
                            string(TYPE)
                                .label("Type")
                                .options(TYPE_OPTIONS)
                                .description("Type of value to be added or updated.")
                                .expressionEnabled(false)
                                .required(true),
                            array(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.ARRAY))
                                .required(true),
                            bool(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.BOOLEAN))
                                .required(true),
                            date(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.DATE))
                                .required(true),
                            dateTime(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.DATE_TIME))
                                .required(true),
                            integer(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.INTEGER))
                                .required(true),
                            nullable(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.NULL))
                                .required(true),
                            number(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.NUMBER))
                                .required(true),
                            object(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.OBJECT))
                                .required(true),
                            string(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.STRING))
                                .required(true),
                            time(VALUE)
                                .label("Value")
                                .description("Value to be added or updated.")
                                .displayCondition("%s == '%s'".formatted("list[index].type", ValueType.TIME))
                                .required(true)))
                .required(true))
        .output(ObjectHelperAddKeyValuePairsAction::output)
        .help(
            "",
            "https://docs.bytechef.io/reference/components/object-helper_v1#add-value-to-the-object-by-key")
        .perform(ObjectHelperAddKeyValuePairsAction::perform);

    private ObjectHelperAddKeyValuePairsAction() {
    }

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OutputResponse.of(perform(inputParameters, connectionParameters, context));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        List<KeyValuePair> keyValuePairs = inputParameters.getRequiredList(LIST, KeyValuePair.class);
        Map<String, Object> modifiedObject = inputParameters.getMap(SOURCE, Object.class, Map.of());

        return addKeyValuePairsToObject(new HashMap<>(modifiedObject), keyValuePairs);
    }

    private static Map<String, Object> addKeyValuePairsToObject(
        Map<String, Object> sourceObject, List<KeyValuePair> keyValuePairs) {

        for (KeyValuePair keyValuePair : keyValuePairs) {
            String key = keyValuePair.key();
            Object value = keyValuePair.value();

            sourceObject.put(key, value);
        }

        return sourceObject;
    }

    private record KeyValuePair(String key, Object value) {
    }
}
