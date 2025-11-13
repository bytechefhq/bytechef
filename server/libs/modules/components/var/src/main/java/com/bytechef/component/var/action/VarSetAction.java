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

package com.bytechef.component.var.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.var.constant.VarConstants.TYPE;
import static com.bytechef.component.var.constant.VarConstants.VALUE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;

/**
 * @author Ivica Cardic
 */
public class VarSetAction {

    private enum ValueType {

        ARRAY, BOOLEAN, DATE, DATE_TIME, INTEGER, /* NULL, */ NUMBER, OBJECT, STRING, TIME;
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("set")
        .title("Set Value")
        .description("Assign value to a variable that can be used in the following steps.")
        .properties(
            string(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Array", ValueType.ARRAY.name()),
                    option("Boolean", ValueType.BOOLEAN.name()),
                    option("Date", ValueType.DATE.name()),
                    option("Date Time", ValueType.DATE_TIME.name()),
                    option("Integer", ValueType.INTEGER.name()),
//                    option("Nullable", ValueType.NULL.name()),
                    option("Number", ValueType.NUMBER.name()),
                    option("Object", ValueType.OBJECT.name()),
                    option("String", ValueType.STRING.name()),
                    option("Time", ValueType.TIME.name())),
            array(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.ARRAY))
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.BOOLEAN))
                .required(true),
            date(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE))
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.DATE_TIME))
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.INTEGER))
                .required(true),
//            nullable(VALUE)
//                .label("Value")
//                .description("Value of any type to set.")
//                .displayCondition("type == '%s'".formatted(ValueType.NULL))
//                .required(true),
            number(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.NUMBER))
                .required(true),
            object(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .additionalProperties()
                .displayCondition("type == '%s'".formatted(ValueType.OBJECT))
                .required(true),
            string(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.STRING))
                .required(true),
            time(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type == '%s'".formatted(ValueType.TIME))
                .required(true))
        .output(VarSetAction::output)
        .perform(VarSetAction::perform);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        if (!inputParameters.containsKey(VALUE)) {
            return null;
        }

        return OutputResponse.of(perform(inputParameters, connectionParameters, context));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return inputParameters.getRequired(VALUE);
    }
}
