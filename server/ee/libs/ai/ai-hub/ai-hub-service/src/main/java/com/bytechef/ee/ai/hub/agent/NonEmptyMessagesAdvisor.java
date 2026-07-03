/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Final defensive layer that prevents Anthropic's {@code messages.<N>: user messages must have non-empty content} HTTP
 * 400 from aborting the streaming turn.
 *
 * <p>
 * Root cause is Spring AI 2.0.0-M{4,5,6}'s {@code JdbcChatMemoryRepository}: the schema only stores
 * {@code (conversation_id, content, type, timestamp)} — there is NO column for
 * {@link ToolResponseMessage#getResponses() responses} or {@link AssistantMessage#getToolCalls() tool calls}. The
 * repository's load path comments the issue inline:
 * </p>
 *
 * <pre>{@code
 * // The content is always stored empty for ToolResponseMessages.
 * // If we want to capture the actual content, we need to extend
 * // AddBatchPreparedStatement to support it.
 * case TOOL -> ToolResponseMessage.builder().responses(List.of()).build();
 * }</pre>
 *
 * <p>
 * The practical effect is: every {@code ToolResponseMessage} replayed from chat memory has an empty {@code responses}
 * list, and every {@code AssistantMessage} replayed from memory has no {@code toolCalls}. When the Anthropic adapter
 * serialises these to API format, the tool_result content block becomes empty and Anthropic rejects the request —
 * aborting every multi-turn conversation that involved tool calls.
 * </p>
 *
 * <h2>Strategy</h2>
 *
 * <ul>
 * <li><b>Strip</b> a {@link ToolResponseMessage} whose responses are all empty (or whose responses list is itself
 * empty) — it carries no usable information for the LLM and breaks the Anthropic wire format.</li>
 * <li><b>Strip</b> an {@link AssistantMessage} that has neither text nor tool calls — same rationale.</li>
 * <li><b>Substitute placeholder</b> for a {@link ToolResponseMessage} that has some empty + some non-empty responses
 * (parallel-tool-call race within current turn) so Anthropic accepts the call and the LLM still sees the non-empty
 * siblings.</li>
 * <li><b>Substitute placeholder</b> for any {@link UserMessage} with blank text — defensive, shouldn't happen but cheap
 * to guard.</li>
 * </ul>
 *
 * <p>
 * Stripping is the right move for chat-memory replay because Spring AI's persistence is lossy by design: substituting a
 * {@code "no content"} placeholder would feed the LLM hallucinated "this tool returned nothing" history that doesn't
 * match the actual prior turn. Removing the stale rows produces a clean user/assistant text-only history that the LLM
 * can reason over without contradiction.
 * </p>
 *
 * <p>
 * Current-turn messages always have either text or tool calls (assistant) or non-empty responses (tool), so the strip
 * predicates only fire on stale-from-memory entries. Every action logs at WARN with the message position so ops can
 * confirm the upstream source.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class NonEmptyMessagesAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(NonEmptyMessagesAdvisor.class);

    private static final String USER_PLACEHOLDER = "no content";
    private static final String TOOL_PLACEHOLDER = "{\"result\":\"no content\"}";

    /**
     * Run as the last advisor in the chain so memory injection and any other request rewrites have already settled — by
     * ordering after them we patch the final outbound shape, not an intermediate state another advisor may overwrite.
     */
    private static final int ORDER = Integer.MAX_VALUE - 1;

    @Override
    public String getName() {
        return "ai-hub-non-empty-messages";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(patch(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(patch(chatClientRequest));
    }

    private ChatClientRequest patch(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> original = prompt.getInstructions();
        List<Message> patched = new ArrayList<>(original.size());

        boolean changed = false;

        for (int index = 0; index < original.size(); index++) {
            Message message = original.get(index);
            Message replacement = patchMessage(message, index);

            if (replacement == null) {
                // Stripped entirely (stale chat-memory replay). Skip and mark changed.
                changed = true;

                continue;
            }

            if (replacement != message) {
                changed = true;
            }

            patched.add(replacement);
        }

        if (!changed) {
            return chatClientRequest;
        }

        Prompt patchedPrompt = new Prompt(patched, prompt.getOptions());

        return chatClientRequest.mutate()
            .prompt(patchedPrompt)
            .build();
    }

    /**
     * Returns the message to keep, a replacement, or {@code null} to strip entirely.
     */
    private Message patchMessage(Message message, int index) {
        MessageType type = message.getMessageType();

        if (type == MessageType.USER) {
            String text = message.getText();

            if (text != null && !text.isBlank()) {
                return message;
            }

            log.warn(
                "Substituting placeholder for empty USER message at position {} before Anthropic dispatch — "
                    + "Anthropic rejects empty user content with HTTP 400.",
                index);

            return UserMessage.builder()
                .text(USER_PLACEHOLDER)
                .metadata(message.getMetadata())
                .build();
        }

        if (type == MessageType.ASSISTANT && message instanceof AssistantMessage assistantMessage) {
            String text = assistantMessage.getText();
            boolean hasText = text != null && !text.isBlank();
            boolean hasToolCalls = assistantMessage.hasToolCalls();

            if (hasText || hasToolCalls) {
                return message;
            }

            return null;
        }

        if (type == MessageType.TOOL && message instanceof ToolResponseMessage toolMessage) {
            List<ToolResponse> responses = toolMessage.getResponses();

            if (responses.isEmpty()) {
                return null;
            }

            boolean allEmpty = responses.stream()
                .allMatch(response -> response.responseData() == null || response.responseData()
                    .isBlank());

            if (allEmpty) {
                return null;
            }

            // Mixed: some non-empty, some empty. Substitute placeholders for empty ones so Anthropic doesn't
            // reject — this is a current-turn parallel-tool-call race rather than chat-memory replay.
            return patchMixedResponses(toolMessage, responses, index);
        }

        return message;
    }

    private Message patchMixedResponses(ToolResponseMessage toolMessage, List<ToolResponse> responses, int index) {
        List<ToolResponse> patchedResponses = null;

        for (int i = 0; i < responses.size(); i++) {
            ToolResponse response = responses.get(i);
            String data = response.responseData();

            if (data != null && !data.isBlank()) {
                if (patchedResponses != null) {
                    patchedResponses.add(response);
                }

                continue;
            }

            if (patchedResponses == null) {
                patchedResponses = new ArrayList<>(responses.subList(0, i));
            }

            log.warn(
                "Substituting placeholder for empty tool response at message {} response {} "
                    + "(tool='{}', toolCallId='{}') — parallel-call race within the current turn.",
                index, i, response.name(), response.id());

            patchedResponses.add(new ToolResponse(response.id(), response.name(), TOOL_PLACEHOLDER));
        }

        if (patchedResponses == null) {
            return toolMessage;
        }

        return ToolResponseMessage.builder()
            .responses(patchedResponses)
            .metadata(toolMessage.getMetadata())
            .build();
    }
}
