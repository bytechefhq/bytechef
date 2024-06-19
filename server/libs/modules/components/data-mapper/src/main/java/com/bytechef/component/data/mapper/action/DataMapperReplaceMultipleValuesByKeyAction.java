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
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.OUTPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataMapperReplaceMultipleValuesByKeyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("replaceMultipleValuesByKey")
        .title("Replace multiple values by key")
        .description(
            "Replaces all values specified by the keys in the input object with the values specified by keys in the output object.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("An object containing one or more properties.")
                .required(true),
            object(OUTPUT)
                .label("Output")
                .description("An object containing one or more properties.")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "Object that contains properties 'from' and 'to'.")
                .items(
                    object()
                        .properties(
                            object(FROM)
                                .label("From")
                                .description("Defines the input property key of the value you want to change."),
                            object(TO)
                                .label("To")
                                .description(
                                    "Defines the output property key of the value you want to change the input value to.")))
                .required(true))
        .output()
        .perform(DataMapperReplaceMultipleValuesByKeyAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO
        return null;
    }
}
