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
    MCP_SERVER_ASK(Source.MCP_SERVER, Mode.ASK),
    MCP_SERVER_BUILD(Source.MCP_SERVER, Mode.BUILD),
    AI_AGENT_ASK(Source.AI_AGENT, Mode.ASK),
    AI_AGENT_BUILD(Source.AI_AGENT, Mode.BUILD),
    DATA_TABLE_ASK(Source.DATA_TABLE, Mode.ASK),
    DATA_TABLE_BUILD(Source.DATA_TABLE, Mode.BUILD),
    KNOWLEDGE_BASE_ASK(Source.KNOWLEDGE_BASE, Mode.ASK),
    KNOWLEDGE_BASE_BUILD(Source.KNOWLEDGE_BASE, Mode.BUILD),
    WORKFLOW_EXECUTION_ASK(Source.WORKFLOW_EXECUTION, Mode.ASK),
    WORKFLOW_EXECUTION_BUILD(Source.WORKFLOW_EXECUTION, Mode.BUILD),
    PROJECT_ASK(Source.PROJECT, Mode.ASK),
    PROJECT_BUILD(Source.PROJECT, Mode.BUILD),
    DEPLOYMENT_ASK(Source.DEPLOYMENT, Mode.ASK),
    DEPLOYMENT_BUILD(Source.DEPLOYMENT, Mode.BUILD),
    PROJECT_DEPLOYMENT_ASK(Source.PROJECT_DEPLOYMENT, Mode.ASK),
    PROJECT_DEPLOYMENT_BUILD(Source.PROJECT_DEPLOYMENT, Mode.BUILD),
    ASSET_FILE_ASK(Source.ASSET_FILE, Mode.ASK),
    ASSET_FILE_BUILD(Source.ASSET_FILE, Mode.BUILD),
    API_COLLECTION_ASK(Source.API_COLLECTION, Mode.ASK),
    API_COLLECTION_BUILD(Source.API_COLLECTION, Mode.BUILD),
    COMPONENT_RULE_ASK(Source.COMPONENT_RULE, Mode.ASK),
    COMPONENT_RULE_BUILD(Source.COMPONENT_RULE, Mode.BUILD),
    WORKFLOW_EDITOR_EMBEDDED_ASK(Source.WORKFLOW_EDITOR_EMBEDDED, Mode.ASK),
    WORKFLOW_EDITOR_EMBEDDED_BUILD(Source.WORKFLOW_EDITOR_EMBEDDED, Mode.BUILD),
    CODE_WORKFLOW_ASK(Source.CODE_WORKFLOW, Mode.ASK),
    CODE_WORKFLOW_BUILD(Source.CODE_WORKFLOW, Mode.BUILD),
    CODE_WORKFLOW_EMBEDDED_ASK(Source.CODE_WORKFLOW_EMBEDDED, Mode.ASK),
    CODE_WORKFLOW_EMBEDDED_BUILD(Source.CODE_WORKFLOW_EMBEDDED, Mode.BUILD),
    CUSTOM_COMPONENT_ASK(Source.CUSTOM_COMPONENT, Mode.ASK),
    CUSTOM_COMPONENT_BUILD(Source.CUSTOM_COMPONENT, Mode.BUILD),
    SKILLS("skills"),
    CONFIGURE_CLUSTER_ELEMENT("configureClusterElement"),
    WRITE_SCRIPT("writeScript"),
    BUILD_WORKFLOW("buildWorkflow"),
    IMPORT_WORKFLOW("importWorkflow"),
    DEBUG_WORKFLOW_EXECUTION("debugWorkflowExecution"),
    BUILD_INTEGRATION_WORKFLOW("buildIntegrationWorkflow"),
    CONFIGURE_MCP_SERVER("configureMcpServer"),
    BUILD_AI_AGENT("buildAiAgent"),
    BUILD_CUSTOM_COMPONENT("buildCustomComponent"),
    BUILD_CODE_WORKFLOW("buildCodeWorkflow"),
    EXPLAIN_CUSTOM_COMPONENT("explainCustomComponent"),
    EXPLAIN_CODE_WORKFLOW("explainCodeWorkflow");

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
