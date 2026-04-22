/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayContentBlockType;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayTool;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayToolChoice;
import com.bytechef.ee.automation.ai.gateway.dto.AiObservabilityTracingHeaders;
import com.bytechef.ee.automation.ai.gateway.dto.AiPromptHeaders;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChatCompletionRequestModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChatCompletionResponseModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChatMessageModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChoiceModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ToolCallModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.UsageModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Flux;

/**
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayChatCompletionApiController implements ChatCompletionApi {

    private final AiGatewayFacade aiGatewayFacade;

    AiGatewayChatCompletionApiController(AiGatewayFacade aiGatewayFacade) {
        this.aiGatewayFacade = aiGatewayFacade;
    }

    @Override
    public ResponseEntity<ChatCompletionResponseModel> chatCompletions(
        ChatCompletionRequestModel chatCompletionRequestModel) {

        AiGatewayChatCompletionRequest request = toDomainRequest(chatCompletionRequestModel);

        if (Boolean.TRUE.equals(chatCompletionRequestModel.getStream())) {
            throw new IllegalArgumentException(
                "Streaming requests must use Accept: text/event-stream header");
        }

        AiObservabilityTracingHeaders tracingHeaders = resolveTracingHeaders();
        AiPromptHeaders promptHeaders = resolvePromptHeaders();

        AiGatewayChatCompletionResponse domainResponse =
            aiGatewayFacade.chatCompletion(request, tracingHeaders, promptHeaders);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        applyGatewayMetadataHeaders(builder, domainResponse);

        return builder.body(toResponseModel(domainResponse));
    }

    /**
     * Emits {@code x-gateway-*} response headers per spec F3 when the facade populated metadata. Headers are skipped
     * individually when their value is null so clients don't see empty/"null" strings.
     */
    private static void applyGatewayMetadataHeaders(
        ResponseEntity.BodyBuilder builder, AiGatewayChatCompletionResponse domainResponse) {

        AiGatewayChatCompletionResponse.GatewayMetadata metadata = domainResponse.gatewayMetadata();

        if (metadata == null) {
            return;
        }

        if (metadata.provider() != null) {
            builder.header("x-gateway-provider", metadata.provider());
        }

        if (metadata.model() != null) {
            builder.header("x-gateway-model", metadata.model());
        }

        if (metadata.latencyMs() != null) {
            builder.header("x-gateway-latency-ms", String.valueOf(metadata.latencyMs()));
        }

        if (metadata.cacheHit() != null) {
            builder.header("x-gateway-cache-hit", String.valueOf(metadata.cacheHit()));
        }

        if (metadata.routingPolicy() != null) {
            builder.header("x-gateway-routing-policy", metadata.routingPolicy());
        }

        if (metadata.requestId() != null) {
            builder.header("x-gateway-request-id", metadata.requestId());
        }

        if (metadata.budgetWarningRemainingUsd() != null) {
            builder.header(
                "x-gateway-budget-warning",
                metadata.budgetWarningRemainingUsd()
                    .toPlainString());
        }
    }

    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<Object>> chatCompletionsStream(
        @RequestBody ChatCompletionRequestModel chatCompletionRequestModel) {

        // Defer request translation, tracing/prompt header resolution, and facade setup inside the reactive pipeline
        // so any synchronous throw (invalid request body, missing model, budget rejection, rate-limit rejection) is
        // converted into a reactor error signal and framed by onErrorResume. Without the defer, a validation throw
        // from toDomainRequest or an immediate rejection inside facade.chatCompletionStream escapes as a 400/403/429
        // with a JSON error body and an abruptly closed stream — clients never see the SSE `event: error` frame.
        return Flux.defer(() -> {
            AiGatewayChatCompletionRequest request = toDomainRequest(chatCompletionRequestModel);

            AiObservabilityTracingHeaders tracingHeaders = resolveTracingHeaders();
            AiPromptHeaders promptHeaders = resolvePromptHeaders();

            return aiGatewayFacade.chatCompletionStream(request, tracingHeaders, promptHeaders)
                .<ServerSentEvent<Object>>map(domainResponse -> ServerSentEvent.<Object>builder()
                    .data(toResponseModel(domainResponse))
                    .build());
        })
            // Frame upstream errors as a terminal `event: error` SSE so clients see a structured failure rather than a
            // silently-closed stream. Matches the OpenAI-compatible contract where a mid-stream error is emitted as a
            // data-framed payload even after headers have been flushed.
            .onErrorResume(exception -> Flux.just(ServerSentEvent.<Object>builder()
                .event("error")
                .data(new StreamErrorChunkModel(
                    resolveErrorType(exception),
                    exception.getMessage() != null ? exception.getMessage() : "Streaming failed"))
                .build()));
    }

    private static String resolveErrorType(Throwable exception) {
        if (exception instanceof com.bytechef.ee.automation.ai.gateway.domain.BudgetExceededException) {
            return "budget_exceeded";
        }

        return exception.getClass()
            .getSimpleName();
    }

    public record StreamErrorChunkModel(String type, String message) {
    }

    private AiPromptHeaders resolvePromptHeaders() {
        ServletRequestAttributes requestAttributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            return new AiPromptHeaders(null, null);
        }

        return extractPromptHeaders(requestAttributes.getRequest());
    }

    private AiPromptHeaders extractPromptHeaders(HttpServletRequest httpServletRequest) {
        return new AiPromptHeaders(
            httpServletRequest.getHeader(AiPromptHeaders.HEADER_PROMPT_NAME),
            httpServletRequest.getHeader(AiPromptHeaders.HEADER_PROMPT_ENVIRONMENT));
    }

    private AiObservabilityTracingHeaders resolveTracingHeaders() {
        ServletRequestAttributes requestAttributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            return new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());
        }

        return extractTracingHeaders(requestAttributes.getRequest());
    }

    private AiObservabilityTracingHeaders extractTracingHeaders(HttpServletRequest httpServletRequest) {
        String traceId = httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_TRACE_ID);
        String sessionId = httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_SESSION_ID);
        String spanName = httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_SPAN_NAME);
        String parentSpanId = httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_PARENT_SPAN_ID);
        String userId = httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_USER_ID);

        Map<String, String> metadata = new HashMap<>();

        java.util.Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();

                if (headerName.toLowerCase()
                    .startsWith(AiObservabilityTracingHeaders.HEADER_METADATA_PREFIX.toLowerCase())) {

                    String metadataKey = headerName.substring(
                        AiObservabilityTracingHeaders.HEADER_METADATA_PREFIX.length());

                    metadata.put(metadataKey, httpServletRequest.getHeader(headerName));
                }
            }
        }

        List<String> tagNames = new ArrayList<>();

        String tagsHeader = httpServletRequest.getHeader(AiObservabilityTracingHeaders.HEADER_TAGS);

        if (tagsHeader != null && !tagsHeader.isBlank()) {
            tagNames = Arrays.stream(tagsHeader.split(","))
                .map(String::trim)
                .filter(tagName -> !tagName.isEmpty())
                .toList();
        }

        return new AiObservabilityTracingHeaders(
            traceId, sessionId, spanName, parentSpanId, userId, metadata, tagNames);
    }

    private AiGatewayChatCompletionRequest toDomainRequest(ChatCompletionRequestModel model) {
        if (model.getModel() == null || model.getModel()
            .isBlank()) {

            throw new IllegalArgumentException("'model' field is required");
        }

        if (model.getMessages() == null || model.getMessages()
            .isEmpty()) {

            throw new IllegalArgumentException("'messages' field is required and must not be empty");
        }

        List<AiGatewayChatMessage> messages = model.getMessages()
            .stream()
            .map(this::toDomainMessage)
            .toList();

        List<AiGatewayTool> tools = model.getTools() == null
            ? null
            : model.getTools()
                .stream()
                .map(toolModel -> new AiGatewayTool(
                    toolModel.getType(),
                    toolModel.getFunction() == null
                        ? null
                        : new AiGatewayTool.AiGatewayToolFunction(
                            toolModel.getFunction()
                                .getName(),
                            toolModel.getFunction()
                                .getDescription(),
                            toParametersMap(toolModel.getFunction()
                                .getParameters()))))
                .toList();

        Map<String, String> tags = model.getTags() == null
            ? null
            : model.getTags()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));

        return new AiGatewayChatCompletionRequest(
            model.getModel(), messages, model.getTemperature(), model.getMaxTokens(),
            model.getTopP(), Boolean.TRUE.equals(model.getStream()), model.getRoutingPolicy(),
            model.getCache(), toToolChoice(model.getToolChoice()), tools, tags);
    }

    private AiGatewayChatMessage toDomainMessage(ChatMessageModel messageModel) {
        List<AiGatewayChatMessage.ToolCall> toolCalls = messageModel.getToolCalls() == null
            ? null
            : messageModel.getToolCalls()
                .stream()
                .map(this::toDomainToolCall)
                .toList();

        List<AiGatewayContentBlock> contentBlocks = null;

        if (messageModel.getContentBlocks() != null) {
            contentBlocks = messageModel.getContentBlocks()
                .stream()
                .map(blockModel -> {
                    AiGatewayContentBlock.ImageUrl imageUrl = blockModel.getImageUrl() == null
                        ? null
                        : new AiGatewayContentBlock.ImageUrl(
                            blockModel.getImageUrl()
                                .getUrl(),
                            blockModel.getImageUrl()
                                .getDetail());

                    return new AiGatewayContentBlock(
                        AiGatewayContentBlockType.fromValue(blockModel.getType()),
                        blockModel.getText(), imageUrl, null);
                })
                .toList();
        }

        return new AiGatewayChatMessage(
            AiGatewayChatRole.fromValue(messageModel.getRole()), messageModel.getContent(), contentBlocks,
            toolCalls, messageModel.getToolCallId());
    }

    private AiGatewayChatMessage.ToolCall toDomainToolCall(ToolCallModel toolCallModel) {
        return new AiGatewayChatMessage.ToolCall(
            toolCallModel.getId(), toolCallModel.getType(),
            toolCallModel.getFunction() == null
                ? null
                : new AiGatewayChatMessage.ToolCallFunction(
                    toolCallModel.getFunction()
                        .getName(),
                    toolCallModel.getFunction()
                        .getArguments()));
    }

    private ChatCompletionResponseModel toResponseModel(AiGatewayChatCompletionResponse domainResponse) {
        ChatCompletionResponseModel responseModel = new ChatCompletionResponseModel();

        responseModel.setId(domainResponse.id());
        responseModel.setObject(domainResponse.object());
        responseModel.setCreated(domainResponse.created());
        responseModel.setModel(domainResponse.model());

        if (domainResponse.choices() != null) {
            List<ChoiceModel> choiceModels = domainResponse.choices()
                .stream()
                .map(choice -> {
                    ChoiceModel choiceModel = new ChoiceModel();

                    choiceModel.setIndex(choice.index());
                    choiceModel.setFinishReason(choice.finishReason());

                    if (choice.message() != null) {
                        ChatMessageModel messageModel = new ChatMessageModel();

                        messageModel.setRole(choice.message()
                            .role()
                            .getValue());
                        messageModel.setContent(choice.message()
                            .content());

                        if (choice.message()
                            .toolCalls() != null) {
                            List<ToolCallModel> toolCallModels = choice.message()
                                .toolCalls()
                                .stream()
                                .map(this::toToolCallModel)
                                .toList();

                            messageModel.setToolCalls(toolCallModels);
                        }

                        choiceModel.setMessage(messageModel);
                    }

                    return choiceModel;
                })
                .toList();

            responseModel.setChoices(choiceModels);
        }

        if (domainResponse.usage() != null) {
            UsageModel usageModel = new UsageModel();

            usageModel.setPromptTokens((long) domainResponse.usage()
                .promptTokens());
            usageModel.setCompletionTokens((long) domainResponse.usage()
                .completionTokens());
            usageModel.setTotalTokens((long) domainResponse.usage()
                .totalTokens());

            responseModel.setUsage(usageModel);
        }

        return responseModel;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toParametersMap(Object parameters) {
        if (parameters instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        if (parameters != null) {
            throw new IllegalArgumentException(
                "Expected tool function parameters to be a Map, got: " + parameters.getClass()
                    .getSimpleName());
        }

        return null;
    }

    private AiGatewayToolChoice toToolChoice(Object toolChoice) {
        switch (toolChoice) {
            case null -> {
                return null;
            }
            case String stringValue -> {
                return AiGatewayToolChoice.ofString(stringValue);
            }
            case Map<?, ?> map -> {
                Object toolRefObj = map.get("function");

                if (toolRefObj instanceof Map<?, ?> toolRefMap) {
                    Object name = toolRefMap.get("name");

                    String toolName = name != null ? name.toString() : null;

                    return AiGatewayToolChoice.ofTool(toolName);
                }
            }
            default -> {
            }
        }

        throw new IllegalArgumentException(
            "Invalid tool_choice value: expected string or object with 'function' key");
    }

    private ToolCallModel toToolCallModel(AiGatewayChatMessage.ToolCall toolCall) {
        ToolCallModel toolCallModel = new ToolCallModel();

        toolCallModel.setId(toolCall.id());
        toolCallModel.setType(toolCall.type());

        if (toolCall.function() != null) {
            com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ToolCallFunctionModel functionModel =
                new com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ToolCallFunctionModel();

            functionModel.setName(toolCall.function()
                .name());
            functionModel.setArguments(toolCall.function()
                .arguments());

            toolCallModel.setFunction(functionModel);
        }

        return toolCallModel;
    }
}
