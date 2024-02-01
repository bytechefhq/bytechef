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
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.VALUE;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DataMapperMapMultipleValuesBetweenObjectsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL
        .action("mapMultipleValuesBetweenObjects")
        .title("Map multiple values between objects\n")
        .description(
            "Transform object properties by assigning new values and generate a new object with updated properties.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("Object containing one or more properties.")
                .required(true),
            object(VALUE)
                .label("Value")
                .description("Object containing one or more properties.")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "The collection of of \"mappings\" where \"From\" corresponds to an existing key in the Input, while the key \"To\" is utilized to determine the value to which the \"From\" key should be set, by referring to its key in the Values.")
                .items(
                    object()
                        .properties(
                            string(FROM)
                                .label("From"),
                            string(TO)
                                .label("To")))
                .required(true))
        .output()
        .perform(DataMapperMapMultipleValuesBetweenObjectsAction::perform);

    protected static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        // TODO
        return null;
    }
}
