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

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_KEY;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FIELD_VALUE;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class DataMapperMergeAndPivotByKeyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL
        .action("mergeAndPivotPropertiesByKey")
        .title("Merge and pivot properties by key")
        .description(
            "Creates a new object that has the specified field key as kay and an object as value. The object in the value has all properties defined in the input field that have the specified field key. Each one of those input values becomes a property key with the specified field value as property value.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("The input object that contains property keys and values.")
                .required(true),
            string(FIELD_KEY)
                .label("Field Key")
                .description("The key of the newly created object.")
                .required(true),
            string(FIELD_VALUE)
                .label("Field Value")
                .description("The value of each property in the newly created objects value.")
                .required(true))
        .output()
        .perform(DataMapperMergeAndPivotByKeyAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return null;
    }
}
