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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.AgentType;

/**
 * The copilot-owned agent types: the workflow-editor, code-editor and cluster-element chat flows (each with its
 * ASK/BUILD variants and a coarse fallback), plus the copilot subagents invoked as tools.
 *
 * @author Ivica Cardic
 */
public enum CopilotAgentType implements AgentType {

    WORKFLOW_EDITOR_ASK("workflow_editor_ask", false),
    WORKFLOW_EDITOR_BUILD("workflow_editor_build", false),
    WORKFLOW_EDITOR("workflow_editor", true),
    CODE_EDITOR_ASK("code_editor_ask", false),
    CODE_EDITOR_BUILD("code_editor_build", false),
    CODE_EDITOR("code_editor", true),
    CLUSTER_ELEMENT_ASK("cluster_element_ask", false),
    CLUSTER_ELEMENT_BUILD("cluster_element_build", false),
    CLUSTER_ELEMENT("cluster_element", true),
    SKILLS("skills", false),
    CLUSTER_ELEMENT_AGENT("cluster_element_agent", false),
    CODE_EDITOR_AGENT("code_editor_agent", false),
    WORKFLOW_EDITOR_AGENT("workflow_editor_agent", false),
    CONVERTER_AGENT("converter_agent", false),
    WORKFLOW_EXECUTION_AGENT("workflow_execution_agent", false);

    private final String key;
    private final boolean fallback;

    CopilotAgentType(String key, boolean fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return fallback;
    }
}
