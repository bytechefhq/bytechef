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

package com.bytechef.component.datamapper.action;

import static com.bytechef.component.datamapper.constant.DataMapperConstants.DEFAULT_VALUE;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.VALUE;
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
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaResponse;
import com.bytechef.hermes.component.definition.ParameterMap;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapOneValueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapOneValue")
        .title("Map one value")
        .description(
            "The action maps a given value by matching it with the defined mappings, and it returns the outcome of the mapping. In case there is no mapping specified for the value, it returns the default value, and if there is no default defined, it returns null.")
        .properties(
            integer(TYPE)
                .label("Value Type")
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
                .description("The value you want to map.")
                .displayCondition("type === 1")
                .required(true),
            bool(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 2")
                .required(true),
            date(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 3")
                .required(true),
            dateTime(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 4")
                .required(true),
            integer(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 5")
                .required(true),
            nullable(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 6")
                .required(true),
            number(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 7")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 8")
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                .required(true),
            string(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 9")
                .required(true),
            time(VALUE)
                .label("Value")
                .description("The value you want to map.")
                .displayCondition("type === 10")
                .required(true),
            array(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 1")
                .required(true),
            bool(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 2")
                .required(true),
            date(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 3")
                .required(true),
            dateTime(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 4")
                .required(true),
            integer(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 5")
                .required(true),
            nullable(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 6")
                .required(true),
            number(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 7")
                .required(true),
            object(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 8")
                .required(true),
            string(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 9")
                .required(true),
            time(DEFAULT_VALUE)
                .label("Default value")
                .description("If there is no existing mapping, assign this value a default mapping.")
                .displayCondition("type === 10")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "The collection of \"mappings\" associated with a certain value. When the specified value matches the \"From\" value, the connector will provide the corresponding \"To\" value.")
                .items(
                    object().properties(
                        array(FROM)
                            .label("From")
                            .displayCondition("type === 1")
                            .required(true),
                        bool(FROM)
                            .label("From")
                            .displayCondition("type === 2")
                            .required(true),
                        date(FROM)
                            .label("From")
                            .displayCondition("type === 3")
                            .required(true),
                        dateTime(FROM)
                            .label("From")
                            .displayCondition("type === 4")
                            .required(true),
                        integer(FROM)
                            .label("From")
                            .displayCondition("type === 5")
                            .required(true),
                        nullable(FROM)
                            .label("From")
                            .displayCondition("type === 6")
                            .required(true),
                        number(FROM)
                            .label("From")
                            .displayCondition("type === 7")
                            .required(true),
                        object(FROM)
                            .label("From")
                            .displayCondition("type === 8")
                            .required(true),
                        string(FROM)
                            .label("From")
                            .displayCondition("type === 9")
                            .required(true),
                        time(FROM)
                            .label("From")
                            .displayCondition("type === 10")
                            .required(true),
                        array(TO)
                            .label("To")
                            .displayCondition("type === 1")
                            .required(true),
                        bool(TO)
                            .label("To")
                            .displayCondition("type === 2")
                            .required(true),
                        date(TO)
                            .label("To")
                            .displayCondition("type === 3")
                            .required(true),
                        dateTime(TO)
                            .label("To")
                            .displayCondition("type === 4")
                            .required(true),
                        integer(TO)
                            .label("To")
                            .displayCondition("type === 5")
                            .required(true),
                        nullable(TO)
                            .label("To")
                            .displayCondition("type === 6")
                            .required(true),
                        number(TO)
                            .label("To")
                            .displayCondition("type === 7")
                            .required(true),
                        object(TO)
                            .label("To")
                            .displayCondition("type === 8")
                            .required(true),
                        string(TO)
                            .label("To")
                            .displayCondition("type === 9")
                            .required(true),
                        time(TO)
                            .label("To")
                            .displayCondition("type === 10")
                            .required(true)))
                .required(true))
        .outputSchema(getOutputSchemaFunction())
        .perform(DataMapperMapOneValueAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        // TODO
        return null;
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters, connectionParameters, context) -> new OutputSchemaResponse(
            switch (inputParameters.getRequiredInteger(TYPE)) {
                case 1 -> array();
                case 2 -> bool();
                case 3 -> date();
                case 4 -> dateTime();
                case 5 -> integer();
                case 6 -> nullable();
                case 7 -> number();
                case 8 -> object();
                case 9 -> string();
                case 10 -> time();
                default -> throw new IllegalArgumentException("Type does not exist");
            });
    }
}
