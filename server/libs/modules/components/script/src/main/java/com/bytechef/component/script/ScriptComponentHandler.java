
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

package com.bytechef.component.script;

import static com.bytechef.component.script.constants.ScriptConstants.INPUT;
import static com.bytechef.component.script.constants.ScriptConstants.JAVA;
import static com.bytechef.component.script.constants.ScriptConstants.JAVASCRIPT;
import static com.bytechef.component.script.constants.ScriptConstants.PYTHON;
import static com.bytechef.component.script.constants.ScriptConstants.R;
import static com.bytechef.component.script.constants.ScriptConstants.SCRIPT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;

import com.bytechef.component.script.constants.ScriptConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.Property;

/**
 * @author Matija Petanjek
 */
public class ScriptComponentHandler implements ComponentHandler {

    private static final PolyglotEngine polyglotEngine = new PolyglotEngine();

    private final ComponentDefinition componentDefinition = component(SCRIPT)
        .display(
            display("Script")
                .description(
                    "Executes user-defined code. User can write custom workflow logic in Java, JavaScript, Python, R or Ruby programming languages."))
        .actions(
            action(JAVA)
                .display(display("Java").description("Executes custom Java code."))
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(oneOf()),
                    string(SCRIPT)
                        .label("Java code")
                        .description("Add your Java custom logic here.")
                        .controlType(Property.ControlType.CODE))
                .output(oneOf())
                .perform(this::performJava),
            action(JAVASCRIPT)
                .display(display("JavaScript").description("Executes custom JavaScript code."))
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(oneOf()),
                    string(SCRIPT)
                        .label("JavaScript code")
                        .description("Add your JavaScript custom logic here.")
                        .controlType(Property.ControlType.CODE))
                .output(oneOf())
                .perform(this::performJavaScript),
            action(PYTHON)
                .display(display("Python").description("Executes custom Python code."))
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(oneOf()),
                    string(SCRIPT)
                        .label("Python code")
                        .description("Add your Python custom logic here.")
                        .controlType(Property.ControlType.CODE))
                .output(oneOf())
                .perform(this::performPython),
            action(R)
                .display(display("R").description("Executes custom R code."))
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(oneOf()),
                    string(SCRIPT)
                        .label("R code")
                        .description("Add your R custom logic here.")
                        .controlType(Property.ControlType.CODE))
                .output(oneOf())
                .perform(this::performR),
            action(ScriptConstants.RUBY)
                .display(display("Ruby").description("Executes custom Ruby code."))
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(oneOf()),
                    string(SCRIPT)
                        .label("Ruby code")
                        .description("Add your Ruby custom logic here.")
                        .controlType(Property.ControlType.CODE))
                .output(oneOf())
                .perform(this::performRuby));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performJava(Context context, ExecutionParameters executionParameters) {
        return polyglotEngine.execute("java", executionParameters);
    }

    protected Object performJavaScript(Context context, ExecutionParameters executionParameters) {
        return polyglotEngine.execute("js", executionParameters);
    }

    protected Object performPython(Context context, ExecutionParameters executionParameters) {
        return polyglotEngine.execute("python", executionParameters);
    }

    protected Object performR(Context context, ExecutionParameters executionParameters) {
        return polyglotEngine.execute("R", executionParameters);
    }

    protected Object performRuby(Context context, ExecutionParameters executionParameters) {
        return polyglotEngine.execute("ruby", executionParameters);
    }
}
