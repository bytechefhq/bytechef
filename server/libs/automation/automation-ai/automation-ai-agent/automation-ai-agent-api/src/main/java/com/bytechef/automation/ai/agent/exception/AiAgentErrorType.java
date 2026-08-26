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

package com.bytechef.automation.ai.agent.exception;

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.exception.AbstractErrorType;

/**
 * Domain error codes raised by {@link AiAgentFacade}. Keys are numeric and MUST remain stable — downstream consumers
 * (clients, exception resolvers, test assertions) key off {@link #getErrorKey()}.
 *
 * @author Ivica Cardic
 */
public class AiAgentErrorType extends AbstractErrorType {

    /**
     * A second singleton-kind ({@code MODEL}/{@code KNOWLEDGE_BASE}/{@code CHAT_MEMORY}/{@code APPROVAL_GATE}/
     * {@code APPROVAL_TOOL}) element. {@code AUTO_MEMORY} was a singleton kind too, but is retired — that built-in is
     * now controlled by {@code AiAgent.settings.builtInTools.autoMemory} instead of a stored row.
     */
    public static final AiAgentErrorType ELEMENT_KIND_ALREADY_PRESENT = new AiAgentErrorType(100);

    /** Deleting the permanent {@code chat}/{@code workflowCall} channel. */
    public static final AiAgentErrorType CHANNEL_NOT_DELETABLE = new AiAgentErrorType(101);

    /** Adding a second {@code chat} or {@code workflowCall} channel. */
    public static final AiAgentErrorType CHANNEL_ALREADY_PRESENT = new AiAgentErrorType(102);

    /** {@code channelType} is not a registered {@code AiAgentChannelType}. */
    public static final AiAgentErrorType UNKNOWN_CHANNEL_TYPE = new AiAgentErrorType(103);

    /** Deleting an agent whose backing project has one or more {@code project_deployment} rows. */
    public static final AiAgentErrorType AGENT_HAS_DEPLOYMENTS = new AiAgentErrorType(104);

    /** Deleting an agent that another agent references as a {@code SUB_AGENT} element. */
    public static final AiAgentErrorType AGENT_REFERENCED_AS_SUB_AGENT = new AiAgentErrorType(105);

    /** Adding a {@code SUB_AGENT} element would create a reference cycle (including a self-reference). */
    public static final AiAgentErrorType SUB_AGENT_CYCLE = new AiAgentErrorType(106);

    /** Publishing an agent that does not have exactly one {@code MODEL} element. */
    public static final AiAgentErrorType MODEL_MISSING = new AiAgentErrorType(107);

    /**
     * Publishing an agent that has a channel whose {@code ChannelDefinition.connectionRequired()} is {@code true} but
     * whose {@code connectionId} is {@code null}.
     */
    public static final AiAgentErrorType CHANNEL_CONNECTION_MISSING = new AiAgentErrorType(108);

    /**
     * Publishing an agent that has a {@code SUB_AGENT} element whose target agent's backing project has no
     * {@code PUBLISHED} version.
     */
    public static final AiAgentErrorType SUB_AGENT_NOT_PUBLISHED = new AiAgentErrorType(109);

    // Error keys 110 (UNKNOWN_APPROVAL_CHANNEL) and 111 (APPROVAL_CHANNEL_MISSING) are retired along with the
    // APPROVAL_CHANNEL element: approvals are delivered over the agent's own channels, so there is no separate row to
    // validate and no configuration in which a gated tool has nowhere to ask. Do not reuse the numbers.

    /**
     * Publishing an agent whose {@code settings.builtInTools.webSearch} is enabled but carries no
     * {@code webSearchConnectionId} — the generated {@code aiAgentUtils/v1/braveWebSearchTool} tool would have no
     * connection to read its API key from.
     */
    public static final AiAgentErrorType BUILT_IN_TOOL_CONNECTION_MISSING = new AiAgentErrorType(112);

    /** Importing an agent from a document that is not valid JSON, or that carries no title. */
    public static final AiAgentErrorType INVALID_AGENT_IMPORT = new AiAgentErrorType(113);

    /**
     * Publishing an agent that has a channel whose row does not carry a parameter its reply action declares REQUIRED.
     * The channel declaration maps row parameters onto reply properties
     * ({@code AgentReplyDefinition.channelParameter(...)}), and the generator simply omits a mapped parameter the row
     * does not carry — so without this the reply task would be generated missing a required value and fail on the
     * agent's first answer instead of at publish time.
     */
    public static final AiAgentErrorType CHANNEL_PARAMETER_MISSING = new AiAgentErrorType(114);

    /**
     * A sharing operation named an agent that does not exist, or one whose {@code workspace_id} is null. Deliberately
     * one code for both: telling them apart would let a caller probe agent ids. Raised by
     * {@code AiAgentSharingFacadeImpl}, the EE agent-keyed face of {@code ProjectSharingFacade}, which collapses its
     * own two cases the same way.
     */
    public static final AiAgentErrorType INVALID_AGENT = new AiAgentErrorType(115);

    /**
     * Publishing an agent whose {@code settings.builtInTools.webSearchProvider} is {@code NATIVE} while its
     * {@code MODEL} element names a provider with no provider-side web search — the generated model element would carry
     * a {@code webSearch} parameter that provider's model cluster element does not declare, so the agent would publish
     * looking search-capable and then never search. See {@code AiAgentSettings#NATIVE_WEB_SEARCH_MODEL_PROVIDERS}.
     */
    public static final AiAgentErrorType NATIVE_WEB_SEARCH_UNSUPPORTED = new AiAgentErrorType(116);

    public AiAgentErrorType(int errorKey) {
        super(AiAgentFacade.class, errorKey);
    }
}
