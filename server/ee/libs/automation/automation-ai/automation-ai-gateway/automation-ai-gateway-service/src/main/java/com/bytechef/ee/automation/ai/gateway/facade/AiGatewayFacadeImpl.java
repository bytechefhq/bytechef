/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker;
import com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.platform.ai.gateway.cache.AiGatewayResponseCache;
import com.bytechef.ee.platform.ai.gateway.compression.AiGatewayContextCompressor;
import com.bytechef.ee.platform.ai.gateway.cost.AiGatewayCostCalculator;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingResponse;
import com.bytechef.ee.platform.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayProviderResolver;
import com.bytechef.ee.platform.ai.gateway.reliability.AiGatewayRetryHandler;
import com.bytechef.ee.platform.ai.gateway.routing.AiGatewayRouter;
import com.bytechef.ee.platform.ai.gateway.routing.AiGatewayRoutingContext;
import com.bytechef.ee.platform.ai.gateway.routing.PromptComplexityScorer;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelDeploymentService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayUrlValidator;
import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "BX_UNBOXING_IMMEDIATELY_REBOXED", "EI", "PREDICTABLE_RANDOM", "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT"
})
public class AiGatewayFacadeImpl implements AiGatewayFacade {

    private static final double CONTEXT_WINDOW_USAGE_RATIO = 0.85;
    private static final BigDecimal DEFAULT_FALLBACK_COST_PER_M_TOKENS = new BigDecimal("10.00");
    private static final Logger log = LoggerFactory.getLogger(AiGatewayFacadeImpl.class);

    private final AiGatewayBudgetChecker aiGatewayBudgetChecker;
    private final @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    private final AiGatewayContextCompressor aiGatewayContextCompressor;
    private final AiGatewayCostCalculator aiGatewayCostCalculator;
    private final AiGatewayGuardrails aiGatewayGuardrails;
    private final AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory;
    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;
    private final AiModelService aiModelService;
    private final WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final AiGatewayProviderResolver aiGatewayProviderResolver;
    private final AiLlmUsageService aiGatewayRequestLogService;
    private final AiGatewayResponseCache aiGatewayResponseCache;
    private final AiGatewayRetryHandler aiGatewayRetryHandler;
    private final AiGatewayRouter aiGatewayRouter;
    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;
    private final PromptComplexityScorer promptComplexityScorer;
    private final AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;
    private final ObjectProvider<AiGatewayMetrics> aiGatewayMetricsProvider;
    private final @Nullable PermissionService permissionService;
    private final TransactionTemplate transactionTemplate;

