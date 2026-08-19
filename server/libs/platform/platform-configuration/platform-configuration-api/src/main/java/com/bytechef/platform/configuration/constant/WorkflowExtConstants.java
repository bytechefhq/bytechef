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

package com.bytechef.platform.configuration.constant;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class WorkflowExtConstants {

    /**
     * The AI Agent whose conversation turns are reported to the AI Hub. Part of the AI Hub visibility identity stamp
     * (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}): three sibling keys {@code AiAgentWorkflowGenerator}
     * writes directly onto the generated {@code aiAgent_1} node, alongside {@link #CLUSTER_ELEMENTS}.
     * {@code AbstractAiAgentChatAction} (the AI Agent component) reads them back as opaque values to report a completed
     * conversation turn to the optional, EE-only {@code AgentConversationRecorder} port — see that class and
     * {@code AiAgentWorkflowGenerator} for the write/read sides. All three are registered as reserved words in
     * {@code WorkflowReservedWordContributorImpl} alongside {@link #CLUSTER_ELEMENTS}/{@link #CONNECTIONS}, since every
     * top-level task key must be allowlisted there for {@code AbstractWorkflowMapper#validateReservedWords} to accept
     * the generated definition.
     */
    public static final String AI_HUB_AGENT_ID = "aiHubAgentId";

    /**
     * The platform user id of the agent's <b>creator</b> — resolved from {@code AiAgent.createdBy}, an immutable
     * {@code @CreatedBy} audit field, so it is deliberately not named "owner": nothing transfers it. Part of the AI Hub
     * visibility identity stamp; see {@link #AI_HUB_AGENT_ID} for the full contract.
     */
    public static final String AI_HUB_CREATOR_USER_ID = "aiHubCreatorUserId";

    /**
     * The workspace the stamped agent belongs to. Part of the AI Hub visibility identity stamp; see
     * {@link #AI_HUB_AGENT_ID} for the full contract.
     */
    public static final String AI_HUB_WORKSPACE_ID = "aiHubWorkspaceId";

    public static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    public static final String CLUSTER_ELEMENTS = "clusterElements";
    public static final String COMPONENT_NAME = "componentName";
    public static final String COMPONENT_VERSION = "componentVersion";
    public static final String CONNECTIONS = "connections";
    public static final String GROUP_NAME = "groupName";
    public static final String INTERNAL_ONLY = "internalOnly";
    public static final String OBJECT_NAME = "objectName";
    public static final String TRIGGERS = "triggers";

    /**
     * The embedded realtime pipeline (voice session sub-workflow) carried on a trigger. A structural key, not a
     * component-declared trigger property — see WebsocketTasks in platform-websocket-webhook-rest.
     */
    public static final String WEBSOCKET_TASKS = "websocketTasks";

    /**
     * Every structural workflow-definition key that is not a built-in {@code WorkflowConstants} word, contributed to
     * {@code AbstractWorkflowMapper#validateReservedWords} by each {@code WorkflowReservedWordContributor}. A key
     * missing from this list makes the definition carrying it fail to parse with "unknown workflow definition
     * property", so this is the single source of truth: both contributor implementations — the shared one in
     * {@code platform-configuration-service} and the standalone one in {@code runtime-job-app} — return it verbatim
     * rather than each enumerating their own copy, which is exactly how the two previously drifted.
     *
     * <p>
     * {@code category} and {@code tags} have no constant of their own: they are project/workflow metadata rather than
     * node extensions, so they would not belong in this class outside this list.
     * </p>
     */
    public static final List<String> RESERVED_WORDS = List.of(
        AI_HUB_AGENT_ID, AI_HUB_CREATOR_USER_ID, AI_HUB_WORKSPACE_ID, AUTHORIZATION_REQUIRED, "category",
        CLUSTER_ELEMENTS, COMPONENT_NAME, COMPONENT_VERSION, CONNECTIONS, GROUP_NAME, INTERNAL_ONLY, OBJECT_NAME,
        "tags", TRIGGERS, WEBSOCKET_TASKS);
}
