package com.agui.spring.ai;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.BaseEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.function.FunctionCall;
import com.agui.core.message.*;
import com.agui.core.state.State;
import com.agui.core.tool.ToolCall;
import com.agui.server.LocalAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.MemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.agui.server.EventFactory.*;
import static java.util.stream.Collectors.toList;

/**
 * A concrete implementation of {@link LocalAgent} that integrates with Spring AI framework
 * to provide AI-powered agent capabilities.
 *
 * This agent leverages Spring AI's ChatClient to process messages and interact with
 * various chat models. It supports tools, advisors, chat memory, and streaming responses.
 * The agent handles the complete lifecycle of chat interactions including tool calls,
 * memory management, and event emission for real-time updates.
 *
 * Key features:
 * <ul>
 * <li>Integration with Spring AI ChatClient and ChatModel</li>
 * <li>Support for tool callbacks and function calling</li>
 * <li>Chat memory management for conversation persistence</li>
 * <li>Advisor pattern support for extending functionality</li>
 * <li>Streaming response handling with real-time events</li>
 * <li>Automatic tool mapping from AG-UI tools to Spring AI tools</li>
 * </ul>
 *
 * @author Pascal Wilbrink
 * @since 1.0
 */
public class SpringAIAgent extends LocalAgent {

    private static final Logger log = LoggerFactory.getLogger(SpringAIAgent.class);

    /**
     * The Spring AI ChatClient used for processing chat requests and responses.
     */
    private final ChatClient chatClient;

    /**
     * Mapper utility for converting AG-UI tools to Spring AI ToolCallback instances.
     */
    private final ToolMapper toolMapper;

    /**
     * List of Spring AI advisors that modify or enhance chat behavior.
     */
    private final List<Advisor> advisors;

    /**
     * List of Spring AI tool callbacks for function calling capabilities.
     */
    private final List<ToolCallback> toolCallbacks;

    /**
     * Chat memory implementation for maintaining conversation history.
     */
    private final ChatMemory chatMemory;

    /**
     * List of Spring AI tools
     */
    private final List<Object> tools;

    /**
     * Protected constructor that initializes the SpringAIAgent using the builder pattern.
     *
     * @param builder the Builder instance containing all configuration parameters
     * @throws AGUIException if the parent LocalAgent constructor validation fails
     */
    protected SpringAIAgent(
        Builder builder
    ) throws AGUIException {
        super(
            builder.agentId,
            builder.state,
            builder.systemMessageProvider,
            builder.systemMessage,
            builder.messages
        );

        this.chatClient = ChatClient.builder(builder.chatModel).build();

        this.chatMemory = builder.chatMemory;

        this.advisors = builder.advisors;

        this.toolCallbacks = builder.toolCallbacks;
        this.tools = builder.tools;

        this.toolMapper = new ToolMapper();
    }

    /**
     * {@inheritDoc}
     *
     * Executes the agent by processing the latest user message through Spring AI's ChatClient.
     * The method handles the complete chat lifecycle including:
     * <ul>
     * <li>Extracting the user message from input</li>
     * <li>Setting up the chat request with tools, advisors, and memory</li>
     * <li>Streaming the response and emitting appropriate events</li>
     * <li>Handling tool calls and deferred events</li>
     * <li>Managing conversation memory</li>
     * </ul>
     *
     * Events are emitted throughout the process to provide real-time updates to subscribers.
     */
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        var messageId = UUID.randomUUID().toString();
        var threadId = input.threadId();
        var runId = input.runId();
        var state = input.state();

        input.messages().forEach((message) -> {
            if (messages.stream().filter((m) -> m.equals(message)).findAny().isEmpty()) {
                messages.add(message);
            }
        });

        String content;

        try {
            var userMessage = this.getLatestUserMessage(messages);
            content = userMessage.getContent();
        } catch (AGUIException e) {
            log.error("Agent '{}' could not resolve the latest user message for threadId={} runId={}",
                this.agentId, threadId, runId, e);

            this.emitEvent(runStartedEvent(threadId, runId), subscriber);
            this.onError(input, e, subscriber);
            return;
        }

        this.emitEvent(
            runStartedEvent(threadId, runId),
            subscriber
        );

        this.emitEvent(
            textMessageStartEvent(messageId, "assistant"),
            subscriber
        );

        final List<BaseEvent> deferredEvents = new ArrayList<>();

        var textMessageStream = new TextMessageStream(messageId, subscriber);

        Consumer<BaseEvent> toolEventConsumer =
            this.splitsTextAtToolCalls() ? textMessageStream::emitToolEvent : deferredEvents::add;