    public AiGatewayFacadeImpl(
        AiGatewayBudgetChecker aiGatewayBudgetChecker,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayChatModelFactory aiGatewayChatModelFactory,
        AiGatewayContextCompressor aiGatewayContextCompressor,
        AiGatewayCostCalculator aiGatewayCostCalculator,
        AiGatewayGuardrails aiGatewayGuardrails,
        AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory,
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService,
        AiModelService aiModelService,
        WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService,
        AiGatewayProviderService aiGatewayProviderService,
        AiGatewayProviderResolver aiGatewayProviderResolver,
        AiLlmUsageService aiGatewayRequestLogService,
        AiGatewayResponseCache aiGatewayResponseCache,
        AiGatewayRetryHandler aiGatewayRetryHandler,
        AiGatewayRouter aiGatewayRouter,
        AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService,
        PromptComplexityScorer promptComplexityScorer,
        AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService,
        ObjectProvider<AiGatewayMetrics> aiGatewayMetricsProvider,
        @Nullable PermissionService permissionService,
        PlatformTransactionManager transactionManager) {

        this.aiGatewayBudgetChecker = aiGatewayBudgetChecker;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
        this.aiGatewayContextCompressor = aiGatewayContextCompressor;
        this.aiGatewayCostCalculator = aiGatewayCostCalculator;
        this.aiGatewayGuardrails = aiGatewayGuardrails;
        this.aiGatewayEmbeddingModelFactory = aiGatewayEmbeddingModelFactory;
        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
        this.aiModelService = aiModelService;
        this.workspaceAiGatewayProjectService = workspaceAiGatewayProjectService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.aiGatewayProviderResolver = aiGatewayProviderResolver;
        this.aiGatewayRequestLogService = aiGatewayRequestLogService;
        this.aiGatewayResponseCache = aiGatewayResponseCache;
        this.aiGatewayRetryHandler = aiGatewayRetryHandler;
        this.aiGatewayRouter = aiGatewayRouter;
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
        this.promptComplexityScorer = promptComplexityScorer;
        this.aiGatewayWorkspaceSettingsService = aiGatewayWorkspaceSettingsService;
        this.aiGatewayMetricsProvider = aiGatewayMetricsProvider;
        this.permissionService = permissionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayChatCompletionResponse chatCompletion(AiGatewayChatCompletionRequest request) {
        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        Long projectId = resolveProjectId(request.tags());

        PiiTokenSession session = aiGatewayGuardrails.newTokenSession();

        try {
            request = aiGatewayGuardrails.apply(request, workspaceId, projectId, session);

            long startTime = System.currentTimeMillis();

            request = applyRoutingPolicyPrecedence(request);

            AiGatewayChatCompletionResponse response;

            if (request.routingPolicy() != null) {
                response = chatCompletionWithRouting(request);
            } else {
                response = chatCompletionDirect(request);
            }

            response = aiGatewayGuardrails.scanResponse(response, workspaceId, projectId);

            response = aiGatewayGuardrails.restoreResponse(response, session);

            return withGatewayMetadata(response, request, startTime);
        } finally {
            session.close();
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(AiGatewayChatCompletionRequest request) {
        return Flux.defer(() -> chatCompletionStreamInternal(request));
    }

    private void recordRequestLogPersistFailure(String kind, String outcome) {
        AiGatewayMetrics metrics = aiGatewayMetricsProvider.getIfAvailable();

        if (metrics != null) {
            metrics.incrementRequestLogPersistFailure(kind, outcome);
        }
    }

    private void validateWorkspaceAccess(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return;
        }

        if (permissionService == null) {
            throw new AccessDeniedException(
                "AiGatewayFacade cannot enforce workspace access: PermissionService bean is not wired. " +
                    "This is a configuration error — refusing request to workspace " + workspaceId);
        }

        var authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AccessDeniedException(
                "AiGatewayFacade cannot enforce workspace access: no authenticated SecurityContext. " +
                    "Internal callers must set a service-account authentication before invoking. " +
                    "Refusing request to workspace " + workspaceId);
        }

        if (permissionService.isTenantAdmin()) {
            return;
        }

        String role = permissionService.getMyWorkspaceRole(workspaceId);

        if (role == null) {
            throw new AccessDeniedException(
                "Authenticated caller is not a member of workspace " + workspaceId);
        }
    }

    private AiGatewayChatCompletionRequest applyRoutingPolicyPrecedence(AiGatewayChatCompletionRequest request) {
        if (request.routingPolicy() != null) {
            return request;
        }

        return applyResolvedRoutingPolicy(request, resolveModelDefaultRoutingPolicyId(request.model()));
    }

    private AiGatewayChatCompletionRequest applyResolvedRoutingPolicy(
        AiGatewayChatCompletionRequest request, @Nullable Long routingPolicyId) {
        if (routingPolicyId == null) {
            return request;
        }

        try {
            AiGatewayRoutingPolicy policy = aiGatewayRoutingPolicyService.getRoutingPolicy(routingPolicyId);

            return new AiGatewayChatCompletionRequest(
                request.model(), request.messages(), request.temperature(), request.maxTokens(), request.topP(),
                request.stream(), policy.getName(), request.cache(), request.toolChoice(), request.tools(),
                request.tags());
        } catch (IllegalArgumentException missingPolicy) {
            log.warn(
                "Resolved routing_policy_id={} for model '{}' but policy not found; falling back to direct routing",
                routingPolicyId, request.model());

            return request;
        }
    }

    private Long resolveModelDefaultRoutingPolicyId(String modelIdentifier) {
        try {
            ModelResolution resolution = resolveModel(modelIdentifier);

            return resolution.model() != null ? resolution.model()
                .getDefaultRoutingPolicyId() : null;
        } catch (IllegalArgumentException resolveFailure) {
            log.warn(
                "Could not resolve default routing policy for model '{}' (reason: {}); " +
                    "falling through to direct routing. Verify the provider is enabled and the model id format is " +
                    "'provider/model'.",
                modelIdentifier, resolveFailure.getMessage());

            return null;
        }
    }

    private AiGatewayChatCompletionResponse withGatewayMetadata(
        AiGatewayChatCompletionResponse response, AiGatewayChatCompletionRequest request, long startTime) {
        if (response == null || response.gatewayMetadata() != null) {
            return response;
        }

        long latencyMs = System.currentTimeMillis() - startTime;
        String providerName = providerFromModel(response.model() != null ? response.model() : request.model());

        AiGatewayChatCompletionResponse.GatewayMetadata metadata = new AiGatewayChatCompletionResponse.GatewayMetadata(
            providerName,
            response.model(),
            latencyMs,
            null,
            request.routingPolicy(),
            response.id(),
            resolveBudgetWarningRemaining(request.tags()));

        return new AiGatewayChatCompletionResponse(
            response.id(), response.object(), response.created(), response.model(),
            response.choices(), response.usage(), metadata);
    }

    private BigDecimal resolveBudgetWarningRemaining(Map<String, String> tags) {
        if (tags == null || !tags.containsKey("workspace_id")) {
            return null;
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            AiGatewayBudgetChecker.BudgetCheckResult result = aiGatewayBudgetChecker.checkBudget(workspaceId);

            if (result.thresholdWarning() && result.budgetAmount() != null && result.currentSpend() != null) {
                return result.budgetAmount()
                    .subtract(result.currentSpend());
            }
        } catch (RuntimeException probeFailure) {
            log.warn(
                "Budget warning probe failed for workspace tag {} — budget warning header will be omitted for this response",
                tags.get("workspace_id"), probeFailure);
        }

        return null;
    }

    private static String providerFromModel(String modelIdentifier) {
        if (modelIdentifier == null) {
            return null;
        }

        int slash = modelIdentifier.indexOf('/');

        return slash > 0 ? modelIdentifier.substring(0, slash) : null;
    }

    private Flux<AiGatewayChatCompletionResponse> chatCompletionStreamInternal(
        AiGatewayChatCompletionRequest request) {
        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        Long projectId = resolveProjectId(request.tags());

        AiGatewayChatCompletionRequest effectiveRequest = applyRoutingPolicyPrecedence(
            aiGatewayGuardrails.apply(request, workspaceId, projectId, null));

        long startTime = System.currentTimeMillis();

        AiGatewayProject project = resolveProject(effectiveRequest.tags());

        StreamModelResolution streamModelResolution =
            resolveStreamModelResolution(effectiveRequest, workspaceId, startTime);

        RoutedDeployments routedDeployments = streamModelResolution.routedDeployments();
        ModelResolution modelResolution = streamModelResolution.modelResolution();

        AiGatewayProvider provider = modelResolution.provider();
        AiModel model = modelResolution.model();

        AtomicReference<AiModel> servedModel = new AtomicReference<>(model);
        AtomicReference<AiGatewayProvider> servedProvider = new AtomicReference<>(provider);

        AtomicLong streamInputTokens = new AtomicLong(0);
        AtomicLong streamOutputTokens = new AtomicLong(0);
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        StreamingResponseRedactor responseRedactor =
            aiGatewayGuardrails.newStreamingResponseRedactor(workspaceId, projectId);
        AtomicReference<String> deferredFinishReason = new AtomicReference<>();

        Flux<ChatResponse> chatResponseFlux;

        if (routedDeployments != null) {
            Map<Long, AiModel> routedModelMap = routedDeployments.modelMap();

            chatResponseFlux = aiGatewayRetryHandler.executeStreamWithRetry(
                routedDeployments.orderedDeployments(),
                deployment -> {
                    AiModel deploymentModel = routedModelMap.get(deployment.getModelId());
                    AiGatewayProvider deploymentProvider =
                        aiGatewayProviderService.getProvider(deploymentModel.getProviderId());
                    ChatModel deploymentChatModel = aiGatewayChatModelFactory.getChatModel(deploymentProvider);
                    Prompt deploymentPrompt = buildPrompt(
                        compressMessages(effectiveRequest, deploymentModel, project), deploymentModel.getName());

                    return deploymentChatModel.stream(deploymentPrompt)
                        .doOnNext(chatResponse -> {
                            servedModel.set(deploymentModel);
                            servedProvider.set(deploymentProvider);
                        });
                });
        } else {
            ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);
            Prompt prompt = buildPrompt(compressMessages(effectiveRequest, model, project), model.getName());

            chatResponseFlux = chatModel.stream(prompt);
        }

        Flux<AiGatewayChatCompletionResponse> chunkFlux = chatResponseFlux
            .map(chatResponse -> toStreamChunkResponse(
                chatResponse, effectiveRequest, streamInputTokens, streamOutputTokens, responseRedactor,
                deferredFinishReason));

        if (responseRedactor != null) {
            chunkFlux = chunkFlux.concatWith(Flux.defer(() -> {
                String tail = responseRedactor.flush();
                String finishReason = deferredFinishReason.get();

                if (responseRedactor.isRedacted()) {
                    aiGatewayGuardrails.recordResponseRedacted();
                }

                if (tail.isEmpty() && finishReason == null) {
                    return Flux.<AiGatewayChatCompletionResponse>empty();
                }

                return Flux.just(streamChunkOf(effectiveRequest.model(), tail, finishReason));
            }));
        }

        return chunkFlux
            .doOnError(streamError::set)
            .doFinally(signalType -> finalizeStreamRequest(
                signalType, streamInputTokens, streamOutputTokens, streamError, effectiveRequest, workspaceId,
                servedModel.get(), servedProvider.get(), project, startTime));
    }

