/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ee.automation.ai.copilot.agent.CodeWorkflowSpringAIAgent;
import com.bytechef.ee.automation.ai.copilot.agent.CustomComponentSpringAIAgent;
import com.bytechef.ee.automation.ai.tool.CodeWorkflowTools;
import com.bytechef.ee.automation.ai.tool.CustomComponentTools;
import com.bytechef.ee.automation.ai.tool.ReadCodeWorkflowTools;
import com.bytechef.ee.automation.ai.tool.ReadCustomComponentTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the EE automation Copilot agents for the code-workflow and custom-component domains — the EE counterpart of
 * the CE {@code CopilotConfiguration}. Per domain this covers up to three surfaces: the in-editor Copilot panel source
 * agents ({@code code_workflow_ask}/{@code code_workflow_build} — custom components have no panel agents yet), and the
 * one-shot subagent {@link ChatClient} beans consumed by the ai_hub agents (via {@code CodeWorkflowAgentToolCallback} /
 * {@code CustomComponentAgentToolCallback}) and the management MCP server. {@link ContextStoreAgentConfiguration} stays
 * separate because its activation additionally requires the {@code bytechef.context-store.enabled} feature toggle.
 *
 * <p>
 * The panel agents join the same {@code List<LocalAgent>} the EE {@code CopilotApiController} resolves by agentId, so
 * the in-editor client reaches them through the ordinary {@code /ai/chat/{agentId}} route, exactly like the embedded
 * twin registered by {@code EmbeddedCopilotConfiguration}. Their tool callbacks are wrapped with
 * {@link RehydrateContextToolCallback} so the caller's tenant/security context is re-established on the
 * {@code boundedElastic} worker thread that executes the tool call — required because the backing facade methods are
 * {@code @PreAuthorize("hasAuthority(ADMIN)")}-gated. The subagent chat clients are one-shot (no chat memory, no
 * streaming). Each mode shares ONE prompt across both surfaces (panel and subagent), like the other domains; the
 * prompts deliberately omit open-tab tools — opening resources in the UI is owned by the caller.
 * </p>
 *
 * <p>
 * ASK is read-only ({@code Read*Tools} only) on every surface; BUILD additionally owns the mutating tool set. This
 * configuration lives here (not in {@code automation-ai-tool}, which holds only {@code @Tool} classes, and not in
 * ai-hub, which contributes only its own agents) because the tools are EE while the CE {@code CopilotConfiguration}
 * hosts the CE domains.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class AutomationCopilotConfiguration {

    // Read once at configuration init rather than per delegation: the *SubAgentChatClientFactory beans below
    // rebuild their ChatClient on every delegation (to honour a caller-picked ChatModel), and readPrompt(Resource)
    // performs I/O.
    private final String codeWorkflowAskSystemPrompt;
    private final String codeWorkflowBuildSystemPrompt;
    private final String customComponentAskSystemPrompt;
    private final String customComponentBuildSystemPrompt;

    private final State state = new State();

    // RUBY-DISABLED: the prompt resources loaded below had every Ruby reference DELETED, not commented out.
    // readPrompt() below is a verbatim readAllBytes with no comment stripping, so the whole file becomes the
    // system message — a commented-out Ruby section would still be read by the model as content and it would
    // keep offering a language that org.graalvm.polyglot:ruby (stuck at 25.0.0, crashes on the pinned Truffle
    // 25.2.4) can no longer run. The marker therefore lives here, in code the model never sees.
    // Affected: prompt_code_workflow_build.txt (supported-language line, the whole '### Ruby' contract section,
    // and the createCodeWorkflow language list), prompt_code_workflow_ask.txt and prompt_custom_component_ask.txt
    // (supported-language lines). Once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is
    // downgraded), restore in each file: the supported-language sentence, the language enumeration in the tool
    // description, and the '### Ruby' contract section (mirror the '### Python' section; the removed text is in
    // git history at this commit). Grep RUBY-DISABLED.
    // CT_CONSTRUCTOR_THROW: the constructor reads and validates the prompt resources up front (see the hoisted
    // *SystemPrompt fields above) so readPrompt's IllegalStateException surfaces at startup, not on the first
    // delegation; Spring never subclasses this @Configuration in a way that could observe partially-initialized state.
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    AutomationCopilotConfiguration(
        @Value("classpath:prompt_code_workflow_ask.txt") Resource promptCodeWorkflowAskResource,
        @Value("classpath:prompt_code_workflow_build.txt") Resource promptCodeWorkflowBuildResource,
        @Value("classpath:prompt_custom_component_ask.txt") Resource promptCustomComponentAskResource,
        @Value("classpath:prompt_custom_component_build.txt") Resource promptCustomComponentBuildResource) {

        this.codeWorkflowAskSystemPrompt = readPrompt(promptCodeWorkflowAskResource);
        this.codeWorkflowBuildSystemPrompt = readPrompt(promptCodeWorkflowBuildResource);
        this.customComponentAskSystemPrompt = readPrompt(promptCustomComponentAskResource);
        this.customComponentBuildSystemPrompt = readPrompt(promptCustomComponentBuildResource);
    }

    @Bean
    CodeWorkflowSpringAIAgent codeWorkflowAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadCodeWorkflowTools readCodeWorkflowTools,
        SecurityContextRehydrator securityContextRehydrator) throws AGUIException {

        return CodeWorkflowSpringAIAgent.builder()
            .agentId("code_workflow_ask")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(codeWorkflowAskSystemPrompt)
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of(readCodeWorkflowTools)))
            .build();
    }

    @Bean
    CodeWorkflowSpringAIAgent codeWorkflowBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, CodeWorkflowTools codeWorkflowTools,
        ReadCodeWorkflowTools readCodeWorkflowTools, SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        return CodeWorkflowSpringAIAgent.builder()
            .agentId("code_workflow_build")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(codeWorkflowBuildSystemPrompt)
            .state(state)
            .toolCallbacks(
                wrapTools(securityContextRehydrator, List.of(codeWorkflowTools, readCodeWorkflowTools)))
            .build();
    }

    @Bean
    ChatClient codeWorkflowAskSubAgentChatClient(ChatModel chatModel, ReadCodeWorkflowTools readCodeWorkflowTools) {
        return buildCodeWorkflowAskSubAgentChatClient(chatModel, readCodeWorkflowTools);
    }

    @Bean
    IntelligentToolChatClientFactory codeWorkflowAskSubAgentChatClientFactory(
        @Qualifier("codeWorkflowAskSubAgentChatClient") ChatClient codeWorkflowAskSubAgentChatClient,
        ReadCodeWorkflowTools readCodeWorkflowTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeWorkflowAskSubAgentChatClient
            : buildCodeWorkflowAskSubAgentChatClient(candidateChatModel, readCodeWorkflowTools);
    }

    private ChatClient buildCodeWorkflowAskSubAgentChatClient(
        ChatModel chatModel, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(codeWorkflowAskSystemPrompt)
            .defaultTools(readCodeWorkflowTools)
            .build();
    }

    @Bean
    ChatClient codeWorkflowBuildSubAgentChatClient(
        ChatModel chatModel, CodeWorkflowTools codeWorkflowTools, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return buildCodeWorkflowBuildSubAgentChatClient(chatModel, codeWorkflowTools, readCodeWorkflowTools);
    }

    @Bean
    IntelligentToolChatClientFactory codeWorkflowBuildSubAgentChatClientFactory(
        @Qualifier("codeWorkflowBuildSubAgentChatClient") ChatClient codeWorkflowBuildSubAgentChatClient,
        CodeWorkflowTools codeWorkflowTools, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeWorkflowBuildSubAgentChatClient
            : buildCodeWorkflowBuildSubAgentChatClient(candidateChatModel, codeWorkflowTools, readCodeWorkflowTools);
    }

    private ChatClient buildCodeWorkflowBuildSubAgentChatClient(
        ChatModel chatModel, CodeWorkflowTools codeWorkflowTools, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(codeWorkflowBuildSystemPrompt)
            .defaultTools(codeWorkflowTools, readCodeWorkflowTools)
            .build();
    }

    @Bean
    CustomComponentSpringAIAgent customComponentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools,
        SecurityContextRehydrator securityContextRehydrator) throws AGUIException {

        return CustomComponentSpringAIAgent.builder()
            .agentId("custom_component_ask")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(customComponentAskSystemPrompt)
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of(readCustomComponentTools)))
            .build();
    }

    @Bean
    CustomComponentSpringAIAgent customComponentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools, SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        return CustomComponentSpringAIAgent.builder()
            .agentId("custom_component_build")
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(customComponentBuildSystemPrompt)
            .state(state)
            .toolCallbacks(
                wrapTools(securityContextRehydrator, List.of(customComponentTools, readCustomComponentTools)))
            .build();
    }

    @Bean
    ChatClient customComponentAskSubAgentChatClient(
        ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools) {

        return buildCustomComponentAskSubAgentChatClient(chatModel, readCustomComponentTools);
    }

    @Bean
    IntelligentToolChatClientFactory customComponentAskSubAgentChatClientFactory(
        @Qualifier("customComponentAskSubAgentChatClient") ChatClient customComponentAskSubAgentChatClient,
        ReadCustomComponentTools readCustomComponentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? customComponentAskSubAgentChatClient
            : buildCustomComponentAskSubAgentChatClient(candidateChatModel, readCustomComponentTools);
    }

    private ChatClient buildCustomComponentAskSubAgentChatClient(
        ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(customComponentAskSystemPrompt)
            .defaultTools(readCustomComponentTools)
            .build();
    }

    @Bean
    ChatClient customComponentBuildSubAgentChatClient(
        ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools) {

        return buildCustomComponentBuildSubAgentChatClient(chatModel, customComponentTools, readCustomComponentTools);
    }

    @Bean
    IntelligentToolChatClientFactory customComponentBuildSubAgentChatClientFactory(
        @Qualifier("customComponentBuildSubAgentChatClient") ChatClient customComponentBuildSubAgentChatClient,
        CustomComponentTools customComponentTools, ReadCustomComponentTools readCustomComponentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? customComponentBuildSubAgentChatClient
            : buildCustomComponentBuildSubAgentChatClient(
                candidateChatModel, customComponentTools, readCustomComponentTools);
    }

    private ChatClient buildCustomComponentBuildSubAgentChatClient(
        ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(customComponentBuildSystemPrompt)
            .defaultTools(customComponentTools, readCustomComponentTools)
            .build();
    }

    /**
     * Local equivalent of the private {@code CopilotConfiguration.wrapTools}: wraps each tool's callbacks in a
     * {@link RehydrateContextToolCallback} so the caller's tenant/security context is re-established on the worker
     * thread that executes the tool.
     */
    private List<ToolCallback> wrapTools(SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : tools) {
            if (tool instanceof ToolCallback toolCallback) {
                toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
            } else {
                for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                    toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
                }
            }
        }

        return toolCallbacks;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read copilot agent prompt resource: " + resource.getDescription(), exception);
        }
    }
}
