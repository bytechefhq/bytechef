
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

package com.bytechef.component.var;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.var.action.VarSetAction;
import com.bytechef.component.var.constant.VarConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * @author Ivica Cardic
 */
public class VarComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(VarConstants.VAR)
        .display(display("Var").description("Sets a value which can then be referenced in other tasks."))
        .actions(VarSetAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
