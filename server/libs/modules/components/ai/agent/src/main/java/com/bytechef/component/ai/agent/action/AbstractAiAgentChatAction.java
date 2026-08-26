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

package com.bytechef.component.ai.agent.action;

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.MAX_TOOL_CALLS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.RESPONSE_PROMPT;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.SIMULATION_MODEL;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.TOOL_SIMULATIONS;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction.CHECK_FOR_VIOLATIONS;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction.SANITIZE_TEXT;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agent.action.event.ToolExecutionEvent;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.agent.tool.AiAgentConversationCheckpoint;
import com.bytechef.component.ai.agent.tool.ConversationResume;
import com.bytechef.component.ai.agent.tool.ConversationState;
import com.bytechef.component.ai.agent.tool.SuspendableToolCallingManager;
import com.bytechef.component.ai.llm.ChatModel.ResponseFormat;
import com.bytechef.component.ai.llm.advisor.ContextLoggerAdvisor;
import com.bytechef.component.ai.llm.converter.JsonSchemaStructuredOutputConverter;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.ai.llm.tool.DelegatingToolCallback;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.RagFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractAiAgentChatAction {

    private static final Logger log = LoggerFactory.getLogger(AbstractAiAgentChatAction.class);
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private static final String TOOL_SIMULATION_UNAVAILABLE = "[tool simulation unavailable]";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementToolCallbacks clusterElementToolCallbacks;
    private final AgentToolCallingManagers agentToolCallingManagers;

    private final @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider;
    private final @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider;
    private final @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider;
    private final @Nullable ObjectProvider<AgentConversationRecorder> agentConversationRecorderObjectProvider;

    /**
     * Bounds the partial-identity-stamp warning to one line per action instance — see
     * {@link #recordAgentConversationTurn}. An action is a long-lived singleton, so this trades "every misconfigured
     * agent is named" for "a misconfigured agent cannot flood the log on every turn"; the first one named is enough to
     * start diagnosing.
     */
    private final AtomicBoolean partialIdentityStampWarned = new AtomicBoolean();

    /**
     * Bounds the recorder-failure warning to one line per action instance — see {@link #warnAboutRecordingFailure}.
     */
    private final AtomicBoolean recordingFailureWarned = new AtomicBoolean();

    protected AbstractAiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers) {

        this(aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers, null);
    }

    protected AbstractAiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider) {

        this(aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, null);
    }

    protected AbstractAiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider) {

        this(aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider, null);
    }

    protected AbstractAiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider) {

        this(aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider,
            workspaceSystemPromptAdvisorProviderObjectProvider, null);
    }

    protected AbstractAiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<AgentConversationRecorder> agentConversationRecorderObjectProvider) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementToolCallbacks =
            new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);
        this.agentToolCallingManagers = agentToolCallingManagers;
        this.toolExecutionRecorderObjectProvider = toolExecutionRecorderObjectProvider;
        this.aiGuardrailsAdvisorProviderObjectProvider = aiGuardrailsAdvisorProviderObjectProvider;
        this.workspaceSystemPromptAdvisorProviderObjectProvider = workspaceSystemPromptAdvisorProviderObjectProvider;
        this.agentConversationRecorderObjectProvider = agentConversationRecorderObjectProvider;
    }

    /**
     * Resolves the optional tool-invocation audit recorder. Absent (null) when the app variant does not carry the
     * platform-tool-execution service module, or when the action was built through the recorder-less constructor — gate
     * decisions are then simply not audited.
     */
    private @Nullable ToolExecutionRecorder fetchToolExecutionRecorder() {
        return toolExecutionRecorderObjectProvider == null
            ? null : toolExecutionRecorderObjectProvider.getIfAvailable();
    }

    protected ChatClient.ChatClientRequestSpec getChatClientRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        @Nullable ToolExecutionListener toolExecutionListener, ActionContext context) throws Exception {

        return getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, toolExecutionListener, context,
            ModelUtils.getMessages(inputParameters, context));
    }

    private ChatModel resolveChatModel(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        ClusterElementMap clusterElementMap) throws Exception {

        ClusterElement modelClusterElement = clusterElementMap.getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(),
            modelClusterElement.getClusterElementName());

        ComponentConnection modelConnection = connectionParameters.get(modelClusterElement.getWorkflowNodeName());

        Map<String, Object> concatenatedInputParameters = MapUtils.concat(
            new HashMap<>(inputParameters.toMap()), new HashMap<>(modelClusterElement.getParameters()));

        return (ChatModel) modelFunction.apply(
            ParametersFactory.create(concatenatedInputParameters),
            ParametersFactory.create(modelConnection.getParameters()), true);
    }

    protected ChatClient.ChatClientRequestSpec getChatClientRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        @Nullable ToolExecutionListener toolExecutionListener, ActionContext context,
        List<Message> messages) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ChatModel chatModel = resolveChatModel(inputParameters, connectionParameters, clusterElementMap);

        String conversationId = resolveConversationId(clusterElementMap);

        // Build the chat-memory Result once here and share it with getAdvisors so the stateful SessionService backing
        // the memory implementation is not constructed twice (the Result carries the advisor AND the recall tool
        // callbacks, both needed downstream).
        Optional<ChatMemoryFunction.Result> chatMemoryResult =
            clusterElementMap.fetchClusterElement(CHAT_MEMORY)
                .map(clusterElement -> buildChatMemoryResult(connectionParameters, clusterElement, context));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> toolSimulations =
            (Map<String, Map<String, String>>) inputParameters.get(TOOL_SIMULATIONS);

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        // Workspace-bound content guardrails, resolved through the optional CE SPI so this component never depends on
        // the EE guardrails module directly (see AiGuardrailsAdvisorProvider). Registered ahead of the rest of the
        // advisor chain — the returned advisor self-orders at HIGHEST_PRECEDENCE, but listing it first here documents
        // that it is meant to run before every other advisor, including the per-node GUARDRAILS cluster elements added
        // by getAdvisors below.
        List<Advisor> workspaceAdvisors = new ArrayList<>();

        if (aiGuardrailsAdvisorProviderObjectProvider != null
            && context instanceof ActionContextAware actionContextAware) {
            aiGuardrailsAdvisorProviderObjectProvider.ifAvailable(
                provider -> provider
                    .getAdvisor(actionContextAware.getPlatformType(), actionContextAware.getJobPrincipalId(),
                        "ai_agent")
                    .ifPresent(workspaceAdvisors::add));
        }

        // Workspace-level system prompt, resolved through the same optional-CE-SPI idiom (see
        // WorkspaceSystemPromptAdvisorProvider). The returned advisor self-orders AFTER the guardrails advisor above,
        // so listing both in one list is order-safe regardless of which providers are present.
        if (workspaceSystemPromptAdvisorProviderObjectProvider != null
            && context instanceof ActionContextAware actionContextAware) {
            workspaceSystemPromptAdvisorProviderObjectProvider.ifAvailable(
                provider -> provider
                    .getAdvisor(actionContextAware.getPlatformType(), actionContextAware.getJobPrincipalId(),
                        "ai_agent")
                    .ifPresent(workspaceAdvisors::add));
        }

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = createPrompt(chatClient, inputParameters, context)
            .advisors(workspaceAdvisors)
            .advisors(
                getAdvisors(
                    clusterElementMap, connectionParameters, chatModel, context, chatMemoryResult,
                    createConversationCheckpointer(inputParameters, context),
                    inputParameters.getInteger(MAX_TOOL_CALLS)))
            .advisors(getConversationAdvisor(conversationId))
            .messages(messages)
            .tools(
                concatToolCallbacks(
                    getToolCallbacks(
                        clusterElementMap.getClusterElements(BaseToolFunction.TOOLS),
                        connectionParameters, context.isEditorEnvironment(), toolExecutionListener, toolSimulations,
                        chatModel, context),
                    chatMemoryResult)
                        .toArray());

        // Propagate the conversation id into the @Tool ToolContext (separate from the advisor-context map set via
        // getConversationAdvisor). The session recall tool (conversation_search / SessionEventTools) reads the session
        // id from tool-context key SessionEventTools.SESSION_ID_CONTEXT_KEY == ChatMemory.CONVERSATION_ID. The literal
        // mirrors that constant; kept as a literal to avoid coupling this module to spring-ai-session-management.
        //
        // RC1 ChatClientRequestSpec.toolContext(Map) MERGES (putAll) into the existing tool-context map rather than
        // replacing it, so this does not clobber the ACTION_CONTEXT / SSE keys set at the other call sites.
        if (conversationId != null) {
            chatClientRequestSpec.toolContext(Map.of("chat_memory_conversation_id", conversationId));
        }

        return chatClientRequestSpec;
    }

    private ChatMemoryFunction.Result buildChatMemoryResult(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement,
        ActionContext context) {

        ChatMemoryFunction chatMemoryFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return chatMemoryFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw clusterElementInitializationException(clusterElement, "chat memory", e, context);
        }
    }

    private static RuntimeException clusterElementInitializationException(
        ClusterElement clusterElement, String kind, Throwable cause, ActionContext context) {

        Class<? extends Throwable> causeClass = cause.getClass();

        String message = String.format(
            "Failed to initialize %s advisor for cluster element '%s' (component=%s v%d): %s",
            kind, clusterElement.getClusterElementName(), clusterElement.getComponentName(),
            clusterElement.getComponentVersion(),
            cause.getMessage() == null ? causeClass.getSimpleName() : cause.getMessage());

        context.log(log -> log.error(message, cause));

        return new IllegalStateException(message, cause);
    }

    private static ToolCallback createObservableToolCallback(
        ToolCallback delegate, AtomicReference<@Nullable AgentThinking> thinkingReference,
        ToolExecutionListener toolExecutionListener, ActionContext context) {

        return new ToolCallback() {

            private final ToolDefinition toolDefinition = delegate.getToolDefinition();

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                return observeAndCall(toolInput, () -> delegate.call(toolInput));
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                return observeAndCall(toolInput, () -> delegate.call(toolInput, toolContext));
            }

            private String observeAndCall(String toolInput, Supplier<String> execution) {
                log.debug("Tool '{}' request: {}", toolDefinition.name(), toolInput);

                Map<String, Object> inputs;

                try {
                    inputs = JSON_MAPPER.readValue(toolInput, new TypeReference<>() {});
                } catch (Exception exception) {
                    context.log(
                        log -> log.debug(
                            "Failed to parse tool input as JSON for '{}': {}", toolDefinition.name(),
                            exception.getMessage()));

                    inputs = Map.of("rawInput", toolInput);
                }

                String result = execution.get();

                AgentThinking agentThinking = thinkingReference.getAndSet(null);

                try {
                    toolExecutionListener.onToolExecution(
                        new ToolExecutionEvent(
                            toolDefinition.name(), inputs, result,
                            agentThinking != null ? agentThinking.reasoning() : null,
                            agentThinking != null ? agentThinking.confidence() : null));
                } catch (Exception exception) {
                    context.log(
                        log -> log.warn(
                            "Tool execution listener failed for '{}'", toolDefinition.name(), exception));
                }

                return result;
            }
        };
    }

    protected Object resumeChat(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = buildPatchedRequestSpec(
            inputParameters, connectionParameters, extensions, continueParameters, data, context);

        return ModelUtils.getChatActionResult(chatClientRequestSpec.call(), inputParameters, context)
            .response();
    }

    protected ChatClient.ChatClientRequestSpec buildPatchedRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

        ConversationState conversationState = continueParameters.get(
            ToolSuspendConstants.CONVERSATION_STATE, ConversationState.class);

        if (conversationState == null) {
            throw new IllegalStateException(
                "Resume continuation is missing the serialized conversation state (key '" +
                    ToolSuspendConstants.CONVERSATION_STATE + "'). The agent's tool-calling loop cannot be " +
                    "reconstructed; the suspend may have been recorded after a delegate tool-call failure.");
        }

        String pendingToolCallId = continueParameters.getRequiredString(
            ToolSuspendConstants.PENDING_TOOL_CALL_ID);

        // A suspend carrying GATED_TOOL_NAME came from the per-tool approval gate, not from a suspending tool:
        // the human's decision determines the tool response — approve executes the tool with the AI-chosen
        // arguments, reject feeds a denial back into the loop. Ordinary tool suspends keep the raw form data.
        String resumeData = continueParameters.getString(ToolSuspendConstants.GATED_TOOL_NAME) != null
            ? resolveGatedToolResumeData(
                inputParameters, continueParameters, data, connectionParameters, extensions, context)
            : JsonUtils.write(data.toMap());

        List<Message> conversation = ConversationResume.patchPendingToolResponse(
            conversationState.toMessages(), pendingToolCallId, resumeData);

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, null, context, conversation);

        chatClientRequestSpec.toolContext(Map.of(AiAgentToolContextKey.ACTION_CONTEXT, context));

        return chatClientRequestSpec;
    }

    /**
     * Resolves the tool-response text for a resume of a per-tool approval-gate suspension. Approval executes the gated
     * tool with the originally captured arguments — through the RAW callback, bypassing the gate, since the human
     * approved this exact invocation — and reports the result together with any reviewer comment. Rejection feeds an
     * explicit denial (with the comment) back into the loop so the LLM can replan.
     */
    @SuppressWarnings("unchecked")
    String resolveGatedToolResumeData(
        Parameters inputParameters, Parameters continueParameters, Parameters data,
        Map<String, ComponentConnection> connectionParameters, Parameters extensions, ActionContext context)
        throws Exception {

        boolean approved = data.getBoolean("approved", false);
        String comment = data.getString("comment");
        boolean hasComment = comment != null && !comment.isBlank();
        // Reserved, server-verified key set by JobResumeFacade when the resolving channel established an identity;
        // absent for the anonymous hosted form.
        String approvedBy = data.getString("approvedBy");
        boolean hasApprovedBy = approvedBy != null && !approvedBy.isBlank();
        ToolExecutionRecorder toolExecutionRecorder = fetchToolExecutionRecorder();

        if (!approved) {
            recordGateResolution(
                toolExecutionRecorder, continueParameters, context, ToolExecutionOutcome.APPROVAL_DENIED);

            Map<String, Object> denial = new HashMap<>();

            denial.put("denied", true);
            denial.put("reason", hasComment ? "Denied by reviewer: " + comment : "Denied by reviewer.");

            if (hasApprovedBy) {
                denial.put("deniedBy", approvedBy);
            }

            return JsonUtils.write(denial);
        }

        String gatedToolName = continueParameters.getRequiredString(ToolSuspendConstants.GATED_TOOL_NAME);
        String gatedToolInput = continueParameters.getRequiredString(ToolSuspendConstants.GATED_TOOL_INPUT);
        boolean editorEnvironment = ((ActionContextAware) context).isEditorEnvironment();

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ToolCallback gatedToolCallback = clusterElementMap.getClusterElements(BaseToolFunction.TOOLS)
            .stream()
            .flatMap(
                clusterElement -> buildElementToolCallbacks(
                    clusterElement, connectionParameters, editorEnvironment, context).stream())
            .filter(toolCallback -> {
                ToolDefinition toolDefinition = toolCallback.getToolDefinition();

                return gatedToolName.equals(toolDefinition.name());
            })
            .findFirst()
            // A gated tool's callback reports its delegate's definition, so the lookup above finds the gate wrapper.
            // Unwrap it: the human approved this exact invocation, so re-gating would raise a second request.
            .map(DelegatingToolCallback::unwrap)
            .orElseThrow(() -> new IllegalStateException(
                "The approved tool '" + gatedToolName + "' is no longer configured on the agent node; the " +
                    "approval cannot be applied."));

        // Re-apply the tool-simulation wrapper the live loop would have applied: without it, approving a gated tool
        // in a simulated (editor/test) run would execute the REAL tool with real side effects — exactly what
        // simulations exist to prevent.
        Map<String, Map<String, String>> toolSimulations =
            (Map<String, Map<String, String>>) inputParameters.get(TOOL_SIMULATIONS);

        ToolCallback effectiveToolCallback = toolSimulations == null || toolSimulations.isEmpty()
            ? gatedToolCallback
            : createSimulationAwareToolCallback(
                gatedToolCallback, toolSimulations,
                resolveChatModel(inputParameters, connectionParameters, clusterElementMap), context);

        Map<String, Object> approvedResult = new HashMap<>();

        approvedResult.put("approvedByReviewer", true);

        if (hasComment) {
            approvedResult.put("reviewerComment", comment);
        }

        if (hasApprovedBy) {
            approvedResult.put("reviewer", approvedBy);
        }

        ToolContext toolContext = new ToolContext(Map.of(AiAgentToolContextKey.ACTION_CONTEXT, context));

        try {
            // The recorder wraps the approved execution so the audit trail carries the post-approval outcome
            // (SUCCESS or ERROR) with the measured duration; without a recorder the tool simply runs unaudited.
            String result = toolExecutionRecorder == null
                ? effectiveToolCallback.call(gatedToolInput, toolContext)
                : toolExecutionRecorder.record(
                    createGateResolutionEventBuilder(continueParameters, context),
                    () -> effectiveToolCallback.call(gatedToolInput, toolContext));

            approvedResult.put("result", result);
        } catch (Exception exception) {
            approvedResult.put(
                "error", Objects.toString(exception.getMessage(), "Tool execution failed after approval"));
        }

        return JsonUtils.write(approvedResult);
    }

    /**
     * Emits a direct (execution-less) gate-resolution audit event; used for the denial branch, where no tool runs. Tool
     * name and outcome only — reviewer comments and tool arguments stay out of the audit trail.
     */
    private static void recordGateResolution(
        @Nullable ToolExecutionRecorder toolExecutionRecorder, Parameters continueParameters, ActionContext context,
        ToolExecutionOutcome outcome) {

        if (toolExecutionRecorder == null) {
            return;
        }

        toolExecutionRecorder.record(
            createGateResolutionEventBuilder(continueParameters, context)
                .outcome(outcome)
                .build());
    }

    // The agent module has its own action.event.ToolExecutionEvent (the SSE listener event), so the platform audit
    // event is referenced fully qualified.
    private static com.bytechef.platform.tool.execution.ToolExecutionEvent.Builder createGateResolutionEventBuilder(
        Parameters continueParameters, ActionContext context) {

        return com.bytechef.platform.tool.execution.ToolExecutionEvent
            .builder(
                ToolExecutionSurface.AI_AGENT, ToolExecutionKind.COMPONENT,
                continueParameters.getRequiredString(ToolSuspendConstants.GATED_TOOL_NAME))
            .jobId(((ActionContextAware) context).getJobId());
    }

    /**
     * Creates the per-tool-round conversation checkpointer for durable (non-editor, job-attached) executions, or
     * {@code null} when checkpointing does not apply. The checkpoint carries a fingerprint of the evaluated input
     * parameters so a resume only replays a checkpoint written by this same agent node configuration.
     */
    protected @Nullable Consumer<List<Message>> createConversationCheckpointer(
        Parameters inputParameters, ActionContext context) {

        if (!(context instanceof ActionContextAware actionContextAware) || actionContextAware.getJobId() == null ||
            actionContextAware.isEditorEnvironment()) {

            return null;
        }

        // The fingerprint is computed lazily, at checkpoint-write time: the tool-calling manager catches and logs
        // checkpointer failures, so a serialization problem can never fail the request-spec build or the turn.
        return conversation -> context.data(
            data -> data.put(
                ActionContext.Data.Scope.CURRENT_EXECUTION, AiAgentConversationCheckpoint.DATA_KEY,
                new AiAgentConversationCheckpoint(
                    getConversationFingerprint(inputParameters), ConversationState.from(conversation))));
    }

    /**
     * Returns the conversation restored from a crash checkpoint left by a previous, interrupted run of this job, or
     * {@code null} when there is none (or it belongs to a differently-parameterized node). Restoration is best-effort:
     * any failure logs and falls back to a fresh conversation.
     */
    protected @Nullable List<Message> fetchCheckpointedConversation(
        Parameters inputParameters, ActionContext context) {

        if (!(context instanceof ActionContextAware actionContextAware) || actionContextAware.getJobId() == null ||
            actionContextAware.isEditorEnvironment()) {

            return null;
        }

        try {
            Optional<Object> checkpointOptional = context.data(
                data -> data.fetch(
                    ActionContext.Data.Scope.CURRENT_EXECUTION, AiAgentConversationCheckpoint.DATA_KEY));

            if (checkpointOptional.isEmpty()) {
                return null;
            }

            AiAgentConversationCheckpoint checkpoint = ConvertUtils.convertValue(
                checkpointOptional.get(), AiAgentConversationCheckpoint.class);

            if (!Objects.equals(checkpoint.fingerprint(), getConversationFingerprint(inputParameters))) {
                return null;
            }

            ConversationState conversationState = checkpoint.conversationState();

            List<Message> messages = conversationState.toMessages();

            context.log(logger -> logger.info(
                "Resuming agent conversation from crash checkpoint (%d messages)".formatted(messages.size())));

            return messages;
        } catch (RuntimeException exception) {
            log.warn("Failed to restore agent conversation checkpoint; starting fresh", exception);

            return null;
        }
    }

    /** Removes this job's conversation checkpoint after the agent turn completed successfully. */
    protected void clearConversationCheckpoint(ActionContext context) {
        if (!(context instanceof ActionContextAware actionContextAware) || actionContextAware.getJobId() == null ||
            actionContextAware.isEditorEnvironment()) {

            return;
        }

        try {
            context.data(
                data -> data.remove(
                    ActionContext.Data.Scope.CURRENT_EXECUTION, AiAgentConversationCheckpoint.DATA_KEY));
        } catch (RuntimeException exception) {
            log.warn("Failed to clear agent conversation checkpoint", exception);
        }
    }

    private static String getConversationFingerprint(Parameters inputParameters) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(
                JsonUtils.write(inputParameters.toMap())
                    .getBytes(StandardCharsets.UTF_8));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Whether this action streams its response. Streaming actions always produce text: a streamed response cannot be
     * validated against a JSON schema or self-corrected once the tokens are flowing, so they force
     * {@link ResponseFormat#TEXT} regardless of what the input parameters request.
     */
    protected boolean isStreaming() {
        return false;
    }

    // Package-private for testing the streaming force-to-TEXT guard.
    ChatClient.ChatClientRequestSpec createPrompt(
        ChatClient chatClient, Parameters inputParameters, ActionContext context) {

        ResponseFormat responseFormat = isStreaming()
            ? ResponseFormat.TEXT
            : inputParameters.getFromPath(RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class, ResponseFormat.TEXT);

        if (responseFormat == ResponseFormat.TEXT) {
            return chatClient.prompt();
        }

        JsonSchemaStructuredOutputConverter converter = new JsonSchemaStructuredOutputConverter(
            inputParameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), context);

        return chatClient.prompt(converter.getFormat());
    }

    protected static void applyStructuredOutputValidation(
        ChatClient.ChatClientRequestSpec chatClientRequestSpec, Parameters inputParameters, ActionContext context) {

        ResponseFormat responseFormat = inputParameters.getFromPath(
            RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class, ResponseFormat.TEXT);

        if (responseFormat == ResponseFormat.TEXT) {
            return;
        }

        JsonSchemaStructuredOutputConverter converter = new JsonSchemaStructuredOutputConverter(
            inputParameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), context);

        chatClientRequestSpec.advisors(
            StructuredOutputValidationAdvisor.builder()
                .outputJsonSchema(converter.getJsonSchema())
                .build());
    }

    private static ToolCallback createSimulationAwareToolCallback(
        ToolCallback delegate, Map<String, Map<String, String>> toolSimulations, ChatModel chatModel,
        ActionContext context) {

        String toolName = delegate.getToolDefinition()
            .name();

        Map<String, String> simulation = toolSimulations.get(toolName);

        if (simulation == null) {
            return delegate;
        }

        return new ToolCallback() {

            private final ToolDefinition toolDefinition = delegate.getToolDefinition();

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                return getSimulatedResult(toolInput, simulation, chatModel, context);
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                return getSimulatedResult(toolInput, simulation, chatModel, context);
            }
        };
    }

    List<Advisor> getAdvisors(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters,
        ChatModel chatModel, ActionContext context, Optional<ChatMemoryFunction.Result> chatMemoryResult,
        @Nullable Consumer<List<Message>> conversationCheckpointer, @Nullable Integer maxToolCalls) {

        List<Advisor> advisors = new ArrayList<>();

        List<ClusterElement> guardrailClusterElements = clusterElementMap.getClusterElements(GUARDRAILS);

        long checkForViolationsCount = guardrailClusterElements.stream()
            .filter(clusterElement -> Objects.equals(CHECK_FOR_VIOLATIONS.key(), clusterElement.getComponentName()))
            .count();

        if (checkForViolationsCount > 1) {
            throw new IllegalStateException(
                "Multiple CheckForViolations parent cluster elements configured — advisor order collides at " +
                    "HIGHEST_PRECEDENCE and Spring AI ordering becomes undefined. Configure at most one.");
        }

        long sanitizeTextCount = guardrailClusterElements.stream()
            .filter(clusterElement -> Objects.equals(SANITIZE_TEXT.key(), clusterElement.getComponentName()))
            .count();

        if (sanitizeTextCount > 1) {
            throw new IllegalStateException(
                "Multiple SanitizeText parent cluster elements configured — advisor order collides at " +
                    "DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 1 and Spring AI ordering becomes undefined. " +
                    "Configure at most one.");
        }

        if (!guardrailClusterElements.isEmpty()) {
            List<Message> conversationHistory = chatMemoryResult
                .map(result -> loadConversationHistory(result.chatMemory(), clusterElementMap))
                .orElse(List.of());

            for (ClusterElement clusterElement : guardrailClusterElements) {
                advisors.add(getGuardrailsAdvisor(connectionParameters, clusterElement, context, conversationHistory));
            }
        }

        // memory

        chatMemoryResult
            .map(ChatMemoryFunction.Result::advisor)
            .ifPresent(advisors::add);

        // tool call
        //
        // When the selected chat-memory type can safely persist the full tool request/response transcript, its advisor
        // is built with an inside-the-loop order (TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER > ToolCallingAdvisor
        // .DEFAULT_ORDER), so it records every intra-turn tool message. In that case the tool advisor's own in-loop
        // history MUST be disabled — otherwise the same transcript is written twice (once by each history). Memory
        // types that don't support it keep the default outside-the-loop placement and the tool advisor keeps its own
        // in-loop history (the Spring AI RC1 default that keeps each tool-call iteration valid).

        boolean persistToolMessagesInLoop = chatMemoryResult
            .map(ChatMemoryFunction.Result::supportsToolMessagePersistence)
            .orElse(false);

        SuspendableToolCallingManager suspendableToolCallingManager = new SuspendableToolCallingManager(
            agentToolCallingManagers.getToolCallingManager(maxToolCalls), (ActionContextAware) context,
            conversationCheckpointer);

        ToolCallingAdvisor toolCallingAdvisor = persistToolMessagesInLoop
            ? ToolCallingAdvisor.builder()
                .toolCallingManager(suspendableToolCallingManager)
                .disableInternalConversationHistory()
                .build()
            : ToolCallingAdvisor.builder()
                .toolCallingManager(suspendableToolCallingManager)
                .build();

        advisors.add(toolCallingAdvisor);

        // One advisor per knowledge base: RAG is a multiple cluster element, and fetchClusterElement would return
        // only the first, silently retrieving from one knowledge base while the agent is configured with several.
        for (ClusterElement ragClusterElement : clusterElementMap.getClusterElements(RAG)) {
            advisors.add(getRagAdvisor(connectionParameters, ragClusterElement, context));
        }

        advisors.add(new ContextLoggerAdvisor(context));

        return advisors;
    }

    private static Parameters getConnectionParameters(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return ParametersFactory.create(componentConnection);
    }

    /**
     * Resolves the conversation id off the node's (possibly absent) {@code CHAT_MEMORY} cluster element — extracted so
     * both {@link #getChatClientRequestSpec} and the turn recorder resolve it the same way. When the element is
     * configured with no explicit {@code conversationId} parameter, a fresh random one is returned on every call; that
     * fallback is unreachable for an AI-Agent-generated workflow (its generator always pins an explicit
     * {@code conversationId} expression), so calling this a second time after the turn completes cannot observe a
     * different value there — see {@link #recordAgentConversationTurn}.
     */
    private static @Nullable String resolveConversationId(ClusterElementMap clusterElementMap) {
        return clusterElementMap.fetchClusterElement(CHAT_MEMORY)
            .map(clusterElement -> {
                Parameters chatMemoryParameters = ParametersFactory.create(clusterElement.getParameters());

                String id = chatMemoryParameters.getString("conversationId");

                if (id != null) {
                    return id;
                }

                UUID uuid = UUID.randomUUID();

                return uuid.toString();
            })
            .orElse(null);
    }

    /**
     * Captures the tenant bound to the <b>calling</b> thread and returns a runnable that re-binds it before reporting
     * the turn, for actions that only know the turn is complete on some other thread — the streaming action reports
     * from {@code Flux#doOnComplete}, which runs on a reactor thread.
     *
     * <p>
     * {@link com.bytechef.tenant.TenantContext} is a bare {@link ThreadLocal} defaulting to {@code "public"}, so a
     * reactor thread would otherwise route the Hub write into the wrong tenant's schema — and
     * {@link #recordAgentConversationTurn}'s fail-open catch would swallow the evidence. The environment gets the same
     * treatment one level up, through {@code AiAgentStreamChatAction#withEnvironmentContext} and
     * {@code EnvironmentContextThreadLocalAccessor}; a {@code TenantContextThreadLocalAccessor} exists too, but both
     * ride on {@code Hooks.enableAutomaticContextPropagation()}, which is installed by a
     * {@code platform-configuration-service} bean that need not be on the classpath wherever this component runs.
     * Capturing explicitly here holds regardless.
     * </p>
     *
     * <p>
     * The returned runnable does not do the work itself: it hands it to {@link Schedulers#boundedElastic()}. Reporting
     * a turn opens a JDBC transaction and walks the conversation's whole session transcript to refresh the Hub row's
     * preview and message count, which is an O(number of turns) <b>blocking</b> read — and the completion callback of a
     * streaming LLM response runs on a shared <b>non-blocking</b> reactor thread ({@code reactor-http-nio-*}). Left
     * inline, one long-lived channel conversation would stall that event loop for every unrelated request it also
     * serves. The tenant is captured explicitly (below), so the extra hop costs nothing in fidelity.
     * </p>
     *
     * <p>
     * Must be called on the perform thread (i.e. while assembling the flux), not from inside the callback.
     * </p>
     */
    protected Runnable createAgentConversationTurnRecorder(Parameters extensions, ActionContext context) {
        String tenantId = TenantContext.getCurrentTenantId();

        return () -> {
            Scheduler scheduler = Schedulers.boundedElastic();

            try {
                scheduler.schedule(() -> recordAgentConversationTurn(tenantId, extensions, context));
            } catch (RejectedExecutionException exception) {
                // Only reachable once the scheduler is shutting down. Dropping the turn keeps the "never block the
                // completion thread" invariant absolute; recording is best-effort by contract anyway.
                context.log(
                    log -> log.warn(
                        "Failed to schedule the agent conversation turn recorder for the AI Hub", exception));
            }
        };
    }

    /**
     * Reports a completed agent-chat turn to the optional, EE-only {@link AgentConversationRecorder} port so it can be
     * surfaced in the AI Hub (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}). Called once per completed turn
     * by each concrete action, at the point where it considers the turn done — never from a job-status listener, which
     * would have to recover the turn text from node output and would double-report for subflow children. Use
     * {@link #createAgentConversationTurnRecorder} instead when the completion point is on another thread.
     *
     * <p>
     * Skips entirely (no recorder interaction) when any of the following holds, all treated as "nothing to report"
     * rather than an error:
     * <ul>
     * <li>no {@code AgentConversationRecorder} bean is registered (CE, or the recorder-less constructor);</li>
     * <li>the run is an editor-environment test run — an Agent Studio test chat is not a conversation to archive;</li>
     * <li>{@code conversationId} cannot be resolved — chat memory is off for this agent, so there is no session and
     * therefore no Hub chat to attribute the turn to;</li>
     * <li>the {@link WorkflowExtConstants#AI_HUB_WORKSPACE_ID}/{@link WorkflowExtConstants#AI_HUB_AGENT_ID}/
     * {@link WorkflowExtConstants#AI_HUB_CREATOR_USER_ID} identity stamp {@code AiAgentWorkflowGenerator} writes onto
     * the node is wholly absent — a hand-built canvas {@code aiAgent} node (out of scope for this feature; see the
     * design spec's Scope section) never carries it.</li>
     * </ul>
     *
     * <p>
     * A <b>partial</b> stamp is a different state and is not silent: it means the generator ran but could not resolve
     * one of the three fields (an agent with no workspace, or a creator that no longer maps to a user), so Hub
     * visibility is dark for a reason ops needs to see. It is warned about once per action instance — enough to
     * diagnose, bounded so a misconfigured agent cannot flood the log on every turn.
     * </p>
     *
     * <p>
     * Fail-open: any exception raised while resolving the stamp or calling the recorder is logged and swallowed — this
     * method must never fail the agent's turn.
     * </p>
     */
    protected void recordAgentConversationTurn(Parameters extensions, ActionContext context) {
        recordAgentConversationTurn(TenantContext.getCurrentTenantId(), extensions, context);
    }

    private void recordAgentConversationTurn(String tenantId, Parameters extensions, ActionContext context) {
        if (agentConversationRecorderObjectProvider == null) {
            return;
        }

        try {
            TenantContext.runWithTenantId(tenantId, () -> doRecordAgentConversationTurn(extensions, context));
        } catch (Exception exception) {
            warnAboutRecordingFailure(exception, context);
        }
    }

    /**
     * Warns once per action instance about a recorder failure, then drops to debug.
     *
     * <p>
     * A failure here is usually permanent rather than transient: on a distributed EE deployment the recorder's
     * workspace-verification lookup ({@code RemoteProjectServiceClient#fetchWorkflowProject}) is an unimplemented stub
     * that throws {@code UnsupportedOperationException}, so an unbounded warn would repeat on <b>every</b> turn of
     * every agent, forever. Same bounding as {@link #warnAboutPartialIdentityStamp}: the first line is enough to
     * diagnose, and the rest stay available at debug.
     * </p>
     */
    private void warnAboutRecordingFailure(Exception exception, ActionContext context) {
        if (recordingFailureWarned.compareAndSet(false, true)) {
            context.log(
                log -> log.warn("Failed to record agent conversation turn for the AI Hub", exception));
        } else {
            context.log(
                log -> log.debug("Failed to record agent conversation turn for the AI Hub", exception));
        }
    }

    private void doRecordAgentConversationTurn(Parameters extensions, ActionContext context) {
        AgentConversationRecorder agentConversationRecorder = Objects
            .requireNonNull(agentConversationRecorderObjectProvider)
            .getIfAvailable();

        if (agentConversationRecorder == null) {
            return;
        }

        if (context.isEditorEnvironment()) {
            return;
        }

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        String conversationId = resolveConversationId(clusterElementMap);

        if (conversationId == null) {
            return;
        }

        Long workspaceId = extensions.getLong(WorkflowExtConstants.AI_HUB_WORKSPACE_ID);
        Long aiAgentId = extensions.getLong(WorkflowExtConstants.AI_HUB_AGENT_ID);
        Long creatorUserId = extensions.getLong(WorkflowExtConstants.AI_HUB_CREATOR_USER_ID);

        if (workspaceId == null && aiAgentId == null && creatorUserId == null) {
            return;
        }

        if (workspaceId == null || aiAgentId == null || creatorUserId == null) {
            warnAboutPartialIdentityStamp(workspaceId, aiAgentId, creatorUserId, context);

            return;
        }

        // The workflow id and environment come from the platform's execution context, NOT from the node definition,
        // so a hand-authored definition cannot forge them. The recorder needs the workflow id to verify the
        // (forgeable) workspace stamp above against the workflow's real owning workspace — see AgentConversation.
        String workflowId = null;
        Long environmentId = null;

        if (context instanceof ActionContextAware actionContextAware) {
            workflowId = actionContextAware.getWorkflowId();
            environmentId = actionContextAware.getEnvironmentId();
        }

        agentConversationRecorder.recordTurn(
            new AgentConversation(
                workspaceId, aiAgentId, creatorUserId, conversationId, null, null, workflowId, environmentId));
    }

    private void warnAboutPartialIdentityStamp(
        @Nullable Long workspaceId, @Nullable Long aiAgentId, @Nullable Long creatorUserId, ActionContext context) {

        if (!partialIdentityStampWarned.compareAndSet(false, true)) {
            return;
        }

        context.log(
            log -> log.warn(
                "AI Hub visibility is off for this agent: the identity stamp on the aiAgent node is partial, so no " +
                    "conversation turn can be reported. {}={}, {}={}, {}={} (a null field means the generator could " +
                    "not resolve it — an agent with no workspace, or a creator that no longer maps to a user).",
                WorkflowExtConstants.AI_HUB_WORKSPACE_ID, workspaceId, WorkflowExtConstants.AI_HUB_AGENT_ID, aiAgentId,
                WorkflowExtConstants.AI_HUB_CREATOR_USER_ID, creatorUserId));
    }

    private static Consumer<ChatClient.AdvisorSpec> getConversationAdvisor(@Nullable String conversationId) {
        return advisor -> {
            if (conversationId != null) {
                advisor.param(ChatMemory.CONVERSATION_ID, conversationId);

                // Session-based chat memory (SessionMemoryAdvisor) keys off its own context param; set it to the same
                // conversation id so the message-window and session memory implementations are interchangeable. The
                // literal mirrors SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY (spring-ai-session); kept as a literal to
                // avoid coupling the core agent module to that dependency.
                advisor.param("chat_memory_session_id", conversationId);
            }
        };
    }

    private Advisor getGuardrailsAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement, ActionContext context,
        List<Message> conversationHistory) {

        GuardrailsFunction guardrailsFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return guardrailsFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections, context,
                conversationHistory);
        } catch (Exception e) {
            throw clusterElementInitializationException(clusterElement, "guardrails", e, context);
        }
    }

    private Advisor getRagAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement,
        ActionContext context) {

        RagFunction ragFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return ragFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw clusterElementInitializationException(clusterElement, "RAG", e, context);
        }
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String getSimulatedResult(
        String toolInput, Map<String, String> simulation, ChatModel chatModel, ActionContext context) {

        String responsePrompt = simulation.get(RESPONSE_PROMPT);
        String simulationModel = simulation.get(SIMULATION_MODEL);

        try {
            ChatClient simulationClient = ChatClient.builder(chatModel)
                .build();

            String prompt =
                "Given this tool call input: %s\n\nGenerate a realistic response following these instructions: %s"
                    .formatted(toolInput, responsePrompt);

            ChatClient.ChatClientRequestSpec requestSpec = simulationClient.prompt()
                .user(prompt);

            if (simulationModel != null && !simulationModel.isEmpty()) {
                requestSpec = requestSpec.options(
                    ChatOptions.builder()
                        .model(simulationModel));
            }

            String generatedResponse = requestSpec.call()
                .content();

            return generatedResponse != null ? generatedResponse : TOOL_SIMULATION_UNAVAILABLE;
        } catch (Exception exception) {
            context.log(log -> log.warn("Failed to generate simulated response", exception));

            return TOOL_SIMULATION_UNAVAILABLE;
        }
    }

    private static List<ToolCallback> concatToolCallbacks(
        List<? extends ToolCallback> toolCallbacks, Optional<ChatMemoryFunction.Result> chatMemoryResult) {

        List<ToolCallback> combinedToolCallbacks = new ArrayList<>(toolCallbacks);

        chatMemoryResult
            .map(ChatMemoryFunction.Result::toolCallbacks)
            .ifPresent(memoryToolCallbacks -> {
                if (memoryToolCallbacks != null) {
                    combinedToolCallbacks.addAll(Arrays.asList(memoryToolCallbacks));
                }
            });

        return combinedToolCallbacks;
    }

    private List<ToolCallback> getToolCallbacks(
        List<ClusterElement> toolClusterElements, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, @Nullable ToolExecutionListener toolExecutionListener,
        @Nullable Map<String, Map<String, String>> toolSimulations, ChatModel chatModel, ActionContext context) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        // A TOOLS entry may arrive already wrapped: the approvalGateTool cluster element returns its children gated.
        // Simulation and observable wrapping below still run over the flattened list, so a gated callback stays
        // INSIDE the observable wrapper and the audit listener records the gate outcome like any other tool result.
        for (ClusterElement clusterElement : toolClusterElements) {
            toolCallbacks.addAll(
                buildElementToolCallbacks(clusterElement, connectionParameters, editorEnvironment, context));
        }

        if (toolSimulations != null && !toolSimulations.isEmpty()) {
            List<ToolCallback> simulatedCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : toolCallbacks) {
                simulatedCallbacks.add(
                    createSimulationAwareToolCallback(toolCallback, toolSimulations, chatModel, context));
            }

            toolCallbacks = simulatedCallbacks;
        }

        if (toolExecutionListener == null) {
            return toolCallbacks;
        }

        AtomicReference<@Nullable AgentThinking> thinkingReference = new AtomicReference<>();

        List<ToolCallback> observableToolCallbacks = toolCallbacks.stream()
            .map(
                toolCallback -> createObservableToolCallback(
                    toolCallback, thinkingReference, toolExecutionListener, context))
            .toList();

        AugmentedToolCallbackProvider<AgentThinking> augmentedToolCallbackProvider =
            AugmentedToolCallbackProvider.<AgentThinking>builder()
                .delegate(() -> observableToolCallbacks.toArray(ToolCallback[]::new))
                .argumentType(AgentThinking.class)
                .argumentConsumer(event -> thinkingReference.set(event.arguments()))
                .removeExtraArgumentsAfterProcessing(true)
                .build();

        return Arrays.asList(augmentedToolCallbackProvider.getToolCallbacks());
    }

    /**
     * Builds the raw (unwrapped) tool callbacks for a single TOOLS cluster-element entry. Used by
     * {@link #getToolCallbacks} before the gate/simulation/observable wrappers are applied, and by the gate-resume path
     * to execute an approved tool call directly — deliberately bypassing the approval gate, since the human already
     * approved this exact invocation.
     */
    private List<ToolCallback> buildElementToolCallbacks(
        ClusterElement clusterElement, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, ActionContext context) {

        return clusterElementToolCallbacks.build(clusterElement, connectionParameters, editorEnvironment, context);
    }

    private List<Message> loadConversationHistory(
        @Nullable ChatMemory chatMemory, ClusterElementMap clusterElementMap) {

        if (chatMemory == null) {
            return List.of();
        }

        return clusterElementMap.fetchClusterElement(CHAT_MEMORY)
            .map(clusterElement -> {
                Parameters chatMemoryParameters = ParametersFactory.create(clusterElement.getParameters());
                String conversationId = chatMemoryParameters.getString("conversationId");

                if (conversationId == null) {
                    return List.<Message>of();
                }

                try {
                    List<Message> all = chatMemory.get(conversationId);

                    if (all.isEmpty()) {
                        return List.<Message>of();
                    }

                    int size = all.size();

                    return size <= 3 ? List.copyOf(all) : List.copyOf(all.subList(size - 3, size));
                } catch (Exception exception) {
                    return List.<Message>of();
                }
            })
            .orElse(List.of());
    }
}
