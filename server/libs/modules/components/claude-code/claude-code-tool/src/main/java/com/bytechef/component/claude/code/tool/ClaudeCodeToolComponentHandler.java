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

package com.bytechef.component.claude.code.tool;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * Registers all built-in Claude Code tools as cluster elements.
 *
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ClaudeCodeToolComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("claudeCodeTool")
        .title("Claude Code Tool")
        .description("Built-in tools for the Claude Code agent.")
        .icon("path:assets/anthropic.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .clusterElements(
            ClaudeCodeFileSystemTools.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeShellTools.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeGrepTool.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeGlobTool.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeSmartWebFetchTool.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeBraveWebSearchTool.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeSkillsTool.CLUSTER_ELEMENT_DEFINITION,
            ClaudeCodeTodoWriteTool.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
