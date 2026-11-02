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
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;

/**
 * The copilot-owned agent types: the chat agents registered per {@link Source} and {@link Mode} (their keys are derived
 * from {@link Source#agentId(Mode)}, so the agent registration and the chat routing cannot drift apart), plus the
 * copilot subagents invoked as tools (keyed by their tool name).
 *
 * @author Ivica Cardic
 */
public enum CopilotAgentType implements AgentType {

    WORKFLOW_EDITOR_ASK(Source.WORKFLOW_EDITOR, Mode.ASK),
    WORKFLOW_EDITOR_BUILD(Source.WORKFLOW_EDITOR, Mode.BUILD),
    CODE_EDITOR_ASK(Source.CODE_EDITOR, Mode.ASK),
    CODE_EDITOR_BUILD(Source.CODE_EDITOR, Mode.BUILD),
    WORKFLOW_CODE_EDITOR_ASK(Source.WORKFLOW_CODE_EDITOR, Mode.ASK),
    WORKFLOW_CODE_EDITOR_BUILD(Source.WORKFLOW_CODE_EDITOR, Mode.BUILD),
    CLUSTER_ELEMENT_ASK(Source.CLUSTER_ELEMENT, Mode.ASK),
    CLUSTER_ELEMENT_BUILD(Source.CLUSTER_ELEMENT, Mode.BUILD),
    CONVERTER_BUILD(Source.CONVERTER, Mode.BUILD),
    SKILLS_ASK(Source.SKILLS, Mode.ASK),
    SKILLS_BUILD(Source.SKILLS, Mode.BUILD),
    JSON_SCHEMA_BUILDER_ASK(Source.JSON_SCHEMA_BUILDER, Mode.ASK),
    JSON_SCHEMA_BUILDER_BUILD(Source.JSON_SCHEMA_BUILDER, Mode.BUILD),
    SAMPLE_OUTPUT_ASK(Source.SAMPLE_OUTPUT, Mode.ASK),
    SAMPLE_OUTPUT_BUILD(Source.SAMPLE_OUTPUT, Mode.BUILD),
    MCP_SERVER_BUILD(Source.MCP_SERVER, Mode.BUILD),
    WORKFLOW_EDITOR_EMBEDDED_ASK(Source.WORKFLOW_EDITOR_EMBEDDED, Mode.ASK),
    WORKFLOW_EDITOR_EMBEDDED_BUILD(Source.WORKFLOW_EDITOR_EMBEDDED, Mode.BUILD),
    SKILLS("skills"),
    CONFIGURE_CLUSTER_ELEMENT("configureClusterElement"),
    WRITE_SCRIPT("writeScript"),
    BUILD_WORKFLOW("buildWorkflow"),
    IMPORT_WORKFLOW("importWorkflow"),
    DEBUG_WORKFLOW_EXECUTION("debugWorkflowExecution"),
    BUILD_INTEGRATION_WORKFLOW("buildIntegrationWorkflow"),
    CONFIGURE_MCP_SERVER("configureMcpServer");

    private final String key;

    CopilotAgentType(Source source, Mode mode) {
        this(source.agentId(mode));
    }

    CopilotAgentType(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return false;
    }
}
