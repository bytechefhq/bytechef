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
import com.bytechef.ai.agent.tool.CurrentAgentContext;
import com.bytechef.ai.agent.tool.CurrentAgentContext.AgentBinding;
import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolution;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ai.copilot.tool.util.WorkflowPersistCaptureUtils;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Hand-rolled Spring AI {@link ToolCallback} that exposes the Converter Copilot subagent to a parent BUILD agent.
 * BUILD-only — there is no ASK variant of the Converter Copilot specialist.
 *
 * @author Ivica Cardic
 */
public class ConverterAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ConverterAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a request to convert an external workflow definition (n8n, Make, Zapier, Workato,
            etc.) into a ByteChef workflow. The Converter subagent owns the canonical behaviour for this
            domain: translating constructs, mapping integrations, and producing valid ByteChef workflow
            JSON plus a rationale. Include the source workflow definition to convert in the request. Pass
            workflowId to convert into an EXISTING workflow, or projectId to create a BRAND-NEW workflow in
            that project and convert into it; never both. It cannot create a project, so create or choose
            that first.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user request in natural language, including the source workflow definition. Pass through verbatim - the subagent does its own task decomposition."
                    },
                    "projectId": {
                        "type": "integer",
                        "description": "Id of the project to create a new workflow in and convert into. Give this or workflowId, never both."
                    },
                    "workflowId": {
                        "type": "string",
                        "description": "Id of an existing workflow to convert into. Give this or projectId, never both."
                    }
                },
                "required": ["request"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final @Nullable TargetWorkflowManager targetWorkflowManager;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ConverterAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this(chatClientFactory, chatModelResolver, null);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ConverterAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        @Nullable TargetWorkflowManager targetWorkflowManager) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
        this.targetWorkflowManager = targetWorkflowManager;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("importWorkflow")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ConverterAgentInput input = JsonUtils.read(toolInput, ConverterAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            Long projectId = input.projectId();
            String workflowId = input.workflowId();

            if (projectId != null && workflowId != null && !workflowId.isBlank()) {
                return toolError(
                    "Give either projectId (to create a new workflow) or workflowId (to convert into an existing " +
                        "workflow), not both");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            Map<String, Object> forwardedContext = WorkflowPersistCaptureUtils.withCaptureHolder(parentContext);

            ChatClient converterChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            if (projectId == null) {
                String content = callSubAgent(converterChatClient, workflowId, request, parentAgent, forwardedContext);

                return toResult(content, request, forwardedContext);
            }

            if (targetWorkflowManager == null) {
                return toolError(
                    "importWorkflow cannot create a workflow here - create it first and pass its workflowId");
            }

            String createdWorkflowId = targetWorkflowManager.createWorkflow(
                projectId, new ToolContext(forwardedContext));

            String content;

            try {
                content = callSubAgent(converterChatClient, createdWorkflowId, request, parentAgent, forwardedContext);
            } catch (RuntimeException exception) {
                deleteUnwrittenWorkflow(createdWorkflowId, forwardedContext);

                throw exception;
            }

            boolean written = WorkflowPersistCaptureUtils.isUpdated(forwardedContext, createdWorkflowId);

            if (!written && (content == null || isToolError(content))) {
                deleteUnwrittenWorkflow(createdWorkflowId, forwardedContext);

                return content == null ? nullResultError(request) : content;
            }

            String result = toResult(content, request, forwardedContext);

            if (written) {
                return result;
            }

            return result + "\n\nThe new workflow was created with workflowId=\"" + createdWorkflowId +
                "\" but has no converted definition yet. Call importWorkflow again with this workflowId to continue.";
        } catch (JacksonException exception) {
            log.warn(
                "importWorkflow rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (ChatModelUnavailableException exception) {
            log.warn("importWorkflow cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                ConverterAgentToolCallback.class, "importWorkflow", exception);
        }
    }

    private static @Nullable String callSubAgent(
        ChatClient converterChatClient, @Nullable String workflowId, String request, @Nullable AgentType parentAgent,
        Map<String, Object> forwardedContext) {

        String subAgentRequest = workflowId == null || workflowId.isBlank()
            ? request : "Target workflowId: " + workflowId + "\n\n" + request;

        return CurrentAgentContext.callWith(
            CopilotAgentType.IMPORT_WORKFLOW, parentAgent,
            () -> converterChatClient.prompt(subAgentRequest)
                .toolContext(forwardedContext)
                .call()
                .content());
    }

    private static String toResult(@Nullable String content, String request, Map<String, Object> forwardedContext) {
        if (content == null) {
            return nullResultError(request);
        }

        String trailer = WorkflowPersistCaptureUtils.renderTrailer(forwardedContext);

        return trailer == null ? content : content + trailer;
    }

    private static String nullResultError(String request) {
        log.warn("converter subagent returned null for request='{}'", request);

        return ToolErrors.toolError("converter subagent returned null");
    }

    private void deleteUnwrittenWorkflow(String workflowId, Map<String, Object> forwardedContext) {
        if (targetWorkflowManager == null || WorkflowPersistCaptureUtils.isUpdated(forwardedContext, workflowId)) {
            return;
        }

        try {
            targetWorkflowManager.deleteWorkflow(workflowId, new ToolContext(forwardedContext));
        } catch (RuntimeException exception) {
            log.warn("importWorkflow failed to delete the empty workflow {} it created", workflowId, exception);
        }
    }

    private static boolean isToolError(String result) {
        String trimmedResult = result.strip();

        if (!trimmedResult.startsWith("{")) {
            return false;
        }

        try {
            JsonNode resultJsonNode = JsonUtils.readTree(trimmedResult);

            JsonNode errorJsonNode = resultJsonNode.get("error");

            return errorJsonNode != null && !errorJsonNode.isNull();
        } catch (JacksonException exception) {
            return false;
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ConverterAgentInput(
        @Nullable String request, @Nullable Long projectId, @Nullable String workflowId) {
    }

    public interface TargetWorkflowManager {

        String createWorkflow(long projectId, ToolContext toolContext);

        void deleteWorkflow(String workflowId, ToolContext toolContext);
    }
}
