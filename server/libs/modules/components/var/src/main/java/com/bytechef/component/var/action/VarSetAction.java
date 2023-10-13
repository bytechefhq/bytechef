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

package com.bytechef.component.var.action;

import static com.bytechef.component.var.constant.VarConstants.SET;
import static com.bytechef.component.var.constant.VarConstants.TYPE;
import static com.bytechef.component.var.constant.VarConstants.VALUE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.any;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;

/**
 * @author Ivica Cardic
 */
public class VarSetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SET)
        .title("Set value")
        .description("Assign value to a variable that can be used in the following steps.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Array", 1),
                    option("Boolean", 2),
                    option("Date", 3),
                    option("Date Time", 4),
                    option("Integer", 5),
                    option("Nullable", 6),
                    option("Number", 7),
                    option("Object", 8),
                    option("String", 9),
                    option("Time", 10)),
            array(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 8")
                .required(true),
            string(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("Value of any type to set.")
                .displayCondition("type === 10")
                .required(true))
        .outputSchema(any())
        .perform(VarSetAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        return inputParameters.getRequired(VALUE);
    }
}
