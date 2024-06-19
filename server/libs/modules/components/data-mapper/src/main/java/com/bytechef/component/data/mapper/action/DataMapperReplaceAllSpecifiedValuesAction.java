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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataMapperReplaceAllSpecifiedValuesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("replaceAllSpecifiedValues")
        .title("Replace all specified values")
        .description(
            "Goes through all object parameters and replaces all specified input parameter values.")
        .properties(
            integer(TYPE)
                .label("Type")
                .description("The input type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(INPUT)
                .label("Input")
                .description("An object containing one or more properties.")
                .displayCondition("type == 1"),
            array(INPUT)
                .label("Input")
                .description("An array containing one or more objects.")
                .displayCondition("type == 2")
                .items(object())
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "Object that contains properties 'from' and 'to'.")
                .items(
                    object().properties(
                        object(FROM)
                            .label("From")
                            .description("Defines the property value you want to change.")
                            .required(true),
                        object(TO)
                            .label("From")
                            .description("Defines what you want to change the property value to")
                            .required(true)))
                .required(true))
        .output()
        .perform(DataMapperReplaceAllSpecifiedValuesAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO
        return null;
    }
}
