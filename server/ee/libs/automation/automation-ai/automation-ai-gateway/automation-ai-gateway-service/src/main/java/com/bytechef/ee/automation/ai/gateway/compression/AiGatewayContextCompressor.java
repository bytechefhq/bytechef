/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.compression;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Context compression service that reduces token usage before requests reach LLM providers.
 *
 * <p>
 * Two techniques applied in order:
 * <ol>
 * <li>Lossless JSON minification — minifies JSON in tool schemas/arguments/results</li>
 * <li>Message trimming — removes oldest non-system messages, preserving system + recent messages</li>
 * </ol>
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayContextCompressor {

    private static final int ESTIMATED_DOCUMENT_TOKEN_CHARS = 1500 * 4;
    private static final int ESTIMATED_IMAGE_TOKEN_CHARS = 765 * 4;
    private static final Logger logger = LoggerFactory.getLogger(AiGatewayContextCompressor.class);

    private final ObjectMapper objectMapper;

    public AiGatewayContextCompressor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Compress messages by applying JSON minification and message trimming.
     *
     * @param messages         the original messages
     * @param maxTokenEstimate the target max token count (0 = no trimming, only minification)
     * @return compressed messages
     */
    public List<AiGatewayChatMessage> compress(
        List<AiGatewayChatMessage> messages, int maxTokenEstimate) {

        List<AiGatewayChatMessage> compressedMessages = new ArrayList<>();

        for (AiGatewayChatMessage message : messages) {
            compressedMessages.add(minifyJsonContent(message));
        }

        if (maxTokenEstimate > 0 && estimateTokens(compressedMessages) > maxTokenEstimate) {
            compressedMessages = trimMessages(compressedMessages, maxTokenEstimate);
        }

        return compressedMessages;
    }

    private int estimateTokens(List<AiGatewayChatMessage> messages) {
        int totalChars = 0;

        for (AiGatewayChatMessage message : messages) {
            if (message.content() != null) {
                totalChars += message.content()
                    .length();
            }

            if (message.hasContentBlocks()) {
                for (AiGatewayContentBlock block : message.contentBlocks()) {
                    switch (block.type()) {
                        case TEXT -> {
                            if (block.text() != null) {
                                totalChars += block.text()
                                    .length();
                            }
                        }
                        case IMAGE_URL, IMAGE -> totalChars += ESTIMATED_IMAGE_TOKEN_CHARS;
                        case DOCUMENT -> totalChars += ESTIMATED_DOCUMENT_TOKEN_CHARS;
                        default -> throw new IllegalArgumentException(
                            "Unsupported content block type: " + block.type());
                    }
                }
            }
        }

        return totalChars / 4;
    }

    private AiGatewayChatMessage minifyJsonContent(AiGatewayChatMessage message) {
        if (message.content() == null) {
            return message;
        }

        String content = message.content()
            .trim();

        if (!content.startsWith("{") && !content.startsWith("[")) {
            return message;
        }

        try {
            Object parsed = objectMapper.readValue(content, Object.class);
            String minified = objectMapper.writeValueAsString(parsed);

            return new AiGatewayChatMessage(
                message.role(), minified, message.contentBlocks(), message.toolCalls(),
                message.toolCallId());
        } catch (JacksonException jacksonException) {
            logger.debug("Failed to minify JSON content in message (role={}): {}",
                message.role(), jacksonException.getMessage());

            return message;
        }
    }

    private List<AiGatewayChatMessage> trimMessages(
        List<AiGatewayChatMessage> messages, int maxTokenEstimate) {

        List<AiGatewayChatMessage> systemMessages = new ArrayList<>();
        List<AiGatewayChatMessage> nonSystemMessages = new ArrayList<>();

        for (AiGatewayChatMessage message : messages) {
            if (AiGatewayChatRole.SYSTEM == message.role()) {
                systemMessages.add(message);
            } else {
                nonSystemMessages.add(message);
            }
        }

        List<AiGatewayChatMessage> keptMessages = new ArrayList<>(nonSystemMessages);

        while (estimateTokens(systemMessages) + estimateTokens(keptMessages) > maxTokenEstimate
            && keptMessages.size() > 1) {

            keptMessages.removeFirst();
        }

        List<AiGatewayChatMessage> result = new ArrayList<>(systemMessages);

        result.addAll(keptMessages);

        return result;
    }
}
