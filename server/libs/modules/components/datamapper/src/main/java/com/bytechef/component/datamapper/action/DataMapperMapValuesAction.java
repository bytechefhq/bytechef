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

import static com.bytechef.component.datamapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.datamapper.constant.DataMapperConstants.TYPE;
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
public class DataMapperMapValuesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("mapValues")
        .title("Map values")
        .description(
            "When provided with an object or an array of objects, the function maps one or more values to new values. It can be restricted to specific keys of the object. The resulting data structure is a new object or array with the original structure and mapped values.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(INPUT)
                .label("Input")
                .description("The object to map.")
                .displayCondition("type === 1"),
            array(INPUT)
                .label("Input")
                .description("The array of values to map.")
                .displayCondition("type === 2")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "The collection of of \"mappings\" where \"From\" refers to a particular key from the Input, while \"To\" represents the name of a new key that is assigned the corresponding value of the \"From\" key.")
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
        .perform(DataMapperMapValuesAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        // TODO
        return null;
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters, connection, context) -> {
            if (inputParameters.getInteger(TYPE, 1) == 1) {
                return new OutputSchemaResponse(object());
            } else {
                return new OutputSchemaResponse(array());
            }
        };
    }
}