    private AiGatewayChatCompletionResponse toStreamChunkResponse(
        ChatResponse chatResponse, AiGatewayChatCompletionRequest effectiveRequest, AtomicLong streamInputTokens,
        AtomicLong streamOutputTokens, @Nullable StreamingResponseRedactor responseRedactor,
        AtomicReference<String> deferredFinishReason) {
        if (chatResponse.getMetadata() != null && chatResponse.getMetadata()
            .getUsage() != null) {
            long promptTokens = chatResponse.getMetadata()
                .getUsage()
                .getPromptTokens();
            long completionTokens = chatResponse.getMetadata()
                .getUsage()
                .getCompletionTokens();

            if (promptTokens > 0) {
                streamInputTokens.set(promptTokens);
            }

            if (completionTokens > 0) {
                streamOutputTokens.set(completionTokens);
            }
        }

        Generation generation = chatResponse.getResult();

        if (generation == null) {
            return new AiGatewayChatCompletionResponse(
                UUID.randomUUID()
                    .toString(),
                "chat.completion.chunk",
                System.currentTimeMillis() / 1000, effectiveRequest.model(), List.of(), null);
        }

        String chunkText = generation.getOutput()
            .getText();

        String deltaText = chunkText;
        String finishReason = generation.getMetadata()
            .getFinishReason();

        if (responseRedactor != null) {
            deltaText = responseRedactor.push(chunkText);

            if (finishReason != null) {
                deferredFinishReason.set(finishReason);
            }

            finishReason = null;
        }

        return streamChunkOf(effectiveRequest.model(), deltaText, finishReason);
    }

    private static AiGatewayChatCompletionResponse streamChunkOf(
        String model, @Nullable String deltaText, @Nullable String finishReason) {
        AiGatewayChatMessage delta = new AiGatewayChatMessage(AiGatewayChatRole.ASSISTANT, deltaText);

        AiGatewayChatCompletionResponse.Choice choice =
            new AiGatewayChatCompletionResponse.Choice(0, delta, finishReason);

        return new AiGatewayChatCompletionResponse(
            UUID.randomUUID()
                .toString(),
            "chat.completion.chunk",
            System.currentTimeMillis() / 1000, model, List.of(choice), null);
    }

