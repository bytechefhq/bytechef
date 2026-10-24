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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowAgentToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ProjectWorkflowAgentToolCallback.class);

    private static final String DESCRIPTION =
        """
            Delegate a user request about whole workflows to a specialised Workflow Editor subagent. Use
            this for requests that design, edit, debug, or explain a workflow (orchestration of tasks,
            triggers, conditions, loops). The subagent owns the canonical behaviour for this domain;
            prefer calling it over reasoning about workflow shape directly. ASK mode returns analysis;
            BUILD mode returns the updated workflow JSON plus a change rationale. For an EXISTING workflow
            include its workflowId in the request. For a BRAND-NEW workflow include the target projectId
            instead — the subagent creates the workflow itself, carrying the complete definition in one
            call. It cannot create a project, so create or choose that first.""";

    private static final String FIX_REQUEST =
        """
            The workflow you saved (workflowId=%s) still fails the workflow editor's validation with these errors:
            %s

            Load it with getWorkflow, fix every error with updateWorkflow, then run validateWorkflow again. Use only \
            output properties the tasks really produce and resources that exist. If an error can only be fixed by \
            the user, leave it and say so.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "request": {
                        "type": "string",
                        "description": "The user request in natural language. Pass through verbatim — the subagent does its own task decomposition."
                    }
                },
                "required": ["request"]
            }""";

    private final IntelligentToolChatClientFactory chatClientFactory;
    private final @Nullable SubAgentChatModelResolver chatModelResolver;
    private final @Nullable PersistedWorkflowChecker persistedWorkflowChecker;
    private final String toolName;
    private final String description;
    private final CopilotAgentType agentType;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ProjectWorkflowAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver) {

        this(chatClientFactory, chatModelResolver, null);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ProjectWorkflowAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        @Nullable PersistedWorkflowChecker persistedWorkflowChecker) {

        this(
            chatClientFactory, chatModelResolver, "buildWorkflow", DESCRIPTION, CopilotAgentType.BUILD_WORKFLOW,
            persistedWorkflowChecker);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ProjectWorkflowAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        String toolName, String description, CopilotAgentType agentType) {

        this(chatClientFactory, chatModelResolver, toolName, description, agentType, null);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ProjectWorkflowAgentToolCallback(
        IntelligentToolChatClientFactory chatClientFactory, @Nullable SubAgentChatModelResolver chatModelResolver,
        String toolName, String description, CopilotAgentType agentType,
        @Nullable PersistedWorkflowChecker persistedWorkflowChecker) {

        this.chatClientFactory = chatClientFactory;
        this.chatModelResolver = chatModelResolver;
        this.persistedWorkflowChecker = persistedWorkflowChecker;
        this.toolName = toolName;
        this.description = description;
        this.agentType = agentType;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(toolName)
            .description(description)
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
            ProjectWorkflowAgentInput input = JsonUtils.read(toolInput, ProjectWorkflowAgentInput.class);

            String request = input.request();

            if (request == null || request.isBlank()) {
                return toolError("request is required and must not be blank");
            }

            AgentBinding parent = CurrentAgentContext.current();
            AgentType parentAgent = parent != null ? parent.agentName() : null;

            Map<String, Object> parentContext = toolContext == null ? Map.of() : toolContext.getContext();

            Map<String, Object> forwardedContext = WorkflowPersistCaptureUtils.withCaptureHolder(parentContext);

            ChatClient workflowEditorChatClient =
                chatClientFactory.get(SubAgentChatModelResolution.resolve(chatModelResolver, parentContext));

            String result = CurrentAgentContext.callWith(
                agentType, parentAgent,
                () -> workflowEditorChatClient.prompt(request)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            if (result == null) {
                log.warn("workflow_editor subagent returned null for request='{}'", request);

                return ToolErrors.toolError("workflow_editor subagent returned null");
            }

            String checkReport = checkPersistedWorkflow(
                workflowEditorChatClient, agentType, parentAgent, forwardedContext);

            String trailer = WorkflowPersistCaptureUtils.renderTrailer(forwardedContext);

            return (trailer == null ? result : result + trailer) + checkReport;
        } catch (JacksonException exception) {
            log.warn(
                "buildWorkflow rejected malformed tool input: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (ChatModelUnavailableException exception) {
            log.warn("buildWorkflow cannot run its sub-agent: {}", exception.getMessage());

            throw exception;
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                ProjectWorkflowAgentToolCallback.class, "buildWorkflow", exception);
        }
    }

    @SuppressFBWarnings(
        value = "VA_FORMAT_STRING_USES_NEWLINE",
        justification = "The fix request is LLM-facing content that must use a literal \\n, not the platform-dependent %n line separator.")
    private String checkPersistedWorkflow(
        ChatClient workflowEditorChatClient, CopilotAgentType agentType, @Nullable AgentType parentAgent,
        Map<String, Object> forwardedContext) {

        String workflowId = WorkflowPersistCaptureUtils.fetchLastWorkflowId(forwardedContext);

        if (persistedWorkflowChecker == null || workflowId == null) {
            return "";
        }

        List<PersistedWorkflowChecker.Issue> issues = checkQuietly(workflowId);

        if (issues == null) {
            return "";
        }

        List<PersistedWorkflowChecker.Issue> fixableIssues = issues.stream()
            .filter(issue -> !issue.userAction())
            .toList();

        if (!fixableIssues.isEmpty()) {
            String fixRequest = FIX_REQUEST.formatted(workflowId, formatIssues(fixableIssues));

            CurrentAgentContext.callWith(
                agentType, parentAgent,
                () -> workflowEditorChatClient.prompt(fixRequest)
                    .toolContext(forwardedContext)
                    .call()
                    .content());

            issues = checkQuietly(workflowId);
        }

        return issues == null ? "" : renderCheckReport(issues);
    }

    private @Nullable List<PersistedWorkflowChecker.Issue> checkQuietly(String workflowId) {
        try {
            return persistedWorkflowChecker == null ? null : persistedWorkflowChecker.check(workflowId);
        } catch (RuntimeException exception) {
            log.warn("Could not validate the workflow {} buildWorkflow saved", workflowId, exception);

            return null;
        }
    }

    static String renderCheckReport(List<PersistedWorkflowChecker.Issue> issues) {
        if (issues.isEmpty()) {
            return "\n\n[WORKFLOW CHECK] The saved workflow passed the editor's validation.";
        }

        List<PersistedWorkflowChecker.Issue> remainingErrors = issues.stream()
            .filter(issue -> !issue.userAction())
            .toList();
        List<PersistedWorkflowChecker.Issue> userActions = issues.stream()
            .filter(PersistedWorkflowChecker.Issue::userAction)
            .toList();

        String report = "\n\n[WORKFLOW CHECK]";

        if (!remainingErrors.isEmpty()) {
            report += " The saved workflow still has errors the editor will show. Tell the user about them and do " +
                "not say the workflow is ready:\n" + formatIssues(remainingErrors);
        }

        if (!userActions.isEmpty()) {
            String lead = remainingErrors.isEmpty() ? " The saved workflow is valid apart from" : "\nIt also needs";

            report += lead + " these steps only the user can do; ask the user to do them:\n" +
                formatIssues(userActions);
        }

        return report;
    }

    private static String formatIssues(List<PersistedWorkflowChecker.Issue> issues) {
        return issues.stream()
            .map(issue -> issue.nodeName() == null
                ? "- " + issue.message() : "- " + issue.nodeName() + ": " + issue.message())
            .collect(Collectors.joining("\n"));
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }

    public record ProjectWorkflowAgentInput(String request) {
    }
}
