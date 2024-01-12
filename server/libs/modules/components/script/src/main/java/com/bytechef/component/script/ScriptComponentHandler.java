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

package com.bytechef.component.script;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.script.constant.ScriptConstants.SCRIPT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.script.action.ScriptJavaAction;
import com.bytechef.component.script.action.ScriptJavaScriptAction;
import com.bytechef.component.script.action.ScriptPythonAction;
import com.bytechef.component.script.action.ScriptRubyAction;
import com.google.auto.service.AutoService;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ScriptComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(SCRIPT)
        .title("Script")
        .description(
            "Executes user-defined code. User can write custom workflow logic in Java, JavaScript, Python, R or Ruby programming languages.")
        .icon("path:assets/script.svg")
        .actions(
            ScriptJavaAction.ACTION_DEFINITION,
            ScriptJavaScriptAction.ACTION_DEFINITION,
            ScriptPythonAction.ACTION_DEFINITION,
//            ScriptRAction.ACTION_DEFINITION,
            ScriptRubyAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
