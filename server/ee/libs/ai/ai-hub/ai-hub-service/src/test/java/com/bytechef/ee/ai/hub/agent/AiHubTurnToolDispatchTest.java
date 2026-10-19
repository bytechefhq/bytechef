/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.event.BaseEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.copilot.agent.CopilotChatClientResolver;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.AiHubAgentFixtures;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.web.rest.AiHubApiController;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolverImpl;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatModel;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatModelFactory;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelDTO;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs whole AI Hub turns through the production path: {@link AiHubApiController#chat} into {@link AiHubChatStreamer}
 * into the ASK and BUILD agents built by {@code AiHubConfiguration}, with a real {@link AiHubChatClientResolver} over
 * the real {@link CopilotChatClientResolver} and {@link CatalogChatClientResolverImpl}, and the {@code @Primary}
 * {@link CatalogChatModel} as the agents' builder-time model. The provider model at the bottom is a stub that records
 * every {@link Prompt} it is sent, so the tests check the tool definitions the LLM actually receives rather than the
 * agents' tool lists.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubTurnToolDispatchTest {

    private static final int ENVIRONMENT = 0;
    private static final long CHAT_ID = 100L;
    private static final String DEFAULT_MODEL = "claude-default";
    private static final String SELECTED_MODEL = "gpt-selected";
    private static final String THREAD_ID = "thread-tool-dispatch";
    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 7L;

    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final CatalogChatModelFactory catalogChatModelFactory = mock(CatalogChatModelFactory.class);
    private final AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();
    private final InFlightAiHubRunRegistry inFlightRunRegistry = fixtures.inFlightRunRegistry;
    private final PromptCapturingChatModel providerChatModel = new PromptCapturingChatModel();

    private AiHubApiController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws AGUIException {
        when(catalogChatModelFactory.createChatModel(any(), anyString(), any(), any())).thenReturn(providerChatModel);

        CatalogChatModel catalogChatModel = new CatalogChatModel(aiProviderFacade, catalogChatModelFactory);
        AiHubChatClientResolver chatClientResolver = new AiHubChatClientResolver(
            new CopilotChatClientResolver(
                new CatalogChatClientResolverImpl(catalogChatModelFactory, aiProviderFacade), ""));

        List<LocalAgent> agents = List.of(
            fixtures.askAgent(catalogChatModel, chatClientResolver),
            fixtures.buildAgent(catalogChatModel, chatClientResolver));

        AiHubChat chat = new AiHubChat(USER_ID);

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setWorkspaceId(WORKSPACE_ID);

        AiHubChatService chatService = mock(AiHubChatService.class);

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        AiHubChatAccessPolicy accessPolicy = mock(AiHubChatAccessPolicy.class);

        when(accessPolicy.canParticipate(chat, USER_ID)).thenReturn(true);

        User currentUser = mock(User.class);

        when(currentUser.getId()).thenReturn(USER_ID);

        UserService userService = mock(UserService.class);

        when(userService.getCurrentUser()).thenReturn(currentUser);

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(true);
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(true);

        ObjectProvider<PermissionService> permissionServiceProvider = mock(ObjectProvider.class);

        when(permissionServiceProvider.getIfAvailable()).thenReturn(permissionService);

        controller = new AiHubApiController(
            new AiHubChatStreamer(inFlightRunRegistry, new JsonMapper()), inFlightRunRegistry, agents, chatService,
            accessPolicy, userService, permissionServiceProvider, mock(AiHubToolApprovalService.class));
    }

    @Test
    void testBuildTurnWithoutASelectedModelSendsTheBuildToolsToTheEnvironmentDefaultModel() {
        activateDefaultProvider();

        List<BaseEvent> events = runTurn("BUILD", null);

        assertThat(events).noneMatch(RunErrorEvent.class::isInstance);
        assertThat(sentToolNames()).contains(
            "createProject", "createProjectWorkflow", "buildWorkflow", "openWorkflowTab", "deleteProject");
        verify(catalogChatModelFactory).createChatModel(Provider.ANTHROPIC, DEFAULT_MODEL, "anthropic-key", null);
    }

    @Test
    void testAskTurnWithoutASelectedModelSendsOnlyTheReadToolsToTheEnvironmentDefaultModel() {
        activateDefaultProvider();

        runTurn("ASK", null);

        assertThat(sentToolNames()).contains("listProjects", "openWorkflowTab")
            .doesNotContain("createProject", "createProjectWorkflow", "buildWorkflow", "deleteProject");
    }

    @Test
    void testBuildTurnWithASelectedModelSendsTheBuildToolsToTheSelectedModel() {
        activateDefaultProvider();

        when(aiProviderFacade.getApiKey(Provider.OPEN_AI.getKey(), ENVIRONMENT)).thenReturn("open-ai-key");

        List<BaseEvent> events = runTurn("BUILD", SELECTED_MODEL);

        assertThat(events).noneMatch(RunErrorEvent.class::isInstance);
        assertThat(sentToolNames()).contains(
            "createProject", "createProjectWorkflow", "buildWorkflow", "openWorkflowTab", "deleteProject");
        verify(catalogChatModelFactory).createChatModel(Provider.OPEN_AI, SELECTED_MODEL, "open-ai-key", null);
    }

    @Test
    void testAskTurnWithASelectedModelSendsOnlyTheReadToolsToTheSelectedModel() {
        when(aiProviderFacade.getApiKey(Provider.OPEN_AI.getKey(), ENVIRONMENT)).thenReturn("open-ai-key");

        runTurn("ASK", SELECTED_MODEL);

        assertThat(sentToolNames()).contains("listProjects", "openWorkflowTab")
            .doesNotContain("createProject", "createProjectWorkflow", "buildWorkflow", "deleteProject");
    }

    @Test
    void testTurnWithoutAnActivatedProviderEndsWithARunErrorInsteadOfAToollessModel() {
        List<BaseEvent> events = runTurn("BUILD", null);

        assertThat(events).filteredOn(RunErrorEvent.class::isInstance)
            .singleElement()
            .extracting(event -> ((RunErrorEvent) event).getError())
            .asString()
            .contains("No AI provider is enabled in environment " + ENVIRONMENT);
        assertThat(providerChatModel.prompts).isEmpty();
        verify(catalogChatModelFactory, never()).createChatModel(any(), anyString(), any(), any());
    }

    @Test
    void testTurnWithoutAnActivatedProviderCodesItsRunErrorNoAiProvider() {
        List<BaseEvent> events = runTurn("BUILD", null);

        assertThat(events).filteredOn(RunErrorEvent.class::isInstance)
            .singleElement()
            .extracting(event -> ((RunErrorEvent) event).getCode())
            .isEqualTo("NO_AI_PROVIDER");
    }

    @Test
    void testTurnFailingForAnUnrelatedReasonEmitsARunErrorWithoutACode() {
        activateDefaultProvider();

        providerChatModel.streamFailure = new IllegalStateException("Provider outage");

        List<BaseEvent> events = runTurn("BUILD", null);

        assertThat(events).filteredOn(RunErrorEvent.class::isInstance)
            .singleElement()
            .extracting(event -> ((RunErrorEvent) event).getCode())
            .isNull();
    }

    @Test
    void testTurnWithAnUnusableSelectedModelEndsWithARunErrorInsteadOfTheEnvironmentDefault() {
        activateDefaultProvider();

        List<BaseEvent> events = runTurn("BUILD", SELECTED_MODEL);

        assertThat(events).filteredOn(RunErrorEvent.class::isInstance)
            .singleElement()
            .extracting(event -> ((RunErrorEvent) event).getError())
            .asString()
            .contains(
                "The selected model " + SELECTED_MODEL + " (" + Provider.OPEN_AI.getKey() +
                    ") is not available in environment " + ENVIRONMENT);
        assertThat(providerChatModel.prompts).isEmpty();
        verify(catalogChatModelFactory, never()).createChatModel(any(), anyString(), any(), any());
    }

    @Test
    void testAStopMidStreamLeavesOnlyTheUserRowInTheSession() throws InterruptedException {
        activateDefaultProvider();

        Sinks.Many<ChatResponse> modelStream = Sinks.many()
            .unicast()
            .onBackpressureBuffer();

        providerChatModel.streamOverride = modelStream.asFlux();

        startTurn("BUILD", null);

        modelStream.tryEmitNext(textChunk("Building the"));

        inFlightRunRegistry.attach(THREAD_ID)
            .orElseThrow()
            .filter(TextMessageContentEvent.class::isInstance)
            .blockFirst(Duration.ofSeconds(10));

        inFlightRunRegistry.cancel(THREAD_ID);

        modelStream.tryEmitNext(textChunk(" workflow."));
        modelStream.tryEmitComplete();

        for (int attempt = 0; attempt < 100 && inFlightRunRegistry.isInFlight(THREAD_ID); attempt++) {
            Thread.sleep(50);
        }

        assertThat(inFlightRunRegistry.isInFlight(THREAD_ID)).isFalse();
        assertThat(fixtures.sessionMemory.sessionService()
            .getEvents(THREAD_ID, EventFilter.all())).extracting(SessionEvent::getMessageType)
                .containsExactly(MessageType.USER);
    }

    private static ChatResponse textChunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private void activateDefaultProvider() {
        when(aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT))
            .thenReturn(new AiDefaultModelDTO(Provider.ANTHROPIC.getKey(), DEFAULT_MODEL));
        when(aiProviderFacade.getAiDefaultChatModelApiKey(anyInt()))
            .thenReturn(new AiDefaultModelWithApiKeyDTO(Provider.ANTHROPIC, DEFAULT_MODEL, "anthropic-key", null));
        when(aiProviderFacade.getApiKey(Provider.ANTHROPIC.getKey(), ENVIRONMENT)).thenReturn("anthropic-key");
    }

    private List<BaseEvent> runTurn(String mode, @Nullable String selectedModel) {
        startTurn(mode, selectedModel);

        Flux<BaseEvent> runEvents = inFlightRunRegistry.attach(THREAD_ID)
            .orElseThrow();

        return runEvents.collectList()
            .block(Duration.ofSeconds(30));
    }

    private void startTurn(String mode, @Nullable String selectedModel) {
        State state = new State();

        state.set("mode", mode);
        state.set(AiHubStateKeys.WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.ENVIRONMENT_ID, ENVIRONMENT);

        if (selectedModel != null) {
            state.set(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY, Provider.OPEN_AI.getKey());
            state.set(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY, selectedModel);
        }

        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("Create a project that posts a daily Slack message");

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        AgUiParameters agUiParameters = new AgUiParameters();

        agUiParameters.setMessages(messages);
        agUiParameters.setRunId("run-1");
        agUiParameters.setState(state);
        agUiParameters.setThreadId(THREAD_ID);

        controller.chat(agUiParameters);
    }

    private List<String> sentToolNames() {
        assertThat(providerChatModel.prompts).hasSize(1);

        ChatOptions chatOptions = providerChatModel.prompts.getFirst()
            .getOptions();

        assertThat(chatOptions).isInstanceOf(ToolCallingChatOptions.class);

        List<ToolCallback> toolCallbacks = Objects.requireNonNull(
            ((ToolCallingChatOptions) chatOptions).getToolCallbacks(), "The prompt carries no tool callbacks");

        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    private static final class PromptCapturingChatModel implements ChatModel {

        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

        private volatile @Nullable RuntimeException streamFailure;
        private volatile @Nullable Flux<ChatResponse> streamOverride;

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return chatResponse();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            prompts.add(prompt);

            RuntimeException failure = streamFailure;

            if (failure != null) {
                return Flux.error(failure);
            }

            Flux<ChatResponse> override = streamOverride;

            if (override != null) {
                return override;
            }

            return Flux.just(chatResponse());
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }

        private static ChatResponse chatResponse() {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Done."))));
        }
    }
}
