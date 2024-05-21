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

package com.bytechef.component.data.storage.action;

import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.DEFAULT_VALUE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.data.storage.constant.DataStorageConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStorageGetValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getValue")
        .title("Get Value")
        .description("Retrieve a previously assigned value within the specified scope using its corresponding key.")
        .properties(
            string(DataStorageConstants.KEY)
                .label("Key")
                .description("The identifier of a value to get, stored earlier in the selected scope.")
                .required(true),
            string(DataStorageConstants.SCOPE)
                .label("Scope")
                .description(
                    "The namespace to get a value from. The value should have been previously accessible, either in the present workflow execution, or the workflow itself for all the executions, or the user account for all the workflows the user has.")
                .options(DataStorageConstants.SCOPE_OPTIONS)
                .required(true),
            integer(DataStorageConstants.TYPE)
                .label("Type")
                .description("The value type.")
                .options(DataStorageConstants.TYPE_OPTIONS),
            array(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 1")
                .required(true),
            bool(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 2")
                .required(true),
            date(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 3")
                .required(true),
            dateTime(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 4")
                .required(true),
            integer(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 5")
                .required(true),
            nullable(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 6")
                .required(true),
            number(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 7")
                .required(true),
            object(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 8")
                .required(true),
            string(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 9")
                .required(true),
            time(DataStorageConstants.DEFAULT_VALUE)
                .label("Default value")
                .description("The default value to return if no value exists under the given key.")
                .displayCondition("type == 10")
                .required(true))
        .output(
            (inputParameters, connectionParameters, context) -> context
                .output(output -> output.get(inputParameters.getRequired(DEFAULT_VALUE))))
        .perform(DataStorageGetValueAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws ClassNotFoundException {

        Class<?> cls = null;
        switch (inputParameters.getRequiredInteger(TYPE)) {
            case 1 -> cls = ArrayList.class;
            case 2 -> cls = Boolean.class;
            case 3 -> cls = LocalDate.class;
            case 4 -> cls = LocalDateTime.class;
            case 5 -> cls = Integer.class;
            case 6 -> cls = nullable().getClass();
            case 7 -> cls = Number.class;
            case 8 -> cls = Object.class;
            case 9 -> cls = String.class;
            case 10 -> cls = LocalTime.class;
        }

        Optional<Object> optional = context
            .data(data -> data.fetchValue(ActionContext.Data.Scope.valueOf(inputParameters.getRequiredString(SCOPE)),
                inputParameters.getRequiredString(KEY)));

        if (optional.isEmpty()) {
            if (ConvertUtils.canConvert(optional.get(), cls))
                return ConvertUtils.convertValue(inputParameters.getRequiredString(DEFAULT_VALUE), cls);

            return inputParameters.getRequiredString(DEFAULT_VALUE);
        }

        if (ConvertUtils.canConvert(optional.get(), cls))
            return ConvertUtils.convertValue(optional.get(), cls);

        return optional.get();
    }
}
