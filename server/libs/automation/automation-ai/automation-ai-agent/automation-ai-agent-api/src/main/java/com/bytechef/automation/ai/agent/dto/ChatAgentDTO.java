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

package com.bytechef.automation.ai.agent.dto;

/**
 * Read-model for one deployed, chat-reachable agent workflow — the "Agent Chats" counterpart of
 * {@code ProjectDeploymentWorkflowGraphQlController.ChatWorkflow}.
 *
 * <p>
 * A separate row type (and a separate query) rather than more {@code ChatWorkflow} rows because agents live in hidden
 * {@code __AI_AGENT__} projects, which every projectId-scoped {@code ProjectDeploymentService} listing path filters out
 * — {@code workspaceChatWorkflows} structurally cannot return them. The agent's identity (id/name/title), not the
 * hidden backing project's, is what the client shows.
 * </p>
 *
 * @param aiAgentId           the owning agent's id
 * @param agentName           the agent's slug name
 * @param agentTitle          the agent's display title
 * @param projectDeploymentId the {@code ProjectDeployment} the chat workflow is deployed under
 * @param workflowExecutionId the static-webhook {@code WorkflowExecutionId}, in the exact string form the chat webhook
 *                            endpoint expects as its path segment — identical in construction to
 *                            {@code ChatWorkflow.workflowExecutionId}, so the client opens an agent chat through the
 *                            same code path as a project chat
 * @param workflowLabel       the chat workflow's label
 *
 * @author Ivica Cardic
 */
public record ChatAgentDTO(
    long aiAgentId, String agentName, String agentTitle, long projectDeploymentId, String workflowExecutionId,
    String workflowLabel) {
}
