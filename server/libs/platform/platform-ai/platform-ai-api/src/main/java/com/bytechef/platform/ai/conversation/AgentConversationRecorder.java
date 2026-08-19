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

package com.bytechef.platform.ai.conversation;

import org.jspecify.annotations.Nullable;

/**
 * CE-side SPI seam so the canvas AI Agent component can report a completed conversation turn without depending on the
 * EE AI Hub module directly — the same idiom as {@code AiGuardrailsAdvisorProvider} and
 * {@code WorkspaceSystemPromptAdvisorProvider}. Consumers pull an implementation through an optional Spring bean; when
 * no EE implementation is registered, callers simply skip recording.
 *
 * @author Ivica Cardic
 */
public interface AgentConversationRecorder {

    /**
     * Records a single completed turn of an agent conversation so it can be surfaced in the AI Hub.
     *
     * @param agentConversation the turn to record
     */
    void recordTurn(AgentConversation agentConversation);

    /**
     * Self-contained by design: it carries the resolved creator so the Hub never looks an agent up, which is what keeps
     * ai-hub free of a dependency on automation-ai-agent.
     *
     * <p>
     * <b>The first three fields are not trustworthy on their own.</b> They are read off the workflow node's extension
     * map, and the keys that carry them are globally allowlisted reserved words, so a hand-authored definition (the
     * workflow editor, {@code updateWorkflow}, the MCP write tools) can set them to any value at all. The calling
     * component has no principal and no way to tell a generated definition from a hand-written one. {@code workflowId}
     * is what makes them checkable: it is supplied by the platform's own execution context rather than by the
     * definition, so the implementation can resolve the workflow's real owning workspace and reject a claim that does
     * not match it. An implementation that persists {@code workspaceId} or {@code creatorUserId} without that check
     * lets one workspace mint rows attributed to another.
     * </p>
     *
     * @param workspaceId    the workspace the agent claims to belong to — <b>unverified</b>, see above
     * @param aiAgentId      the agent whose conversation this turn belongs to
     * @param creatorUserId  the resolved <b>creator</b> of the agent, so the Hub never has to look the agent up itself.
     *                       It comes from {@code AiAgent.createdBy}, an immutable {@code @CreatedBy} audit field —
     *                       deliberately not called "owner", because nothing transfers it
     * @param conversationId the conversation's stable identifier
     * @param channelType    the inbound channel type (e.g. {@code "slack"}), or {@code null} when the calling action
     *                       cannot reach it
     * @param title          a display title for the conversation, or {@code null} when none is available
     * @param workflowId     the id of the workflow the executing job is running, straight from the platform's execution
     *                       context ({@code ActionContextAware#getWorkflowId()}) and therefore not forgeable by the
     *                       definition. Implementations use it to verify {@code workspaceId}. Null when the caller runs
     *                       outside a workflow context, in which case nothing can be verified
     * @param environmentId  the ordinal of the executing job's {@code Environment}, also from the execution context.
     *                       Null when unavailable
     */
    record AgentConversation(
        long workspaceId, long aiAgentId, long creatorUserId, String conversationId,
        @Nullable String channelType, @Nullable String title, @Nullable String workflowId,
        @Nullable Long environmentId) {
    }
}
