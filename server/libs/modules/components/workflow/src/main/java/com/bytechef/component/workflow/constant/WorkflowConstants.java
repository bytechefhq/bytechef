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

package com.bytechef.component.workflow.constant;

/**
 * @author Ivica Cardic
 */
public class WorkflowConstants {

    /**
     * The {@code inputSchema} the {@code workflowCall} agent channel pins on {@code newWorkflowCall}. That trigger's
     * output is function-valued off its own {@code inputSchema} property (see
     * {@code WorkflowResponseUtils#triggerOutput}), so pinning this schema is the only thing that makes the trigger
     * emit the agent channel contract's {@code conversationId} / {@code message} / {@code attachments} names. It is the
     * JSON-schema spelling of {@code ComponentDsl.agentChannelRequest()}: the same property names, and a required list
     * that is that helper's plus {@code message}. Both halves of that claim are enforced by
     * {@code WorkflowNewWorkflowCallTriggerAgentChannelTest}, which compares this literal against the helper itself —
     * without which the claim could only decay, as it already had once when the helper gained property descriptions.
     * Descriptions are deliberately NOT carried across: the helper's exist to label a trigger's output in the editor,
     * while this literal is fed to a schema builder that never renders them.
     * <p>
     * Both {@code conversationId} and {@code message} are required, so the advertised contract matches what the
     * generated workflow actually depends on at both ends. {@code conversationId} because the generated workflow's
     * {@code branch_in} reads {@code ${workflowCall_1.conversationId}} — leaving it optional would let a caller produce
     * a run whose chat-memory key is null. {@code message} because a workflow calling an agent with no text at all is
     * almost certainly a mistake. This is stricter than the schema the generator pinned before this channel existed,
     * which required {@code message} alone.
     */
    public static final String AI_AGENT_CALL_INPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"conversationId\":{\"type\":\"string\"},"
            + "\"message\":{\"type\":\"string\"},"
            + "\"attachments\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}}},"
            + "\"required\":[\"conversationId\",\"message\"]}";

    /**
     * The {@code outputSchema} the {@code workflowCall} agent channel pins on {@code responseToWorkflowCall}. That
     * action's {@code response} property is a {@code dynamicProperties} map whose members are derived from this schema,
     * so pinning it is what makes {@code response.message} a real target.
     */
    public static final String AI_AGENT_CALL_OUTPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}},\"required\":[\"message\"]}";

    public static final String NEW_WORKFLOW_ERROR = "newWorkflowError";
    public static final String RESPONSE = "response";

    /**
     * The agent channel's stored key, as persisted in {@code ai_agent_channel.channel_type}. It is deliberately NOT the
     * trigger's own name ({@code newWorkflowCall}) — renaming or re-deriving it would break every stored row.
     */
    public static final String WORKFLOW_CALL = "workflowCall";
}
