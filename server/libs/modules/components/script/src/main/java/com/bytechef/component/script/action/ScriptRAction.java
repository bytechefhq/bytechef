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

package com.bytechef.component.script.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;

import com.bytechef.component.definition.Property;
import com.bytechef.component.script.action.definition.ScriptActionDefinition;
import com.bytechef.platform.component.runner.TaskRunnerRegistry;

/**
 * Not registered in {@code ScriptComponentHandler} - its entry there is commented out.
 *
 * <p>
 * It passes the registry to {@link ScriptActionDefinition}, so {@code perform} would route through the selected runner,
 * but it contributes no {@code taskRunner} property the way {@link ScriptJavaScriptAction} does. Re-enabling it as it
 * stands therefore yields an action that routes through a runner a workflow author cannot select: with no property in
 * the definition, every task falls back to {@code graalvm}. Whoever re-enables it must add
 * {@code TaskRunnerPropertyFactory.taskRunnerProperty(...)} to the property list as well.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class ScriptRAction {

    public static ScriptActionDefinition of(TaskRunnerRegistry taskRunnerRegistry) {
        return new ScriptActionDefinition(
            action("r")
                .title("R")
                .description("Executes custom R code.")
                .properties(
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                            time())
                        .expressionEnabled(false),
                    string(SCRIPT)
                        .label("R Code")
                        .description("Add your R custom logic here.")
                        .controlType(Property.ControlType.CODE_EDITOR)
                        .languageId("R")
                        .defaultValue("perform <- function(input, context) {\n\treturn null\n}")
                        .required(true))
                .output(),
            "R", taskRunnerRegistry);
    }

    private ScriptRAction() {
    }
}
