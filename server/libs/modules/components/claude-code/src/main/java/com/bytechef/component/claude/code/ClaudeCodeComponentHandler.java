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

package com.bytechef.component.claude.code;

import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.CLAUDE_CODE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.claude.code.action.ClaudeCodeChatAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ClaudeCodeComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CLAUDE_CODE + "_v1_ComponentHandler")
public class ClaudeCodeComponentHandler implements ComponentHandler {

    private final ClaudeCodeComponentDefinition componentDefinition;

    public ClaudeCodeComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new ClaudeCodeComponentDefinitionImpl(
            component(CLAUDE_CODE)
                .title("Claude Code")
                .description("AI agent with built-in tools for file system operations, code search, "
                    + "web fetching, shell execution, and task management.")
                .icon("path:assets/anthropic.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(ClaudeCodeChatAction.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ClaudeCodeComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ClaudeCodeComponentDefinition {

        public ClaudeCodeComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