        var assistantMessage = new AssistantMessage();
        assistantMessage.setId(messageId);
        assistantMessage.setName(this.agentId);

        try {
            getChatRequest(input, content, textMessageStream, toolEventConsumer, this.createSystemMessage(state, input.context()), subscriber)
                .stream()
                .chatResponse()
                .contextCapture()
                .subscribe(
                    evt -> onEvent(subscriber, evt, assistantMessage, textMessageStream),
                    err -> {
                        log.error(
                            "Agent '{}' chat stream failed for threadId={} runId={}: {}",
                            this.agentId, threadId, runId, err.toString(), err);

                        this.onError(input, err, subscriber);
                    },
                    () -> onComplete(input, assistantMessage, subscriber, textMessageStream, deferredEvents)
                );
        } catch (AGUIException | RuntimeException e) {
            log.error("Agent '{}' failed to start chat stream for threadId={} runId={}", this.agentId, threadId, runId, e);

            this.onError(input, e, subscriber);
        }
    }

    private void onError(RunAgentInput input, Throwable throwable, AgentSubscriber subscriber) {
        try {
            this.emitEvent(runErrorEvent(this.runErrorMessage(input, throwable)), subscriber);
        } finally {
            subscriber.onRunFinalized(new AgentSubscriberParams(input.messages(), state, this, input));
        }
    }

    /**
     * Hook for subclasses to choose the message a RUN_ERROR event carries to the client. Default returns the
     * throwable's message.
     *
     * @param input     the current run input
     * @param throwable the failure that ended the run
     * @return the message sent to the client
     */
    protected String runErrorMessage(RunAgentInput input, Throwable throwable) {
        return throwable.getMessage();
    }

    /**
     * Hook for subclasses to choose the chat memory conversation id. Default returns the run's thread id.
     *
     * @param input the current run input
     * @return the conversation id chat memory is keyed by
     */
    protected String conversationId(RunAgentInput input) {
        return input.threadId();
    }

    protected boolean endsTurn(ToolResponseMessage.ToolResponse toolResponse) {
        return false;
    }

    protected boolean splitsTextAtToolCalls() {
        return false;
    }

    protected void prepareChatMemory(RunAgentInput input, ChatMemory chatMemory, String conversationId) {
    }

    /**
     * Hook for subclasses to contribute per-invocation values into Spring AI's
     * {@link org.springframework.ai.chat.model.ToolContext}. The returned map is merged into the request via
     * {@code ChatClient.ChatClientRequestSpec.toolContext(Map)} and reaches every {@link ToolCallback#call(String,
     * org.springframework.ai.chat.model.ToolContext) toolCallback.call} invocation regardless of which thread eventually
     * executes it. Default returns an empty map (no-op).
     *
     * @param input the current run input
     * @return the per-invocation ToolContext entries (never {@code null})
     */
    protected Map<String, Object> toolContext(RunAgentInput input) {
        return Collections.emptyMap();
    }

    /**
     * Hook for subclasses to contribute per-invocation {@link ToolCallback} instances on top of the static set
     * configured at builder time. The returned list is concatenated with the builder-supplied
     * {@link #toolCallbacks} before {@code ChatClient.ChatClientRequestSpec.toolCallbacks(...)} is called, so
     * the LLM sees union(static, dynamic) for this turn only. Default returns an empty list (no-op).
     *
     * @param input the current run input
     * @return additional per-invocation tool callbacks (never {@code null})
     */
    protected List<ToolCallback> additionalToolCallbacks(RunAgentInput input) {
        return Collections.emptyList();
    }

    protected Map<String, Object> advisorParams(RunAgentInput input) {
        return Collections.emptyMap();
    }

    /**
     * Handles individual chat response events from the streaming response.
     *
     * This method processes each chunk of the streaming response and emits
     * text message content events when the response contains actual text content.
     *
     * @param subscriber the event subscriber to notify
     * @param evt the chat response event from Spring AI
     * @param textMessageStream the run's text message stream the content is emitted to
     */
    private void onEvent(AgentSubscriber subscriber, ChatResponse evt, AssistantMessage assistantMessage, TextMessageStream textMessageStream) {
        if (evt.getResult() == null) {
            return;
        }

        if (evt.hasToolCalls()) {
            assistantMessage.setToolCalls(new ArrayList<>());
            evt.getResult().getOutput().getToolCalls()
                .forEach(toolCall -> {
                    var call = new ToolCall(toolCall.id(), "function", new FunctionCall(toolCall.name(), toolCall.arguments()));

                    assistantMessage.getToolCalls().add(call);

                    subscriber.onNewToolCall(call);
                });
        }

        if (ToolExecutionResult.FINISH_REASON.equals(evt.getResult().getMetadata().getFinishReason())) {
            return;
        }

        var content = evt.getResult().getOutput().getText();

        if (content != null && !content.isEmpty()) {
            textMessageStream.emitContent(content);

            assistantMessage.setContent(
                assistantMessage.getContent() + content
            );
        }
    }

    /**
     * Handles the completion of the chat response stream.
     * This method is called when the streaming response is complete and handles:
     * <ul>
     * <li>Emitting the text message end event</li>
     * <li>Processing any deferred tool call events</li>
     * <li>Emitting the run finished event</li>
     * <li>Finalizing the agent run with updated state</li>
     * </ul>
     *
     * @param input the original run input parameters
     * @param subscriber the event subscriber to notify
     * @param textMessageStream the run's text message stream, ended here if it is still open
     * @param deferredEvents list of tool call events to process after message completion
     */
    private void onComplete(RunAgentInput input, AssistantMessage assistantMessage, AgentSubscriber subscriber, TextMessageStream textMessageStream, List<BaseEvent> deferredEvents) {
        textMessageStream.end();

        deferredEvents.forEach(deferredEvent -> {
            this.emitEvent(deferredEvent, subscriber);

        });

        subscriber.onNewMessage(assistantMessage);

        this.emitEvent(runFinishedEvent(input.threadId(), input.runId()), subscriber);
        subscriber.onRunFinalized(new AgentSubscriberParams(input.messages(), state, this, input));
    }

    /**
     * Hook for subclasses to select a different ChatClient per request based on input state.
     *
     * <p>Default returns the builder-time client (constructed from {@link Builder#chatModel(ChatModel)}). Subclasses
     * that need per-request model resolution — e.g. a host application that picks the model based on a per-agent or
     * per-user setting carried in {@link RunAgentInput#state()} — can override this method to return a freshly built
     * client over the resolved {@link ChatModel}. Implementations should cache the resolved client where appropriate;
     * the default {@link ChatClient.Builder#build()} is cheap but not free.
     *
     * <p>Backward-compat note: every existing call site of {@code this.chatClient} in this class routes through this
     * method, so non-overriding subclasses see exactly the same behaviour as before this hook existed.
     *
     * @param input the current run input (state, messages, tools, context)
     * @return the {@link ChatClient} to use for this request; must not be {@code null}
     */
    protected ChatClient resolveChatClient(RunAgentInput input) {
        return this.chatClient;
    }

    /**
     * Constructs and configures the Spring AI ChatClient request specification.
     *
     * This method builds a complete chat request by combining:
     * <ul>
     * <li>The user message content and system message</li>
     * <li>Available tools converted to Spring AI ToolCallbacks</li>
     * <li>Configured advisors for behavior modification</li>
     * <li>Chat memory for conversation persistence</li>
     * </ul>
     *
     * @param input the run input containing messages, tools, and context
     * @param content the user message content to send
     * @param textMessageStream the run's text message stream, whose current message parents the tool call events
     * @param toolEventConsumer receives the tool call events, either deferred or emitted as each tool runs
     * @param systemMessage the formatted system message including state and context
     * @return configured ChatClient request specification ready for execution
     */
    private ChatClient.ChatClientRequestSpec getChatRequest(RunAgentInput input, String content, TextMessageStream textMessageStream, Consumer<BaseEvent> toolEventConsumer, SystemMessage systemMessage, AgentSubscriber subscriber) throws AGUIException {
        ChatClient.ChatClientRequestSpec chatRequest = resolveChatClient(input).prompt(
            Prompt
                .builder()
                .content(content)
                .build()
            )
            .system(systemMessage.getContent()
        );

        if (!this.tools.isEmpty()) {
            try {
                chatRequest = chatRequest.tools(this.tools.toArray(new Object[0]));
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add tools", e);
            }
        }

        if (!input.tools().isEmpty()) {
            try {
                chatRequest = chatRequest.toolCallbacks(
                    input.tools()
                        .stream()
                        .map((tool) -> this.toolMapper.toSpringTool(
                            tool,
                            textMessageStream.messageId(),
                            toolEventConsumer
                        )).toList()
                );
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add Tools", e);
            }
        }

        List<ToolCallback> dynamicCallbacks = this.additionalToolCallbacks(input);
        List<ToolCallback> mergedCallbacks = new ArrayList<>(this.toolCallbacks);
        if (dynamicCallbacks != null && !dynamicCallbacks.isEmpty()) {
            mergedCallbacks.addAll(dynamicCallbacks);
        }

        if (!mergedCallbacks.isEmpty()) {
            try {
                chatRequest = chatRequest.toolCallbacks(
                    mergedCallbacks
                        .stream()
                        .map(toolCallback -> new AgUiFunctionToolCallback(toolCallback, (AgUiToolCallbackParams params) -> {
                            var toolCallId = UUID.randomUUID().toString();
                            var parentMessageId = textMessageStream.messageId();
                            toolEventConsumer.accept(toolCallStartEvent(parentMessageId, toolCallback.getToolDefinition().name(), toolCallId));
                            toolEventConsumer.accept(toolCallArgsEvent(params.arguments(), toolCallId));
                            toolEventConsumer.accept(toolCallEndEvent(toolCallId));
                            toolEventConsumer.accept(toolCallResultEvent(toolCallId, params.result(), parentMessageId, Role.tool));

                        }))
                        .collect(toList())
                );
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add Tool Callbacks", e);
            }
        }

        if (!this.advisors.isEmpty()) {
            try {
                chatRequest = chatRequest.advisors(this.advisors);
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add advisors", e);
            }
        }

        Map<String, Object> advisorParameters = this.advisorParams(input);

        if (advisorParameters != null && !advisorParameters.isEmpty()) {
            try {
                chatRequest = chatRequest.advisors(advisorSpec -> advisorSpec.params(advisorParameters));
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add advisor params", e);
            }
        }

        try {
            chatRequest = chatRequest.advisors(createToolCallingAdvisor());
        } catch (RuntimeException e) {
            throw new AGUIException("Could not add the tool calling advisor", e);
        }

        if (Objects.nonNull(this.chatMemory)) {
            try {
                this.prepareChatMemory(input, this.chatMemory, this.conversationId(input));

                chatRequest.advisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build()
                );

                chatRequest.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, this.conversationId(input)));
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add chat memory", e);
            }
        }

        Map<String, Object> toolContext = this.toolContext(input);

        if (toolContext != null && !toolContext.isEmpty()) {
            try {
                chatRequest = chatRequest.toolContext(toolContext);
            } catch (RuntimeException e) {
                throw new AGUIException("Could not add tool context", e);
            }
        }

        return chatRequest;
    }

    private ToolCallingAdvisor createToolCallingAdvisor() {
        boolean hasDownstreamMemoryAdvisor = (Objects.nonNull(this.chatMemory)
            && Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER > ToolCallingAdvisor.DEFAULT_ORDER)
            || this.advisors.stream()
                .anyMatch(advisor -> advisor instanceof MemoryAdvisor
                    && advisor.getOrder() > ToolCallingAdvisor.DEFAULT_ORDER);

        return ToolCallingAdvisor.builder()
            .toolCallingManager(
                new TurnEndingToolCallingManager(ToolCallingManager.builder().build(), this::endsTurn))
            .conversationHistoryEnabled(!hasDownstreamMemoryAdvisor)
            .build();
    }

    private final class TextMessageStream {

        private final AgentSubscriber subscriber;
        private String messageId;
        private boolean open = true;

        private TextMessageStream(String messageId, AgentSubscriber subscriber) {
            this.messageId = messageId;
            this.subscriber = subscriber;
        }

        private synchronized String messageId() {
            return messageId;
        }

        private synchronized void emitContent(String content) {
            if (!open) {
                messageId = UUID.randomUUID().toString();
                open = true;

                emitEvent(textMessageStartEvent(messageId, "assistant"), subscriber);
            }

            emitEvent(textMessageContentEvent(messageId, content), subscriber);
        }

        private synchronized void emitToolEvent(BaseEvent toolEvent) {
            end();

            emitEvent(toolEvent, subscriber);
        }

        private synchronized void end() {
            if (open) {
                open = false;

                emitEvent(textMessageEndEvent(messageId), subscriber);
            }
        }
    }

    /**
     * Creates a new Builder instance for constructing SpringAIAgent instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing SpringAIAgent instances using the builder pattern.
     *
     * This builder provides a fluent API for configuring all aspects of the SpringAIAgent
     * including the chat model, advisors, tools, memory, and agent-specific settings.
     * The builder validates that required components are provided before creating the agent.
     */
    public static class Builder {

        /**
         * The Spring AI ChatModel to use for processing chat requests.
         */
        private ChatModel chatModel;

        /**
         * List of Spring AI advisors to apply to chat requests.
         */
        private final List<Advisor> advisors = new ArrayList<>();

        /**
         * List of Spring AI tool callbacks for function calling.
         */
        private final List<ToolCallback> toolCallbacks = new ArrayList<>();

        /**
         * List of Spring AI tools for function calling.
         */
        private final List<Object> tools = new ArrayList<>();

        /**
         * Unique identifier for the agent being built.
         */
        private String agentId;

        /**
         * Initial state for the agent being built.
         */
        private State state;

        /**
         * Static system message content for the agent.
         */
        private String systemMessage;

        /**
         * Dynamic system message provider function.
         */
        private Function<LocalAgent, String> systemMessageProvider;

        /**
         * Chat memory implementation for conversation persistence.
         */
        private ChatMemory chatMemory;

        /**
         * Initial messages for the agent
         */
        private List<BaseMessage> messages = new ArrayList<>();

        /**
         * Sets the ChatModel for the agent.
         *
         * @param chatModel the Spring AI ChatModel to use
         * @return this builder instance for method chaining
         */
        public Builder chatModel(final ChatModel chatModel) {
            this.chatModel = chatModel;

            return this;
        }

        /**
         * Adds multiple advisors to the agent configuration.
         *
         * @param advisors list of Spring AI advisors to add
         * @return this builder instance for method chaining
         */
        public Builder advisors(final List<Advisor> advisors) {
            this.advisors.addAll(advisors);

            return this;
        }

        /**
         * Adds a single advisor to the agent configuration.
         *
         * @param advisor the Spring AI advisor to add
         * @return this builder instance for method chaining
         */
        public Builder advisor(final Advisor advisor) {
            this.advisors.add(advisor);

            return this;
        }

        /**
         * Adds multiple tools to the agent configuration.
         *
         * @param tools list of Spring AI tools to add
         * @return this builder instance for method chaining
         */
        public Builder tools(final List<Object> tools) {
            this.tools.addAll(tools);

            return this;
        }

        /**
         * Adds a single tool to the agent configuration
         *
         * @param tool the Spring AI tool to add
         * @return this builder instance for method chaining
         */
        public Builder tool(final Object tool) {
            this.tools.add(tool);

            return this;
        }

        /**
         * Sets the unique identifier for the agent.
         *
         * @param agentId the unique agent identifier
         * @return this builder instance for method chaining
         */
        public Builder agentId(final String agentId) {
            this.agentId = agentId;

            return this;
        }

        /**
         * Sets the initial state for the agent.
         *
         * @param state the initial agent state
         * @return this builder instance for method chaining
         */
        public Builder state(final State state) {
            this.state = state;

            return this;
        }

        /**
         * Adds multiple tool callbacks to the agent configuration.
         *
         * @param toolCallbacks list of Spring AI tool callbacks to add
         * @return this builder instance for method chaining
         */
        public Builder toolCallbacks(final List<ToolCallback> toolCallbacks) {
            this.toolCallbacks.addAll(toolCallbacks);

            return this;
        }

        /**
         * Adds a single tool callback to the agent configuration.
         *
         * @param toolCallback the Spring AI tool callback to add
         * @return this builder instance for method chaining
         */
        public Builder toolCallback(final ToolCallback toolCallback) {
            this.toolCallbacks.add(toolCallback);

            return this;
        }

        /**
         * Sets the static system message for the agent.
         *
         * @param systemMessage the static system message content
         * @return this builder instance for method chaining
         */
        public Builder systemMessage(final String systemMessage) {
            this.systemMessage = systemMessage;

            return this;
        }

        /**
         * Sets the dynamic system message provider for the agent.
         *
         * @param systemMessageProvider function that generates system messages dynamically
         * @return this builder instance for method chaining
         */
        public Builder systemMessageProvider(final Function<LocalAgent, String> systemMessageProvider) {
            this.systemMessageProvider = systemMessageProvider;

            return this;
        }

        /**
         * Sets the chat memory implementation for conversation persistence.
         *
         * @param chatMemory the Spring AI ChatMemory implementation
         * @return this builder instance for method chaining
         */
        public Builder chatMemory(final ChatMemory chatMemory) {
            this.chatMemory = chatMemory;

            return this;
        }

        /**
         * Sets the initial messages
         *
         * @param messages the initial Messages for the agent
         * @return this builder instance for method chaining
         */
        public Builder messages(final List<BaseMessage> messages) {
            this.messages = messages;

            return this;
        }

        /**
         * Builds and returns a new SpringAIAgent instance with the configured parameters.
         *
         * @return a new SpringAIAgent instance
         * @throws AGUIException if the configuration is invalid or required parameters are missing
         */
        public SpringAIAgent build() throws AGUIException {
            return new SpringAIAgent(this);
        }
    }

}
