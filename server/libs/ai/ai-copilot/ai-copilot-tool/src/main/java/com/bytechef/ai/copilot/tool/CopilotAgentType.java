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
    CONFIGURE_CLUSTER_ELEMENT("configureClusterElement", false),
    WRITE_SCRIPT("writeScript", false),
    BUILD_WORKFLOW("buildWorkflow", false),
    IMPORT_WORKFLOW("importWorkflow", false),
    DEBUG_WORKFLOW_EXECUTION("debugWorkflowExecution", false),
    JSON_SCHEMA_BUILDER_ASK("json_schema_builder_ask", false),
    JSON_SCHEMA_BUILDER_BUILD("json_schema_builder_build", false),
    JSON_SCHEMA_BUILDER("json_schema_builder", true),
    SAMPLE_OUTPUT_ASK("sample_output_ask", false),
    SAMPLE_OUTPUT_BUILD("sample_output_build", false),
    SAMPLE_OUTPUT("sample_output", true),
    BUILD_CUSTOM_COMPONENT("buildCustomComponent", false),
    BUILD_CODE_WORKFLOW("buildCodeWorkflow", false),
    WORKFLOW_EDITOR_EMBEDDED_ASK("workflow_editor_embedded_ask", false),
    WORKFLOW_EDITOR_EMBEDDED_BUILD("workflow_editor_embedded_build", false),
    WORKFLOW_EDITOR_EMBEDDED("workflow_editor_embedded", true),
    BUILD_INTEGRATION_WORKFLOW("buildIntegrationWorkflow", false),
    CODE_WORKFLOW_ASK("code_workflow_ask", false),
    CODE_WORKFLOW_BUILD("code_workflow_build", false),
    CODE_WORKFLOW("code_workflow", true),
    CODE_WORKFLOW_EMBEDDED_ASK("code_workflow_embedded_ask", false),
    CODE_WORKFLOW_EMBEDDED_BUILD("code_workflow_embedded_build", false),
    CODE_WORKFLOW_EMBEDDED("code_workflow_embedded", true),
    WORKFLOW_EXECUTION_EMBEDDED_ASK("workflow_execution_embedded_ask", false),
    WORKFLOW_EXECUTION_EMBEDDED_BUILD("workflow_execution_embedded_build", false),
    WORKFLOW_EXECUTION_EMBEDDED("workflow_execution_embedded", true),
    CONTEXT_STORE_ASK("context_store_ask", false),
    CONTEXT_STORE_BUILD("context_store_build", false),
    CONTEXT_STORE("context_store", true),
    KNOWLEDGE_BASE_ASK("knowledge_base_ask", false),
    KNOWLEDGE_BASE_BUILD("knowledge_base_build", false),
    KNOWLEDGE_BASE("knowledge_base", true),
    DATA_TABLE_ASK("data_table_ask", false),
    DATA_TABLE_BUILD("data_table_build", false),
    DATA_TABLE("data_table", true),
    AI_AGENT_ASK("ai_agent_ask", false),
    AI_AGENT_BUILD("ai_agent_build", false),
    AI_AGENT("ai_agent", true),
    PROJECT_ASK("project_ask", false),
    PROJECT_BUILD("project_build", false),
    PROJECT("project", true),
    PROJECT_DEPLOYMENT_ASK("project_deployment_ask", false),
    PROJECT_DEPLOYMENT_BUILD("project_deployment_build", false),
    PROJECT_DEPLOYMENT("project_deployment", true),
    MCP_SERVER_ASK("mcp_server_ask", false),
    MCP_SERVER_BUILD("mcp_server_build", false),
    MCP_SERVER("mcp_server", true),
    ASSET_FILE_ASK("asset_file_ask", false),
    ASSET_FILE_BUILD("asset_file_build", false),
    ASSET_FILE("asset_file", true),
    API_COLLECTION_ASK("api_collection_ask", false),
    API_COLLECTION_BUILD("api_collection_build", false),
    API_COLLECTION("api_collection", true),
    CONFIGURE_MCP_SERVER("configureMcpServer", false);

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
