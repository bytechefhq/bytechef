/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ChatCompletionRequestModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ChatCompletionResponseModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ChatMessageModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ChoiceModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ContentBlockImageUrlModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ContentBlockModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.StreamErrorChunkModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ToolCallFunctionModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ToolCallModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ToolFunctionModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.ToolModel;
import com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model.UsageModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayContentBlockType;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayTool;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayToolChoice;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

/**
 * @version ee
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class EmbeddedAiGatewayChatCompletionApiController {

    private static final String PATH_CHAT_COMPLETIONS = "/{externalUserId}/ai-gateway/chat/completions";

    private final AiGatewayFacade aiGatewayFacade;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public EmbeddedAiGatewayChatCompletionApiController(
        AiGatewayFacade aiGatewayFacade, ConnectedUserService connectedUserService) {

        this.aiGatewayFacade = aiGatewayFacade;
        this.connectedUserService = connectedUserService;
    }

    @PostMapping(
        value = PATH_CHAT_COMPLETIONS, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> chatCompletions(
        @PathVariable String externalUserId, @RequestBody ChatCompletionRequestModel payload) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        Optional<Long> connectedUserId = resolveConnectedUserId(externalUserId);

        if (connectedUserId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .build();
        }

        AiGatewayChatCompletionRequest request = toDomainRequest(payload);

        if (request.stream()) {
            throw new IllegalArgumentException("Streaming requests must use Accept: text/event-stream header");
        }

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, null, connectedUserId.get());

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        applyGatewayMetadataHeaders(builder, response);

        return builder.body(toResponseModel(response));
    }

    @PostMapping(value = PATH_CHAT_COMPLETIONS, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chatCompletionsStream(
        @PathVariable String externalUserId, @RequestBody ChatCompletionRequestModel payload) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        Optional<Long> connectedUserId = resolveConnectedUserId(externalUserId);

        if (connectedUserId.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Connected user not found or not enabled for external id: " + externalUserId);
        }

        return Flux.defer(() -> {
            AiGatewayChatCompletionRequest request = toDomainRequest(payload);

            return aiGatewayFacade.chatCompletionStream(request, null, connectedUserId.get(), null)
                .<ServerSentEvent<Object>>map(
                    domainResponse -> ServerSentEvent.<Object>builder()
                        .data(toResponseModel(domainResponse))
                        .build());
        })
            .onErrorResume(
                exception -> Flux.just(
                    ServerSentEvent.<Object>builder()
                        .event("error")
                        .data(
                            new StreamErrorChunkModel()
                                .type(resolveErrorType(exception))
                                .message(
                                    exception.getMessage() != null ? exception.getMessage()
                                        : "Streaming failed"))
                        .build()));
    }

    private static String resolveErrorType(Throwable exception) {
        if (exception instanceof BudgetExceededException) {
            return "budget_exceeded";
        }

        return exception.getClass()
            .getSimpleName();
    }

    private Optional<Long> resolveConnectedUserId(String externalUserId) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        if (!(securityContext
            .getAuthentication() instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken)) {

            return Optional.empty();
        }

        return connectedUserService.fetchConnectedUser(externalUserId, apiKeyAuthenticationToken.getEnvironmentId())
            .filter(ConnectedUser::isEnabled)
            .map(ConnectedUser::getId);
    }

    private static void applyGatewayMetadataHeaders(
        ResponseEntity.BodyBuilder builder, AiGatewayChatCompletionResponse response) {

        AiGatewayChatCompletionResponse.GatewayMetadata metadata = response.gatewayMetadata();

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

    private static <T> List<T> nullIfEmpty(List<T> values) {
        return values == null || values.isEmpty() ? null : values;
    }

    private static AiGatewayChatCompletionRequest toDomainRequest(ChatCompletionRequestModel payload) {
        if (payload.getModel() == null || payload.getModel()
            .isBlank()) {

            throw new IllegalArgumentException("'model' field is required");
        }

        if (payload.getMessages() == null || payload.getMessages()
            .isEmpty()) {

            throw new IllegalArgumentException("'messages' field is required and must not be empty");
        }

        List<AiGatewayChatMessage> messages = payload.getMessages()
            .stream()
            .map(EmbeddedAiGatewayChatCompletionApiController::toDomainMessage)
            .toList();

        List<ToolModel> toolModels = nullIfEmpty(payload.getTools());

        List<AiGatewayTool> tools = toolModels == null
            ? null
            : toolModels.stream()
                .map(EmbeddedAiGatewayChatCompletionApiController::toDomainTool)
                .toList();

        Map<String, String> tags = payload.getTags();

        return new AiGatewayChatCompletionRequest(
            payload.getModel(), messages, payload.getTemperature(), payload.getMaxTokens(), payload.getTopP(),
            Boolean.TRUE.equals(payload.getStream()), payload.getRoutingPolicy(), payload.getCache(),
            toToolChoice(payload.getToolChoice()), tools, tags == null || tags.isEmpty() ? null : tags);
    }

    private static AiGatewayChatMessage toDomainMessage(ChatMessageModel messageModel) {
        List<ToolCallModel> toolCallModels = nullIfEmpty(messageModel.getToolCalls());

        List<AiGatewayChatMessage.ToolCall> toolCalls = toolCallModels == null
            ? null
            : toolCallModels.stream()
                .map(EmbeddedAiGatewayChatCompletionApiController::toDomainToolCall)
                .toList();

        List<ContentBlockModel> contentBlockModels = nullIfEmpty(messageModel.getContentBlocks());

        List<AiGatewayContentBlock> contentBlocks = contentBlockModels == null
            ? null
            : contentBlockModels.stream()
                .map(EmbeddedAiGatewayChatCompletionApiController::toDomainContentBlock)
                .toList();

        return new AiGatewayChatMessage(
            AiGatewayChatRole.fromValue(messageModel.getRole()), messageModel.getContent(), contentBlocks, toolCalls,
            messageModel.getToolCallId());
    }

    private static AiGatewayContentBlock toDomainContentBlock(ContentBlockModel blockModel) {
        ContentBlockImageUrlModel imageUrlModel = blockModel.getImageUrl();

        AiGatewayContentBlock.ImageUrl imageUrl = imageUrlModel == null
            ? null
            : new AiGatewayContentBlock.ImageUrl(imageUrlModel.getUrl(), imageUrlModel.getDetail());

        return new AiGatewayContentBlock(
            AiGatewayContentBlockType.fromValue(blockModel.getType()), blockModel.getText(), imageUrl, null);
    }

    private static AiGatewayChatMessage.ToolCall toDomainToolCall(ToolCallModel toolCallModel) {
        ToolCallFunctionModel functionModel = toolCallModel.getFunction();

        return new AiGatewayChatMessage.ToolCall(
            toolCallModel.getId(), toolCallModel.getType(),
            functionModel == null
                ? null
                : new AiGatewayChatMessage.ToolCallFunction(functionModel.getName(), functionModel.getArguments()));
    }

    private static AiGatewayTool toDomainTool(ToolModel toolModel) {
        ToolFunctionModel functionModel = toolModel.getFunction();

        if (functionModel == null) {
            return new AiGatewayTool(toolModel.getType(), null);
        }

        Object parameters = functionModel.getParameters();

        if (parameters != null && !(parameters instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Invalid tool parameters: expected a JSON object");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parameterMap = (Map<String, Object>) parameters;

        return new AiGatewayTool(
            toolModel.getType(),
            new AiGatewayTool.AiGatewayToolFunction(
                functionModel.getName(), functionModel.getDescription(), parameterMap));
    }

    private static AiGatewayToolChoice toToolChoice(Object toolChoice) {
        switch (toolChoice) {
            case null -> {
                return null;
            }
            case String stringValue -> {
                return AiGatewayToolChoice.ofString(stringValue);
            }
            case Map<?, ?> map -> {
                Object toolRefObject = map.get("function");

                if (toolRefObject instanceof Map<?, ?> toolRefMap) {
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

    private static ChatCompletionResponseModel toResponseModel(AiGatewayChatCompletionResponse response) {
        ChatCompletionResponseModel responseModel = new ChatCompletionResponseModel()
            .id(response.id())
            ._object(response.object())
            .created(response.created())
            .model(response.model());

        if (response.choices() != null) {
            responseModel.setChoices(
                response.choices()
                    .stream()
                    .map(EmbeddedAiGatewayChatCompletionApiController::toChoiceModel)
                    .toList());
        }

        return responseModel.usage(toUsageModel(response.usage()));
    }

    private static ChoiceModel toChoiceModel(AiGatewayChatCompletionResponse.Choice choice) {
        return new ChoiceModel()
            .index(choice.index())
            .message(choice.message() == null ? null : toMessageModel(choice.message()))
            .finishReason(choice.finishReason());
    }

    private static ChatMessageModel toMessageModel(AiGatewayChatMessage message) {
        ChatMessageModel messageModel = new ChatMessageModel()
            .role(
                message.role()
                    .getValue())
            .content(message.content());

        if (message.toolCalls() != null) {
            messageModel.setToolCalls(
                message.toolCalls()
                    .stream()
                    .map(EmbeddedAiGatewayChatCompletionApiController::toToolCallModel)
                    .toList());
        }

        return messageModel;
    }

    private static ToolCallModel toToolCallModel(AiGatewayChatMessage.ToolCall toolCall) {
        AiGatewayChatMessage.ToolCallFunction function = toolCall.function();

        return new ToolCallModel()
            .id(toolCall.id())
            .type(toolCall.type())
            .function(
                function == null
                    ? null
                    : new ToolCallFunctionModel()
                        .name(function.name())
                        .arguments(function.arguments()));
    }

    private static UsageModel toUsageModel(AiGatewayChatCompletionResponse.Usage usage) {
        if (usage == null) {
            return null;
        }

        return new UsageModel()
            .promptTokens((long) usage.promptTokens())
            .completionTokens((long) usage.completionTokens())
            .totalTokens((long) usage.totalTokens());
    }
}
