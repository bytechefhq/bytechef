
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.component.var.constant.VarConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;

/**
 * @author Ivica Cardic
 */
public class VarSetAction {
    public static final ActionDefinition ACTION_DEFINITION = action(VarConstants.SET)
        .title("Set value")
        .description("Assign value to a variable that can be used in the following steps.")
        .properties(oneOf(VarConstants.VALUE)
            .label("Value")
            .description("Value of any type to set.")
            .required(true))
        .execute(VarSetAction::executeSetValue);

    public static Object executeSetValue(Context context, InputParameters inputParameters) {
        return inputParameters.get(VarConstants.VALUE);
    }
}
