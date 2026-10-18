/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import reactor.core.publisher.Flux;

/**
 * Stores each result of a tool call that ended the turn as an archived TOOL event tagged
 * {@link #DISPLAY_ONLY_METADATA_KEY}, for display in the chat history only. A reader of archived session events, such
 * as a recall tool, must skip events with that tag.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubTurnEndingToolResultAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String DISPLAY_ONLY_METADATA_KEY = "aiHubDisplayOnly";

    private static final Logger log = LoggerFactory.getLogger(AiHubTurnEndingToolResultAdvisor.class);

    private final SessionService sessionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubTurnEndingToolResultAdvisor(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public static boolean isDisplayOnly(SessionEvent sessionEvent) {
        return Boolean.TRUE.equals(sessionEvent.getMetadata()
            .get(DISPLAY_ONLY_METADATA_KEY));
    }

    @Override
    public String getName() {
        return AiHubTurnEndingToolResultAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER - 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        storeTurnEndingToolResults(chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(chatClientRequest)
            .doOnNext(this::storeTurnEndingToolResults);
    }

    private void storeTurnEndingToolResults(ChatClientResponse chatClientResponse) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return;
        }

        List<ToolResponse> toolResponses = chatResponse.getResults()
            .stream()
            .map(AiHubTurnEndingToolResultAdvisor::toToolResponse)
            .filter(Objects::nonNull)
            .toList();

        if (toolResponses.isEmpty()) {
            return;
        }

        if (!(chatClientResponse.context()
            .get(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY) instanceof String sessionId) || sessionId.isBlank()) {

            return;
        }

        try {
            sessionService.appendEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .message(ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build())
                .metadata(DISPLAY_ONLY_METADATA_KEY, true)
                .archived(true)
                .build());
        } catch (RuntimeException exception) {
            log.warn("Failed to store the turn-ending tool result for session {}", sessionId, exception);
        }
    }

    private static @Nullable ToolResponse toToolResponse(Generation generation) {
        ChatGenerationMetadata metadata = generation.getMetadata();

        if (!ToolExecutionResult.FINISH_REASON.equals(metadata.getFinishReason())) {
            return null;
        }

        Object toolCallId = metadata.get(ToolExecutionResult.METADATA_TOOL_ID);
        Object toolName = metadata.get(ToolExecutionResult.METADATA_TOOL_NAME);

        if (!(toolCallId instanceof String toolCallIdString) || !(toolName instanceof String toolNameString)) {
            return null;
        }

        return new ToolResponse(
            toolCallIdString, toolNameString, Objects.requireNonNullElse(generation.getOutput()
                .getText(), ""));
    }
}
