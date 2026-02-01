/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.script.action.ScriptJavaAction;
import com.bytechef.component.script.action.ScriptJavaScriptAction;
import com.bytechef.component.script.action.ScriptPythonAction;
import com.bytechef.component.script.action.ScriptRAction;
import com.bytechef.component.script.action.ScriptRubyAction;
import com.bytechef.component.script.datastream.ScriptJavaItemProcessor;
import com.bytechef.component.script.datastream.ScriptJavaScriptItemProcessor;
import com.bytechef.component.script.datastream.ScriptPythonItemProcessor;
import com.bytechef.component.script.datastream.ScriptRubyItemProcessor;
import com.bytechef.component.script.engine.PolyglotEngine;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component(SCRIPT + "_v1_ComponentHandler")
public class ScriptComponentHandler implements ComponentHandler {

    private final ScriptComponentDefinition componentDefinition;

    public ScriptComponentHandler(PolyglotEngine polyglotEngine) {
        this.componentDefinition = new ScriptComponentDefinitionImpl(polyglotEngine);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ScriptComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ScriptComponentDefinition {

        private ScriptComponentDefinitionImpl(PolyglotEngine polyglotEngine) {
            super(
                component(SCRIPT)
                    .title("Script")
                    .description(
                        "Executes user-defined code. User can write custom workflow logic in Java, JavaScript, Python, R or Ruby programming languages.")
                    .icon("path:assets/script.svg")
                    .categories(ComponentCategory.HELPERS, ComponentCategory.DEVELOPER_TOOLS)
                    .actions(
                        ScriptJavaScriptAction.of(polyglotEngine),
                        ScriptPythonAction.of(polyglotEngine),
                        ScriptRAction.of(polyglotEngine),
                        ScriptRubyAction.of(polyglotEngine),
                        ScriptJavaAction.of(polyglotEngine))
                    .clusterElements(
                        ScriptJavaItemProcessor.of(polyglotEngine),
                        ScriptJavaScriptItemProcessor.of(polyglotEngine),
                        ScriptPythonItemProcessor.of(polyglotEngine),
                        ScriptRubyItemProcessor.of(polyglotEngine)));
        }
    }
}
