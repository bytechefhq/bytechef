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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.KEY;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TYPE_OPTIONS;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.VALUE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.VALUE_TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;

/**
 * @author J. Iamsamang
 */
public class ObjectHelperAddValueByKeyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addValueByKey")
        .title("Add value to the object by key")
        .description("Add value to the object by key if it exists. Otherwise, update the value")
        .properties(
            object(SOURCE)
                .label("Source")
                .description("Source object to be added or updated")
                .required(true),
            string(KEY)
                .label("Key")
                .description("Key of the value to be added or updated.")
                .required(true),
            integer(VALUE_TYPE)
                .label("Value type")
                .options(TYPE_OPTIONS)
                .description("Type of value to be added or updated.")
                .required(true),
            array(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 8")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("Value to be added or updated.")
                .displayCondition("valueType == 10")
                .required(true))
        .output()
        .perform(ObjectHelperAddValueByKeyAction::perform);

    private ObjectHelperAddValueByKeyAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> modifiedObject =
            new HashMap<>(Map.copyOf(inputParameters.getRequiredMap(SOURCE, Object.class)));

        String targetKey = inputParameters.getRequiredString(KEY);
        Object value = inputParameters.getRequired(VALUE);

        modifiedObject.put(targetKey, value);

        return modifiedObject;
    }
}