    private void finalizeStreamRequest(
        reactor.core.publisher.SignalType signalType, AtomicLong streamInputTokens, AtomicLong streamOutputTokens,
        AtomicReference<Throwable> streamError, AiGatewayChatCompletionRequest effectiveRequest, Long workspaceId,
        AiModel model, AiGatewayProvider provider,
        AiGatewayProject project, long startTime) {
        int inputTokens = (int) Math.min(streamInputTokens.get(), Integer.MAX_VALUE);
        int outputTokens = (int) Math.min(streamOutputTokens.get(), Integer.MAX_VALUE);

        AiLlmUsage requestLog = new AiLlmUsage(
            UUID.randomUUID()
                .toString(),
            effectiveRequest.model());

        requestLog.setRoutedModel(model.getName());
        requestLog.setRoutedProvider(provider.getType()
            .name());
        requestLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        requestLog.setInputTokens(inputTokens);
        requestLog.setOutputTokens(outputTokens);

        switch (signalType) {
            case ON_COMPLETE -> requestLog.setStatus(200);
            case ON_ERROR -> {
                requestLog.setStatus(500);

                Throwable error = streamError.get();

                requestLog.setErrorMessage(
                    error != null ? error.getMessage() : "Stream completed with error");
            }
            case CANCEL -> {
                requestLog.setStatus(499);
                requestLog.setErrorMessage("Client disconnected");
            }
            default -> requestLog.setStatus(500);
        }

        if (signalType == reactor.core.publisher.SignalType.CANCEL) {
            requestLog.setCost(null);
        } else {
            try {
                BigDecimal cost = aiGatewayCostCalculator.calculateCost(
                    model, inputTokens, outputTokens);

                requestLog.setCost(cost);
            } catch (IllegalStateException illegalStateException) {
                log.error("Failed to calculate cost for streaming model '{}' — " +
                    "using model's configured rates as fallback to prevent budget bypass",
                    effectiveRequest.model(), illegalStateException);

                BigDecimal fallbackCost = calculateFallbackCost(model, inputTokens, outputTokens);

                requestLog.setCost(fallbackCost);
            }
        }

        setProjectIdFromProject(requestLog, project);
        setApiKeyIdFromAuthentication(requestLog);

        try {
            transactionTemplate.executeWithoutResult(
                status -> aiGatewayRequestLogService.create(requestLog, workspaceId));
        } catch (Exception exception) {
            recordRequestLogPersistFailure("streaming", "success");

            log.error("Failed to persist request log for streaming model '{}'. " +
                "Alert on ai_gateway.request_log.persist_failure{{kind=streaming}}.",
                effectiveRequest.model(), exception);
        }

        enforcePostRequestBudget(effectiveRequest.tags());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayEmbeddingResponse embedding(AiGatewayEmbeddingRequest request) {
        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        Long projectId = resolveProjectId(request.tags());

        List<String> guardrailedInputs = aiGatewayGuardrails.applyToInputs(request.input(), workspaceId, projectId);

        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution = resolveModel(request.model());

        AiGatewayProvider provider = modelResolution.provider();
        AiModel model = modelResolution.model();

        EmbeddingModel embeddingModel = aiGatewayEmbeddingModelFactory.getEmbeddingModel(provider);

        EmbeddingRequest embeddingRequest = new EmbeddingRequest(
            guardrailedInputs,
            org.springframework.ai.embedding.EmbeddingOptions.builder()
                .model(model.getName())
                .build());

        try {
            EmbeddingResponse embeddingResponse = embeddingModel.call(embeddingRequest);

            List<AiGatewayEmbeddingResponse.EmbeddingData> embeddingDataList =
                embeddingResponse.getResults()
                    .stream()
                    .map(embedding -> {
                        List<Float> values = new ArrayList<>();

                        for (float value : embedding.getOutput()) {
                            values.add(value);
                        }

                        return new AiGatewayEmbeddingResponse.EmbeddingData(
                            "embedding", embedding.getIndex(), values);
                    })
                    .toList();

            int promptTokens = 0;

            if (embeddingResponse.getMetadata() != null && embeddingResponse.getMetadata()
                .getUsage() != null) {
                promptTokens = (int) Math.min(
                    embeddingResponse.getMetadata()
                        .getUsage()
                        .getPromptTokens(),
                    Integer.MAX_VALUE);
            }

            AiLlmUsage requestLog = new AiLlmUsage(
                UUID.randomUUID()
                    .toString(),
                request.model());

            requestLog.setRoutedModel(model.getName());
            requestLog.setRoutedProvider(provider.getType()
                .name());
            requestLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            requestLog.setInputTokens(promptTokens);
            requestLog.setStatus(200);

            BigDecimal cost = aiGatewayCostCalculator.calculateCost(model, promptTokens, 0);

            requestLog.setCost(cost);

            setApiKeyIdFromAuthentication(requestLog);

            try {
                aiGatewayRequestLogService.create(requestLog, workspaceId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("embedding", "success");

                log.error("Failed to persist request log for embedding model '{}' — " +
                    "cost of ${} will be missing from spend tracking. " +
                    "Alert on ai_gateway.request_log.persist_failure{{kind=embedding}}.",
                    request.model(), requestLog.getCost(), logException);
            }

            enforcePostRequestBudget(request.tags());

            AiGatewayChatCompletionResponse.GatewayMetadata embeddingMetadata =
                new AiGatewayChatCompletionResponse.GatewayMetadata(
                    provider.getType()
                        .name()
                        .toLowerCase(),
                    model.getName(),
                    System.currentTimeMillis() - startTime,
                    null,
                    null,
                    UUID.randomUUID()
                        .toString(),
                    resolveBudgetWarningRemaining(request.tags()));

            AiGatewayEmbeddingResponse embeddingResult = new AiGatewayEmbeddingResponse(
                "list",
                embeddingDataList,
                request.model(),
                new AiGatewayEmbeddingResponse.Usage(promptTokens, promptTokens),
                embeddingMetadata);

            return embeddingResult;
        } catch (Exception exception) {
            AiLlmUsage errorLog = new AiLlmUsage(
                UUID.randomUUID()
                    .toString(),
                request.model());

            errorLog.setRoutedModel(model.getName());
            errorLog.setRoutedProvider(provider.getType()
                .name());
            errorLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            errorLog.setStatus(resolveErrorStatus(exception));
            errorLog.setErrorMessage(exception.getMessage());

            setApiKeyIdFromAuthentication(errorLog);

            try {
                aiGatewayRequestLogService.create(errorLog, workspaceId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("embedding", "error");

                log.error("Failed to log embedding error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    private AiGatewayChatCompletionResponse chatCompletionDirect(
        AiGatewayChatCompletionRequest request) {
        if (isCacheable(request)) {
            String cacheKey = aiGatewayResponseCache.computeCacheKey(request, null);
            AiGatewayChatCompletionResponse cached = aiGatewayResponseCache.get(cacheKey);

            if (cached != null) {
                return cached;
            }
        }

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution = resolveModel(request.model());

        AiGatewayProvider provider = modelResolution.provider();
        AiModel model = modelResolution.model();

        AiGatewayProject project = resolveProject(request.tags());

        ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

        AiGatewayChatCompletionRequest processedRequest = compressMessages(request, model, project);

        Prompt prompt = buildPrompt(processedRequest, model.getName());

        try {
            ChatResponse chatResponse = chatModel.call(prompt);

            int[] tokenCounts = extractTokenCounts(chatResponse);

            AiLlmUsage requestLog = createSuccessLog(
                request, model, provider, startTime, tokenCounts[0], tokenCounts[1]);

            setProjectIdFromProject(requestLog, project);

            try {
                aiGatewayRequestLogService.create(requestLog, workspaceId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("chat", "success");

                log.error("Failed to persist request log for chat completion model '{}' — " +
                    "cost of ${} will be missing from spend tracking. " +
                    "Alert on ai_gateway.request_log.persist_failure{{kind=chat}}.",
                    request.model(), requestLog.getCost(), logException);
            }

            enforcePostRequestBudget(request.tags());

            AiGatewayChatCompletionResponse response = toResponse(chatResponse, request.model());

            if (isCacheable(request)) {
                String cacheKey = aiGatewayResponseCache.computeCacheKey(request, null);

                try {
                    aiGatewayResponseCache.put(cacheKey, response);
                } catch (Exception cacheException) {
                    log.warn(
                        "Failed to cache chat completion response for model '{}' (key={}); " +
                            "request already succeeded, continuing",
                        request.model(), cacheKey, cacheException);
                }
            }

            return response;
        } catch (Exception exception) {
            try {
                AiLlmUsage errorLog = createErrorLog(request, startTime, exception);

                setProjectIdFromProject(errorLog, project);

                aiGatewayRequestLogService.create(errorLog, workspaceId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("chat", "error");

                log.error("Failed to log chat completion error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    private AiGatewayChatCompletionResponse chatCompletionWithRouting(
        AiGatewayChatCompletionRequest request) {
        if (isCacheable(request)) {
            String cacheKey = aiGatewayResponseCache.computeCacheKey(request, null);
            AiGatewayChatCompletionResponse cached = aiGatewayResponseCache.get(cacheKey);

            if (cached != null) {
                return cached;
            }
        }

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        long startTime = System.currentTimeMillis();

        AiGatewayRoutingPolicy routingPolicy =
            aiGatewayRoutingPolicyService.getRoutingPolicyByName(request.routingPolicy());

        List<AiGatewayModelDeployment> deployments =
            aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(routingPolicy.getId());

        if (deployments.isEmpty()) {
            throw new IllegalStateException(
                "No deployments configured for routing policy: " + routingPolicy.getName());
        }

        Map<Long, AiModel> modelMap = deployments.stream()
            .map(AiGatewayModelDeployment::getModelId)
            .distinct()
            .collect(Collectors.toMap(
                modelId -> modelId,
                modelId -> aiModelService.getModel(modelId)));

        List<AiGatewayModelDeployment> enabledDeployments = deployments.stream()
            .filter(AiGatewayModelDeployment::isEnabled)
            .toList();

        if (enabledDeployments.isEmpty()) {
            throw new IllegalStateException(
                "No enabled deployments for routing policy: " + routingPolicy.getName());
        }

        AiGatewayProject project = resolveProject(request.tags());

        AiGatewayRoutingContext routingContext = buildRoutingContext(request, modelMap);

        AiGatewayModelDeployment primaryDeployment = aiGatewayRouter.route(
            routingPolicy.getStrategy(), enabledDeployments, routingContext);

        List<AiGatewayModelDeployment> orderedDeployments = new ArrayList<>();

        orderedDeployments.add(primaryDeployment);

        for (AiGatewayModelDeployment deployment : enabledDeployments) {
            if (!deployment.getId()
                .equals(primaryDeployment.getId())) {
                orderedDeployments.add(deployment);
            }
        }

        try {
            AiGatewayChatCompletionResponse response =
                aiGatewayRetryHandler.executeWithRetry(orderedDeployments, deployment -> {
                    AiModel model = modelMap.get(deployment.getModelId());
                    AiGatewayProvider provider = aiGatewayProviderService.getProvider(model.getProviderId());

                    ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

                    AiGatewayChatCompletionRequest processedRequest = compressMessages(request, model, project);

                    Prompt prompt = buildPrompt(processedRequest, model.getName());

                    ChatResponse chatResponse = chatModel.call(prompt);

                    int[] tokenCounts = extractTokenCounts(chatResponse);

                    AiLlmUsage requestLog = createSuccessLog(
                        request, model, provider, startTime, tokenCounts[0], tokenCounts[1]);

                    setProjectIdFromProject(requestLog, project);

                    requestLog.setRoutingPolicyId(routingPolicy.getId());
                    requestLog.setRoutingStrategy(
                        routingPolicy.getStrategy() == null ? null : routingPolicy.getStrategy()
                            .ordinal());

                    try {
                        aiGatewayRequestLogService.create(requestLog, workspaceId);
                    } catch (Exception logException) {
                        recordRequestLogPersistFailure("routing", "success");

                        log.error("Failed to persist request log for routed model '{}' — " +
                            "cost of ${} will be missing from spend tracking. " +
                            "Alert on ai_gateway.request_log.persist_failure{{kind=routing}}.",
                            request.model(), requestLog.getCost(), logException);
                    }

                    enforcePostRequestBudget(request.tags());

                    return toResponse(chatResponse, request.model());
                });

            if (isCacheable(request)) {
                String cacheKey = aiGatewayResponseCache.computeCacheKey(request, null);

                try {
                    aiGatewayResponseCache.put(cacheKey, response);
                } catch (Exception cacheException) {
                    log.warn(
                        "Failed to cache routed chat completion response for model '{}' (key={}); " +
                            "request already succeeded, continuing",
                        request.model(), cacheKey, cacheException);
                }
            }

            return response;
        } catch (Exception exception) {
            try {
                AiLlmUsage errorLog = createErrorLog(request, startTime, exception);

                setProjectIdFromProject(errorLog, project);

                errorLog.setRoutingPolicyId(routingPolicy.getId());
                errorLog.setRoutingStrategy(routingPolicy.getStrategy() == null ? null : routingPolicy.getStrategy()
                    .ordinal());

                aiGatewayRequestLogService.create(errorLog, workspaceId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("routing", "error");

                log.error("Failed to log routing error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    private void checkBudget(Map<String, String> tags, String model) {
        if (tags == null || !tags.containsKey("workspace_id")) {
            throw new IllegalArgumentException(
                "Request for model '" + model + "' is missing required 'workspace_id' tag");
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            validateWorkspaceAccess(workspaceId);

            AiGatewayBudgetChecker.BudgetCheckResult budgetResult =
                aiGatewayBudgetChecker.checkBudget(workspaceId);

            if (!budgetResult.requestAllowed()) {
                AiGatewayMetrics metrics = aiGatewayMetricsProvider.getIfAvailable();

                if (metrics != null) {
                    metrics.onBudgetExceeded(new AiGatewayBudgetExceededEvent(
                        workspaceId, model, budgetResult.currentSpend(), budgetResult.budgetAmount()));
                }

                throw new BudgetExceededException(
                    "Budget limit exceeded for workspace " + workspaceId +
                        ". Current spend: $" + budgetResult.currentSpend() +
                        " / Budget: $" + budgetResult.budgetAmount(),
                    Money.usd(budgetResult.budgetAmount()), Money.usd(budgetResult.currentSpend()));
            }

            if (budgetResult.thresholdWarning()) {
                log.warn(
                    "Budget threshold warning for workspace {}: current spend ${} / budget ${} ({}%)",
                    workspaceId, budgetResult.currentSpend(), budgetResult.budgetAmount(),
                    budgetResult.usagePercentage());
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(
                "Invalid workspace_id tag: " + tags.get("workspace_id"), numberFormatException);
        }
    }

    private void checkRateLimits(Map<String, String> tags) {
        if (aiGatewayRateLimitChecker == null || tags == null || !tags.containsKey("workspace_id")) {
            return;
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            validateWorkspaceAccess(workspaceId);

            Long projectId = tags.containsKey("project_id") ? Long.parseLong(tags.get("project_id")) : null;
            String userId = tags.get("user_id");

            aiGatewayRateLimitChecker.checkRateLimits(workspaceId, projectId, userId, tags);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(
                "Invalid numeric tag value during rate limit check " +
                    "(workspace_id/project_id must parse as long)",
                numberFormatException);
        }
    }

    private void enforcePostRequestBudget(Map<String, String> tags) {
        if (tags == null || !tags.containsKey("workspace_id")) {
            log.error(
                "Missing workspace_id tag during post-request budget enforcement — " +
                    "this indicates a bug in the request pipeline");

            return;
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            aiGatewayBudgetChecker.recordSpendAndEnforce(workspaceId);
        } catch (RuntimeException exception) {
            AiGatewayMetrics metrics = aiGatewayMetricsProvider.getIfAvailable();

            if (metrics != null) {
                metrics.incrementPostRequestBudgetFailure();
            }

            log.error(
                "Post-request budget enforcement failed for workspace_id={} — " +
                    "pre-request budget checking remains active via the budget checker cache. " +
                    "Alert on ai_gateway.budget.post_request_failure counter.",
                tags.get("workspace_id"), exception);
        }
    }

    private Prompt buildPrompt(AiGatewayChatCompletionRequest request, String modelName) {
        List<Message> messages = request.messages()
            .stream()
            .map(this::toSpringAiMessage)
            .toList();

        if (request.tools() != null && !request.tools()
            .isEmpty()) {
            OpenAiChatOptions.Builder openAiOptionsBuilder = OpenAiChatOptions.builder()
                .model(modelName);

            if (request.temperature() != null) {
                openAiOptionsBuilder.temperature(request.temperature());
            }

            if (request.maxTokens() != null) {
                openAiOptionsBuilder.maxTokens(request.maxTokens());
            }

            if (request.topP() != null) {
                openAiOptionsBuilder.topP(request.topP());
            }

            List<Map<String, Object>> functionTools = request.tools()
                .stream()
                .map(tool -> Map.<String, Object>of(
                    "type", "function",
                    "function", Map.of(
                        "name", tool.function()
                            .name(),
                        "description", tool.function()
                            .description(),
                        "parameters", tool.function()
                            .parameters())))
                .toList();

            openAiOptionsBuilder.extraBody(Map.of("tools", functionTools));

            return new Prompt(messages, openAiOptionsBuilder.build());
        }

        ChatOptions.Builder<?> chatOptionsBuilder = ChatOptions.builder()
            .model(modelName);

        if (request.temperature() != null) {
            chatOptionsBuilder.temperature(request.temperature());
        }

        if (request.maxTokens() != null) {
            chatOptionsBuilder.maxTokens(request.maxTokens());
        }

        if (request.topP() != null) {
            chatOptionsBuilder.topP(request.topP());
        }

        return new Prompt(messages, chatOptionsBuilder.build());
    }

    private AiGatewayRoutingContext buildRoutingContext(
        AiGatewayChatCompletionRequest request, Map<Long, AiModel> modelMap) {
        Map<String, Double> latencyByModelName = aiGatewayRequestLogService.getAverageLatencyByModel(
            Instant.now()
                .minus(Duration.ofHours(1)));

        Map<Long, Double> averageLatencyByModelId = modelMap.entrySet()
            .stream()
            .filter(entry -> latencyByModelName.containsKey(entry.getValue()
                .getName()))
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> latencyByModelName.get(entry.getValue()
                .getName())));

        double promptComplexityScore;

        try {
            promptComplexityScore = promptComplexityScorer.score(request);
        } catch (Exception exception) {
            log.warn("Prompt complexity scoring failed; falling back to most capable tier", exception);

            promptComplexityScore = 1.0;
        }

        Map<String, String> tags = request.tags() != null ? request.tags() : Map.of();

        Map<Long, String> providerTypeByModelId = modelMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    AiGatewayProvider provider = aiGatewayProviderService.getProvider(entry.getValue()
                        .getProviderId());

                    return provider.getType()
                        .name();
                }));

        return new AiGatewayRoutingContext(
            averageLatencyByModelId, modelMap, promptComplexityScore, providerTypeByModelId, tags);
    }

    private RoutedDeployments selectRoutedDeployments(AiGatewayChatCompletionRequest request) {
        AiGatewayRoutingPolicy routingPolicy =
            aiGatewayRoutingPolicyService.getRoutingPolicyByName(request.routingPolicy());

        List<AiGatewayModelDeployment> deployments =
            aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(routingPolicy.getId());

        List<AiGatewayModelDeployment> enabledDeployments = deployments.stream()
            .filter(AiGatewayModelDeployment::isEnabled)
            .toList();

        if (enabledDeployments.isEmpty()) {
            throw new IllegalStateException(
                "No enabled deployments for routing policy: " + routingPolicy.getName());
        }

        Map<Long, AiModel> modelMap = enabledDeployments.stream()
            .map(AiGatewayModelDeployment::getModelId)
            .distinct()
            .collect(Collectors.toMap(modelId -> modelId, aiModelService::getModel));

        AiGatewayRoutingContext routingContext = buildRoutingContext(request, modelMap);

        AiGatewayModelDeployment primaryDeployment = aiGatewayRouter.route(
            routingPolicy.getStrategy(), enabledDeployments, routingContext);

        List<AiGatewayModelDeployment> orderedDeployments = new ArrayList<>();

        orderedDeployments.add(primaryDeployment);

        for (AiGatewayModelDeployment deployment : enabledDeployments) {
            if (!deployment.getId()
                .equals(primaryDeployment.getId())) {
                orderedDeployments.add(deployment);
            }
        }

        return new RoutedDeployments(orderedDeployments, modelMap);
    }

    private record RoutedDeployments(
        List<AiGatewayModelDeployment> orderedDeployments, Map<Long, AiModel> modelMap) {
    }

    private record StreamModelResolution(
        ModelResolution modelResolution, @Nullable RoutedDeployments routedDeployments) {
    }

    private StreamModelResolution resolveStreamModelResolution(
        AiGatewayChatCompletionRequest effectiveRequest, Long workspaceId, long startTime) {
        RoutedDeployments routedDeployments = null;
        ModelResolution modelResolution;

        try {
            if (effectiveRequest.routingPolicy() != null) {
                routedDeployments = selectRoutedDeployments(effectiveRequest);

                AiGatewayModelDeployment primaryDeployment = routedDeployments.orderedDeployments()
                    .get(0);
                AiModel primaryModel = routedDeployments.modelMap()
                    .get(primaryDeployment.getModelId());
                AiGatewayProvider primaryProvider = aiGatewayProviderService.getProvider(primaryModel.getProviderId());

                modelResolution = new ModelResolution(primaryProvider, primaryModel);
            } else {
                modelResolution = resolveModel(effectiveRequest.model());
            }
        } catch (Exception exception) {
            try {
                aiGatewayRequestLogService.create(createErrorLog(effectiveRequest, startTime, exception), workspaceId);
            } catch (Exception logException) {
                log.error("Failed to log streaming setup error for model '{}'. Original error: {}",
                    effectiveRequest.model(), exception.getMessage(), logException);
            }

            throw exception;
        }

        return new StreamModelResolution(modelResolution, routedDeployments);
    }

    private ModelResolution resolveModel(String modelIdentifier) {
        String[] parts = modelIdentifier.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Model must be in format 'provider/model', got: " + modelIdentifier);
        }

        String providerTypeName = parts[0].toUpperCase()
            .replace("-", "_");
        String modelName = parts[1];

        AiGatewayProvider provider = resolveTenantProviderByTypeName(providerTypeName);

        AiModel model = aiModelService.getModel(provider.getId(), modelName);

        return new ModelResolution(provider, model);
    }

    private AiGatewayProvider resolveTenantProviderByTypeName(String providerTypeName) {
        AiGatewayProviderType providerType;

        try {
            providerType = AiGatewayProviderType.valueOf(providerTypeName);
        } catch (IllegalArgumentException unknownProviderType) {
            throw new IllegalArgumentException(
                "No enabled provider found for type: " + providerTypeName, unknownProviderType);
        }

        return aiGatewayProviderResolver.resolveTenantProvider(providerType);
    }

    private AiGatewayChatCompletionRequest compressMessages(
        AiGatewayChatCompletionRequest request, AiModel model,
        @Nullable AiGatewayProject project) {
        if (!resolveCompressionEnabled(project)) {
            return request;
        }

        if (model.getContextWindow() == null) {
            return request;
        }

        int targetTokens = (int) (model.getContextWindow() * CONTEXT_WINDOW_USAGE_RATIO);

        List<AiGatewayChatMessage> compressedMessages =
            aiGatewayContextCompressor.compress(request.messages(), targetTokens);

        return new AiGatewayChatCompletionRequest(
            request.model(), compressedMessages, request.temperature(), request.maxTokens(),
            request.topP(), request.stream(), request.routingPolicy(), request.cache(),
            request.toolChoice(), request.tools(), request.tags());
    }

    private Message toSpringAiMessage(AiGatewayChatMessage chatMessage) {
        return switch (chatMessage.role()) {
            case SYSTEM -> new SystemMessage(chatMessage.content());
            case ASSISTANT -> {
                if (chatMessage.toolCalls() != null && !chatMessage.toolCalls()
                    .isEmpty()) {
                    List<AssistantMessage.ToolCall> springToolCalls = chatMessage.toolCalls()
                        .stream()
                        .map(
                            toolCall -> new AssistantMessage.ToolCall(
                                toolCall.id(), toolCall.type(), toolCall.function()
                                    .name(),
                                toolCall.function()
                                    .arguments()))
                        .toList();

                    yield AssistantMessage.builder()
                        .content(chatMessage.content() != null ? chatMessage.content() : "")
                        .toolCalls(springToolCalls)
                        .build();
                }

                yield new AssistantMessage(chatMessage.content());
            }
            case TOOL -> {
                ToolResponseMessage.ToolResponse toolResponse = new ToolResponseMessage.ToolResponse(
                    chatMessage.toolCallId(), null,
                    chatMessage.content() != null ? chatMessage.content() : "");

                yield ToolResponseMessage.builder()
                    .responses(List.of(toolResponse))
                    .build();
            }
            case USER -> {
                if (chatMessage.hasContentBlocks()) {
                    yield buildMultimodalUserMessage(chatMessage);
                }

                yield new UserMessage(chatMessage.content());
            }
        };
    }

    private AiGatewayChatCompletionResponse toResponse(ChatResponse chatResponse, String requestedModel) {
        Generation generation = chatResponse.getResult();

        if (generation == null) {
            return new AiGatewayChatCompletionResponse(
                UUID.randomUUID()
                    .toString(),
                "chat.completion",
                System.currentTimeMillis() / 1000, requestedModel, List.of(), null);
        }

        AssistantMessage assistantMessage = generation.getOutput();

        List<AiGatewayChatMessage.ToolCall> toolCalls = null;

        if (assistantMessage.hasToolCalls()) {
            toolCalls = assistantMessage.getToolCalls()
                .stream()
                .map(
                    toolCall -> new AiGatewayChatMessage.ToolCall(
                        toolCall.id(), toolCall.type(),
                        new AiGatewayChatMessage.ToolCallFunction(
                            toolCall.name(), toolCall.arguments())))
                .toList();
        }

        AiGatewayChatMessage responseMessage = new AiGatewayChatMessage(
            AiGatewayChatRole.ASSISTANT, assistantMessage.getText(), toolCalls, null);

        AiGatewayChatCompletionResponse.Choice choice = new AiGatewayChatCompletionResponse.Choice(
            0,
            responseMessage,
            generation.getMetadata()
                .getFinishReason());

        AiGatewayChatCompletionResponse.Usage responseUsage = null;

        if (chatResponse.getMetadata() != null && chatResponse.getMetadata()
            .getUsage() != null) {
            org.springframework.ai.chat.metadata.Usage chatResponseUsage = chatResponse.getMetadata()
                .getUsage();

            responseUsage = new AiGatewayChatCompletionResponse.Usage(
                chatResponseUsage.getPromptTokens(),
                chatResponseUsage.getCompletionTokens(),
                chatResponseUsage.getTotalTokens());
        }

        return new AiGatewayChatCompletionResponse(
            UUID.randomUUID()
                .toString(),
            "chat.completion",
            System.currentTimeMillis() / 1000,
            requestedModel,
            List.of(choice),
            responseUsage);
    }

    private UserMessage buildMultimodalUserMessage(AiGatewayChatMessage chatMessage) {
        StringBuilder textContent = new StringBuilder();
        List<Media> mediaList = new ArrayList<>();

        for (AiGatewayContentBlock block : chatMessage.contentBlocks()) {
            switch (block.type()) {
                case TEXT -> textContent.append(block.text());
                case IMAGE_URL -> {
                    if (block.imageUrl() != null) {
                        String validatedUrl = validateExternalUrl(block.imageUrl()
                            .url());

                        try {
                            mediaList.add(
                                new Media(MimeTypeUtils.IMAGE_PNG, new UrlResource(validatedUrl)));
                        } catch (Exception exception) {
                            throw new IllegalArgumentException(
                                "Invalid image URL: " + block.imageUrl()
                                    .url(),
                                exception);
                        }
                    }
                }
                case IMAGE -> {
                    if (block.imageUrl() != null && block.imageUrl()
                        .url() != null) {
                        String url = block.imageUrl()
                            .url();

                        if (url.startsWith("data:")) {
                            String[] parts = url.split(",", 2);

                            if (parts.length == 2) {
                                String mimeTypePart = parts[0].replace("data:", "")
                                    .replace(";base64", "");
                                byte[] imageBytes = EncodingUtils.base64Decode(parts[1]);

                                mediaList.add(
                                    new Media(
                                        MimeTypeUtils.parseMimeType(mimeTypePart),
                                        new ByteArrayResource(imageBytes)));
                            } else {
                                throw new IllegalArgumentException(
                                    "Image block has malformed data URI — expected 'data:<mime>;base64,<data>' format");
                            }
                        } else {
                            String validatedUrl = validateExternalUrl(url);

                            try {
                                mediaList.add(
                                    new Media(MimeTypeUtils.IMAGE_PNG, new UrlResource(validatedUrl)));
                            } catch (Exception exception) {
                                throw new IllegalArgumentException("Invalid image URL: " + url, exception);
                            }
                        }
                    }
                }
                case DOCUMENT -> {
                    if (block.document() != null) {
                        if ("base64".equals(block.document()
                            .sourceType())) {
                            byte[] documentBytes = EncodingUtils.base64Decode(block.document()
                                .data());
                            String mediaType =
                                block.document()
                                    .mediaType() != null ? block.document()
                                        .mediaType() : "application/pdf";

                            mediaList.add(
                                new Media(
                                    MimeTypeUtils.parseMimeType(mediaType),
                                    new ByteArrayResource(documentBytes)));
                        } else if ("url".equals(block.document()
                            .sourceType())
                            && block.document()
                                .url() != null) {
                            String validatedDocumentUrl = validateExternalUrl(block.document()
                                .url());

                            try {
                                String mediaType =
                                    block.document()
                                        .mediaType() != null
                                            ? block.document()
                                                .mediaType()
                                            : "application/pdf";

                                mediaList.add(
                                    new Media(
                                        MimeTypeUtils.parseMimeType(mediaType),
                                        new UrlResource(validatedDocumentUrl)));
                            } catch (Exception exception) {
                                throw new IllegalArgumentException(
                                    "Invalid document URL: " + block.document()
                                        .url(),
                                    exception);
                            }
                        } else {
                            throw new IllegalArgumentException(
                                "Document block has unsupported sourceType: " + block.document()
                                    .sourceType() + " — supported: 'base64', 'url'");
                        }
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported content block type: " + block.type());
            }
        }

        if (mediaList.isEmpty()) {
            return new UserMessage(textContent.toString());
        }

        return UserMessage.builder()
            .text(textContent.toString())
            .media(mediaList)
            .build();
    }

    @Nullable
    private AiGatewayProject resolveProject(Map<String, String> tags) {
        String projectSlug = tags != null ? tags.get("project_id") : null;
        String workspaceId = tags != null ? tags.get("workspace_id") : null;

        if (projectSlug == null || projectSlug.isBlank() || workspaceId == null) {
            return null;
        }

        return workspaceAiGatewayProjectService.fetchProjectByWorkspaceIdAndSlug(
            Long.parseLong(workspaceId), projectSlug)
            .orElse(null);
    }

    private @Nullable Long resolveProjectId(Map<String, String> tags) {
        AiGatewayProject project = resolveProject(tags);

        return project != null ? project.getId() : null;
    }

    private boolean resolveCompressionEnabled(@Nullable AiGatewayProject project) {
        if (project != null && project.getCompressionEnabled() != null) {
            return project.getCompressionEnabled();
        }

        return true;
    }

    private void setProjectIdFromProject(AiLlmUsage requestLog, @Nullable AiGatewayProject project) {
        if (project != null) {
            requestLog.setProjectId(project.getId());
        }
    }

    private static Long resolveWorkspaceIdFromTags(Map<String, String> tags) {
        if (tags == null || !tags.containsKey("workspace_id")) {
            return null;
        }

        return Long.parseLong(tags.get("workspace_id"));
    }

    private int[] extractTokenCounts(ChatResponse chatResponse) {
        int inputTokens = 0;
        int outputTokens = 0;

        if (chatResponse.getMetadata() != null && chatResponse.getMetadata()
            .getUsage() != null) {
            inputTokens = (int) Math.min(
                chatResponse.getMetadata()
                    .getUsage()
                    .getPromptTokens(),
                Integer.MAX_VALUE);
            outputTokens = (int) Math.min(
                chatResponse.getMetadata()
                    .getUsage()
                    .getCompletionTokens(),
                Integer.MAX_VALUE);
        }

        return new int[] {
            inputTokens, outputTokens
        };
    }

    private AiLlmUsage createSuccessLog(
        AiGatewayChatCompletionRequest request, AiModel model, AiGatewayProvider provider,
        long startTime, int inputTokens, int outputTokens) {
        AiLlmUsage requestLog = new AiLlmUsage(
            UUID.randomUUID()
                .toString(),
            request.model());

        requestLog.setRoutedModel(model.getName());
        requestLog.setRoutedProvider(provider.getType()
            .name());
        requestLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        requestLog.setStatus(200);
        requestLog.setInputTokens(inputTokens);
        requestLog.setOutputTokens(outputTokens);

        BigDecimal cost = aiGatewayCostCalculator.calculateCost(model, inputTokens, outputTokens);

        requestLog.setCost(cost);

        setApiKeyIdFromAuthentication(requestLog);

        return requestLog;
    }

    private static void setApiKeyIdFromAuthentication(AiLlmUsage requestLog) {
        Long apiKeyId = resolveAuthenticatedApiKeyId();

        if (apiKeyId != null) {
            requestLog.setApiKeyId(apiKeyId);
        }
    }

    private static Long resolveAuthenticatedApiKeyId() {
        org.springframework.security.core.context.SecurityContext context = SecurityContextHolder.getContext();

        if (context == null) {
            return null;
        }

        org.springframework.security.core.Authentication authentication = context.getAuthentication();

        if (authentication instanceof com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken token) {
            return token.getApiKeyId();
        }

        return null;
    }

    private AiLlmUsage createErrorLog(
        AiGatewayChatCompletionRequest request, long startTime, Exception exception) {
        AiLlmUsage errorLog = new AiLlmUsage(
            UUID.randomUUID()
                .toString(),
            request.model());

        errorLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        errorLog.setStatus(resolveErrorStatus(exception));
        errorLog.setErrorMessage(exception.getMessage());

        setApiKeyIdFromAuthentication(errorLog);

        return errorLog;
    }

    private static BigDecimal calculateFallbackCost(AiModel model, int inputTokens, int outputTokens) {
        BigDecimal millionTokens = BigDecimal.valueOf(1_000_000);

        BigDecimal inputRate = model.getInputCostPerMTokens();
        BigDecimal outputRate = model.getOutputCostPerMTokens();

        if (inputRate == null || outputRate == null) {
            log.error(
                "Model '{}' (id={}) has incomplete cost configuration (input={}, output={}) — " +
                    "using conservative default rate of ${}/M tokens for missing rates to prevent budget bypass. " +
                    "Configure pricing to fix this.",
                model.getName(), model.getId(), inputRate, outputRate, DEFAULT_FALLBACK_COST_PER_M_TOKENS);

            if (inputRate == null) {
                inputRate = DEFAULT_FALLBACK_COST_PER_M_TOKENS;
            }

            if (outputRate == null) {
                outputRate = DEFAULT_FALLBACK_COST_PER_M_TOKENS;
            }
        }

        BigDecimal inputCost = inputRate
            .multiply(BigDecimal.valueOf(inputTokens))
            .divide(millionTokens, 10, java.math.RoundingMode.HALF_UP);

        BigDecimal outputCost = outputRate
            .multiply(BigDecimal.valueOf(outputTokens))
            .divide(millionTokens, 10, java.math.RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }

    private static int resolveErrorStatus(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return 400;
        }

        if (exception instanceof BudgetExceededException) {
            return 429;
        }

        if (exception instanceof IllegalStateException) {
            return 503;
        }

        return 500;
    }

    private boolean isWorkspaceCachingEnabled(@Nullable Map<String, String> tags) {
        if (tags == null || !tags.containsKey("workspace_id")) {
            return true;
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            return aiGatewayWorkspaceSettingsService.findByWorkspaceId(workspaceId)
                .map(settings -> settings.cacheEnabled() == null || Boolean.TRUE.equals(settings.cacheEnabled()))
                .orElse(true);
        } catch (NumberFormatException malformed) {
            return true;
        }
    }

    private boolean isCacheable(AiGatewayChatCompletionRequest request) {
        return isWorkspaceCachingEnabled(request.tags()) && aiGatewayResponseCache.shouldCache(request)
            && !containsPiiToken(request);
    }

    private static boolean containsPiiToken(AiGatewayChatCompletionRequest request) {
        for (AiGatewayChatMessage message : request.messages()) {
            String content = message.content();

            if (content != null && PiiToken.pattern()
                .matcher(content)
                .find()) {
                return true;
            }
        }

        return false;
    }

    static String validateExternalUrl(String url) {
        AiGatewayUrlValidator.validateExternalUrl(url);

        return url;
    }

    private record ModelResolution(AiGatewayProvider provider, AiModel model) {
    }
}
