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

package com.bytechef.component.script.cluster.tool;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Property.ControlType.CODE_EDITOR;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;

import com.bytechef.component.script.cluster.tool.definition.ScriptToolDefinition;
import com.bytechef.component.script.engine.PolyglotEngine;

/**
 * @author Ivica Cardic
 */
public class ScriptPythonTool {

    public static ScriptToolDefinition of(PolyglotEngine polyglotEngine) {
        return new ScriptToolDefinition(
            clusterElement("python")
                .title("Python")
                .description("Executes custom Python code as an AI agent tool.")
                .type(TOOLS)
                .properties(
                    string(TOOL_NAME)
                        .label("Name")
                        .description("The tool name exposed to the AI model.")
                        .expressionEnabled(false)
                        .required(true),
                    string(TOOL_DESCRIPTION)
                        .label("Description")
                        .description("The tool description exposed to the AI model.")
                        .controlType(TEXT_AREA)
                        .expressionEnabled(false)
                        .required(true),
                    object(INPUT)
                        .label("Input")
                        .description("Initialize parameter values used in the custom code.")
                        .additionalProperties(
                            array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                            time()),
                    string(SCRIPT)
                        .label("Python Code")
                        .description("Custom Python code to execute as a tool.")
                        .controlType(CODE_EDITOR)
                        .languageId("python")
                        .defaultValue("def perform(input, context):\n\treturn None")
                        .required(true)),
            "python", polyglotEngine);
    }

    private ScriptPythonTool() {
    }
}
