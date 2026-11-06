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
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.gateway.cache.AiGatewayResponseCache;
import com.bytechef.ee.platform.ai.gateway.compression.AiGatewayContextCompressor;
import com.bytechef.ee.platform.ai.gateway.cost.AiGatewayCostCalculator;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayConnectedUserSettings;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderScopeViolationException;
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
import com.bytechef.ee.platform.ai.gateway.event.AiGatewayTraceCompletedEvent;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayProviderResolver;
import com.bytechef.ee.platform.ai.gateway.reliability.AiGatewayRetryHandler;
import com.bytechef.ee.platform.ai.gateway.routing.AiGatewayRouter;
import com.bytechef.ee.platform.ai.gateway.routing.AiGatewayRoutingContext;
import com.bytechef.ee.platform.ai.gateway.routing.PromptComplexityScorer;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayConnectedUserSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayEmbeddedSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelDeploymentService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayConstraintMatchers;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayUrlValidator;
import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceTag;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityTracingHeaders;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySessionService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
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

    private static final String TRACE_DEDUP_INDEX = "uq_ai_obs_trace_ext_trace_id";
    private static final Pattern TRACE_DEDUP_INDEX_PATTERN =
        AiGatewayConstraintMatchers.wordBoundaryPattern(TRACE_DEDUP_INDEX);

    private final AiGatewayBudgetChecker aiGatewayBudgetChecker;
    private final @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    private final AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService;
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
    private final AiGatewaySpendService aiGatewaySpendService;
    private final PromptComplexityScorer promptComplexityScorer;
    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final TagService tagService;
    private final AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;
    private final AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final ObjectProvider<AiGatewayMetrics> aiGatewayMetricsProvider;
    private final ObjectProvider<AiGatewayEmbeddedSettingsService> embeddedSettingsServiceProvider;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable PermissionService permissionService;
    private final TransactionTemplate transactionTemplate;

    public AiGatewayFacadeImpl(
        AiGatewayBudgetChecker aiGatewayBudgetChecker,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayChatModelFactory aiGatewayChatModelFactory,
        AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService,
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
        AiGatewaySpendService aiGatewaySpendService,
        PromptComplexityScorer promptComplexityScorer,
        AiObservabilitySessionService aiObservabilitySessionService,
        WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilityTraceService aiObservabilityTraceService,
        TagService tagService,
        AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService,
        AiGuardrailsWorkspaceSettingsService aiGuardrailsWorkspaceSettingsService,
        ObjectProvider<AiGatewayMetrics> aiGatewayMetricsProvider,
        ObjectProvider<AiGatewayEmbeddedSettingsService> embeddedSettingsServiceProvider,
        ApplicationEventPublisher applicationEventPublisher,
        @Nullable PermissionService permissionService,
        PlatformTransactionManager transactionManager) {

        this.aiGatewayBudgetChecker = aiGatewayBudgetChecker;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
        this.aiGatewayConnectedUserSettingsService = aiGatewayConnectedUserSettingsService;
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
        this.aiGatewaySpendService = aiGatewaySpendService;
        this.promptComplexityScorer = promptComplexityScorer;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.workspaceAiObservabilitySessionService = workspaceAiObservabilitySessionService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.tagService = tagService;
        this.aiGatewayWorkspaceSettingsService = aiGatewayWorkspaceSettingsService;
        this.aiGuardrailsWorkspaceSettingsService = aiGuardrailsWorkspaceSettingsService;
        this.aiGatewayMetricsProvider = aiGatewayMetricsProvider;
        this.embeddedSettingsServiceProvider = embeddedSettingsServiceProvider;
        this.applicationEventPublisher = applicationEventPublisher;
        this.permissionService = permissionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders) {

        return chatCompletion(request, tracingHeaders, null);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable Long connectedUserId) {

        if (tracingHeaders == null) {
            tracingHeaders = new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());
        }

        long environmentId = resolveConnectedUserEnvironmentId(connectedUserId);

        checkRequestScope(request.tags(), request.model(), connectedUserId);

        EmbeddedRequestSettings requestSettings = resolveEmbeddedSettingsAndCheckBudget(connectedUserId, environmentId);

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        Long projectId = resolveProjectId(request.tags());

        PiiTokenSession session = aiGatewayGuardrails.newTokenSession();

        try {
            request = aiGatewayGuardrails.apply(
                request, resolvePlatformType(connectedUserId), workspaceId, projectId, session);

            long startTime = System.currentTimeMillis();

            AiGatewayChatCompletionResponse response;
            boolean success = true;

            request = applyRoutingPolicyPrecedence(request, connectedUserId, requestSettings);

            try {
                if (request.routingPolicy() != null) {
                    response = chatCompletionWithRouting(request, connectedUserId);
                } else {
                    response = chatCompletionDirect(request, connectedUserId);
                }
            } catch (Exception exception) {
                success = false;

                processTracingHeaders(
                    tracingHeaders, workspaceId, request, null, startTime, false, connectedUserId);

                throw exception;
            }

            AiGatewayChatCompletionResponse scannedResponse =
                aiGatewayGuardrails.scanResponse(
                    response, resolvePlatformType(connectedUserId), workspaceId, projectId);

            processTracingHeaders(
                tracingHeaders, workspaceId, request, scannedResponse, startTime, success, connectedUserId);

            response = aiGatewayGuardrails.restoreResponse(scannedResponse, session);

            return withGatewayMetadata(response, request, startTime);
        } finally {
            session.close();
        }

    }

    private AiGatewayChatCompletionRequest applyRoutingPolicyPrecedence(
        AiGatewayChatCompletionRequest request, @Nullable Long connectedUserId,
        EmbeddedRequestSettings requestSettings) {

        String requestedRoutingPolicy = request.routingPolicy();

        if (requestedRoutingPolicy != null) {
            requireRequestedRoutingPolicyIsAssignedPlan(requestedRoutingPolicy, connectedUserId, requestSettings);

            return request;
        }

        Long routingPolicyId = resolveAssignedRoutingPolicyId(requestSettings);

        if (routingPolicyId == null) {
            routingPolicyId = resolveModelDefaultRoutingPolicyId(request.model());
        }

        if (routingPolicyId == null) {
            routingPolicyId =
                resolveEmbeddedDefaultRoutingPolicyId(connectedUserId, requestSettings.embeddedSettings());
        }

        return applyResolvedRoutingPolicy(request, routingPolicyId, connectedUserId);
    }

    private @Nullable Long resolveAssignedRoutingPolicyId(EmbeddedRequestSettings requestSettings) {
        Long assignedRoutingPolicyId = requestSettings.connectedUserSettings()
            .map(AiGatewayConnectedUserSettings::getRoutingPolicyId)
            .orElse(null);

        if (assignedRoutingPolicyId == null) {
            return null;
        }

        try {
            AiGatewayRoutingPolicy assignedPolicy =
                aiGatewayRoutingPolicyService.getRoutingPolicy(assignedRoutingPolicyId);

            return assignedPolicy.isEnabled() ? assignedRoutingPolicyId : null;
        } catch (IllegalArgumentException missingPolicy) {
            return null;
        }
    }

    private void requireRequestedRoutingPolicyIsAssignedPlan(
        String requestedRoutingPolicy, @Nullable Long connectedUserId, EmbeddedRequestSettings requestSettings) {

        if (connectedUserId == null) {
            return;
        }

        Long assignedRoutingPolicyId = requestSettings.connectedUserSettings()
            .map(AiGatewayConnectedUserSettings::getRoutingPolicyId)
            .orElse(null);

        if (assignedRoutingPolicyId == null) {
            throw new IllegalArgumentException("Routing policy not found: " + requestedRoutingPolicy);
        }

        AiGatewayRoutingPolicy assignedPolicy = aiGatewayRoutingPolicyService.getRoutingPolicy(assignedRoutingPolicyId);

        if (!requestedRoutingPolicy.equals(assignedPolicy.getName())) {
            throw new IllegalArgumentException("Routing policy not found: " + requestedRoutingPolicy);
        }

        if (assignedPolicy.getWorkspaceId() != null) {
            throw new IllegalArgumentException("Routing policy not found: " + requestedRoutingPolicy);
        }
    }

    private AiGatewayChatCompletionRequest applyResolvedRoutingPolicy(
        AiGatewayChatCompletionRequest request, @Nullable Long routingPolicyId,
        @Nullable Long connectedUserId) {

        if (routingPolicyId == null) {
            return request;
        }

        try {
            AiGatewayRoutingPolicy policy = aiGatewayRoutingPolicyService.getRoutingPolicy(routingPolicyId);

            if (connectedUserId != null && policy.getWorkspaceId() != null) {
                log.warn(
                    "Resolved routing_policy_id={} for model '{}' belongs to workspace {}, which embedded traffic " +
                        "cannot route through; falling back to direct routing",
                    routingPolicyId, request.model(), policy.getWorkspaceId());

                return request;
            }

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

    Long resolveEmbeddedDefaultRoutingPolicyId(@Nullable Long connectedUserId, long environmentId) {
        return resolveEmbeddedDefaultRoutingPolicyId(
            connectedUserId, resolveEmbeddedSettingsOnce(connectedUserId, environmentId));
    }

    private Long resolveEmbeddedDefaultRoutingPolicyId(
        @Nullable Long connectedUserId, Optional<AiGatewayEmbeddedSettings> embeddedSettings) {

        if (connectedUserId == null) {
            return null;
        }

        return embeddedSettings.map(AiGatewayEmbeddedSettings::defaultRoutingPolicyId)
            .orElse(null);
    }

    private Optional<AiGatewayEmbeddedSettings> resolveEmbeddedSettingsOnce(
        @Nullable Long connectedUserId, long environmentId) {

        if (connectedUserId == null) {
            return Optional.empty();
        }

        AiGatewayEmbeddedSettingsService embeddedSettingsService = embeddedSettingsServiceProvider.getIfAvailable();

        if (embeddedSettingsService == null) {
            return Optional.empty();
        }

        return embeddedSettingsService.find(environmentId);
    }

    private EmbeddedRequestSettings resolveEmbeddedSettingsAndCheckBudget(
        @Nullable Long connectedUserId, long environmentId) {

        if (connectedUserId == null) {
            return EmbeddedRequestSettings.NONE;
        }

        EmbeddedRequestSettings requestSettings = new EmbeddedRequestSettings(
            resolveEmbeddedSettingsOnce(connectedUserId, environmentId),
            aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(connectedUserId));

        checkConnectedUserBudget(connectedUserId, requestSettings);

        return requestSettings;
    }

    private Long resolveModelDefaultRoutingPolicyId(String modelIdentifier) {
        try {
            ModelResolution resolution = resolveModel(modelIdentifier, null);

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

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders) {

        return chatCompletionStream(request, tracingHeaders, (AtomicLong) null);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AtomicLong traceIdHolder) {

        return chatCompletionStream(request, tracingHeaders, null, traceIdHolder);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable Long connectedUserId, @Nullable AtomicLong traceIdHolder) {

        return Flux.defer(
            () -> chatCompletionStreamInternal(request, tracingHeaders, connectedUserId, traceIdHolder));
    }

    private Flux<AiGatewayChatCompletionResponse> chatCompletionStreamInternal(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable Long connectedUserId, @Nullable AtomicLong traceIdHolder) {

        AiObservabilityTracingHeaders effectiveTracingHeaders = tracingHeaders != null
            ? tracingHeaders
            : new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());

        long environmentId = resolveConnectedUserEnvironmentId(connectedUserId);

        checkRequestScope(request.tags(), request.model(), connectedUserId);

        EmbeddedRequestSettings requestSettings = resolveEmbeddedSettingsAndCheckBudget(connectedUserId, environmentId);

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        Long projectId = resolveProjectId(request.tags());

        AiGatewayChatCompletionRequest effectiveRequest = applyRoutingPolicyPrecedence(
            aiGatewayGuardrails.apply(
                request, resolvePlatformType(connectedUserId), workspaceId, projectId, null),
            connectedUserId, requestSettings);

        long startTime = System.currentTimeMillis();

        AiGatewayProject project = resolveProject(effectiveRequest.tags());

        StreamModelResolution streamModelResolution =
            resolveStreamModelResolution(effectiveRequest, workspaceId, startTime, connectedUserId);

        RoutedDeployments routedDeployments = streamModelResolution.routedDeployments();
        ModelResolution modelResolution = streamModelResolution.modelResolution();

        AiGatewayProvider provider = modelResolution.provider();
        AiModel model = modelResolution.model();

        AtomicReference<AiModel> servedModel = new AtomicReference<>(model);
        AtomicReference<AiGatewayProvider> servedProvider = new AtomicReference<>(provider);

        AtomicLong streamInputTokens = new AtomicLong(0);
        AtomicLong streamOutputTokens = new AtomicLong(0);
        AtomicReference<Throwable> streamError = new AtomicReference<>();
        StringBuilder streamOutputContent = new StringBuilder();

        StreamingResponseRedactor responseRedactor =
            aiGatewayGuardrails.newStreamingResponseRedactor(
                resolvePlatformType(connectedUserId), workspaceId, projectId);
        AtomicReference<String> deferredFinishReason = new AtomicReference<>();

        Flux<ChatResponse> chatResponseFlux;

        if (routedDeployments != null) {
            Map<Long, AiModel> routedModelMap = routedDeployments.modelMap();

            chatResponseFlux = aiGatewayRetryHandler.executeStreamWithRetry(
                routedDeployments.orderedDeployments(),
                deployment -> {
                    AiModel deploymentModel = routedModelMap.get(deployment.getModelId());
                    AiGatewayProvider deploymentTenantProvider =
                        aiGatewayProviderService.getProvider(deploymentModel.getProviderId());
                    AiGatewayProvider deploymentProvider =
                        applyByokOverride(deploymentTenantProvider, connectedUserId);
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
                chatResponse, effectiveRequest, streamInputTokens, streamOutputTokens, streamOutputContent,
                responseRedactor, deferredFinishReason));

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
                signalType, streamInputTokens, streamOutputTokens, streamError, streamOutputContent,
                effectiveRequest, effectiveTracingHeaders, workspaceId, servedModel.get(), servedProvider.get(),
                project, startTime, traceIdHolder, connectedUserId));
    }

    private AiGatewayChatCompletionResponse toStreamChunkResponse(
        ChatResponse chatResponse, AiGatewayChatCompletionRequest effectiveRequest, AtomicLong streamInputTokens,
        AtomicLong streamOutputTokens, StringBuilder streamOutputContent,
        @Nullable StreamingResponseRedactor responseRedactor, AtomicReference<String> deferredFinishReason) {

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

        if (chunkText != null) {
            streamOutputContent.append(chunkText);
        }

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
        AtomicReference<Throwable> streamError, StringBuilder streamOutputContent,
        AiGatewayChatCompletionRequest effectiveRequest, AiObservabilityTracingHeaders effectiveTracingHeaders,
        Long workspaceId, AiModel model, AiGatewayProvider provider, AiGatewayProject project,
        long startTime, @Nullable AtomicLong traceIdHolder, @Nullable Long connectedUserId) {

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

        requestLog.setUserId(connectedUserId);

        try {
            transactionTemplate.executeWithoutResult(
                status -> persistUsage(requestLog, workspaceId, connectedUserId));
        } catch (Exception exception) {
            recordRequestLogPersistFailure("streaming", "success");

            log.error("Failed to persist request log for streaming model '{}'. " +
                "Alert on ai_gateway.request_log.persist_failure{{kind=streaming}}.",
                effectiveRequest.model(), exception);
        }

        boolean streamSuccess = streamError.get() == null;

        String accumulatedOutput = streamOutputContent.toString();

        processStreamingTracingHeaders(
            effectiveTracingHeaders, workspaceId, effectiveRequest, model, provider, inputTokens,
            outputTokens,
            startTime, streamSuccess, accumulatedOutput.isEmpty() ? null : accumulatedOutput, traceIdHolder,
            connectedUserId);

        enforcePostRequestBudget(effectiveRequest.tags(), connectedUserId);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayEmbeddingResponse embedding(
        AiGatewayEmbeddingRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders) {

        if (tracingHeaders == null) {
            tracingHeaders = new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());
        }

        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        Long projectId = resolveProjectId(request.tags());

        List<String> guardrailedInputs = aiGatewayGuardrails.applyToInputs(request.input(), workspaceId, projectId);

        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution = resolveModel(request.model(), null);

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

            processEmbeddingTracingHeaders(
                tracingHeaders, workspaceId, request, promptTokens, startTime, true);

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

            processEmbeddingTracingHeaders(
                tracingHeaders, workspaceId, request, 0, startTime, false);

            throw exception;
        }
    }

    private AiGatewayChatCompletionResponse chatCompletionDirect(
        AiGatewayChatCompletionRequest request, @Nullable Long connectedUserId) {
        if (isCacheable(request)) {
            String cacheKey = aiGatewayResponseCache.computeCacheKey(request, connectedUserId);
            AiGatewayChatCompletionResponse cached = aiGatewayResponseCache.get(cacheKey);

            if (cached != null) {
                return cached;
            }
        }

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());
        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution = resolveModel(request.model(), connectedUserId);

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
                request, model, provider, startTime, tokenCounts[0], tokenCounts[1], connectedUserId);

            setProjectIdFromProject(requestLog, project);

            try {
                persistUsage(requestLog, workspaceId, connectedUserId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("chat", "success");

                log.error("Failed to persist request log for chat completion model '{}' — " +
                    "cost of ${} will be missing from spend tracking. " +
                    "Alert on ai_gateway.request_log.persist_failure{{kind=chat}}.",
                    request.model(), requestLog.getCost(), logException);
            }

            enforcePostRequestBudget(request.tags(), connectedUserId);

            AiGatewayChatCompletionResponse response = toResponse(chatResponse, request.model());

            if (isCacheable(request)) {
                String cacheKey = aiGatewayResponseCache.computeCacheKey(request, connectedUserId);

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
                AiLlmUsage errorLog = createErrorLog(request, startTime, exception, connectedUserId);

                setProjectIdFromProject(errorLog, project);

                persistUsage(errorLog, workspaceId, connectedUserId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("chat", "error");

                log.error("Failed to log chat completion error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    private AiGatewayChatCompletionResponse chatCompletionWithRouting(
        AiGatewayChatCompletionRequest request, @Nullable Long connectedUserId) {

        if (isCacheable(request)) {
            String cacheKey = aiGatewayResponseCache.computeCacheKey(request, connectedUserId);
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
                    AiGatewayProvider tenantProvider = aiGatewayProviderService.getProvider(model.getProviderId());
                    AiGatewayProvider provider = applyByokOverride(tenantProvider, connectedUserId);

                    ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

                    AiGatewayChatCompletionRequest processedRequest = compressMessages(request, model, project);

                    Prompt prompt = buildPrompt(processedRequest, model.getName());

                    ChatResponse chatResponse = chatModel.call(prompt);

                    int[] tokenCounts = extractTokenCounts(chatResponse);

                    AiLlmUsage requestLog = createSuccessLog(
                        request, model, provider, startTime, tokenCounts[0], tokenCounts[1], connectedUserId);

                    setProjectIdFromProject(requestLog, project);

                    requestLog.setRoutingPolicyId(routingPolicy.getId());
                    requestLog.setRoutingStrategy(
                        routingPolicy.getStrategy() == null ? null : routingPolicy.getStrategy()
                            .ordinal());

                    try {
                        persistUsage(requestLog, workspaceId, connectedUserId);
                    } catch (Exception logException) {
                        recordRequestLogPersistFailure("routing", "success");

                        log.error("Failed to persist request log for routed model '{}' — " +
                            "cost of ${} will be missing from spend tracking. " +
                            "Alert on ai_gateway.request_log.persist_failure{{kind=routing}}.",
                            request.model(), requestLog.getCost(), logException);
                    }

                    enforcePostRequestBudget(request.tags(), connectedUserId);

                    return toResponse(chatResponse, request.model());
                });

            if (isCacheable(request)) {
                String cacheKey = aiGatewayResponseCache.computeCacheKey(request, connectedUserId);

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
                AiLlmUsage errorLog = createErrorLog(request, startTime, exception, connectedUserId);

                setProjectIdFromProject(errorLog, project);

                errorLog.setRoutingPolicyId(routingPolicy.getId());
                errorLog.setRoutingStrategy(routingPolicy.getStrategy() == null ? null : routingPolicy.getStrategy()
                    .ordinal());

                persistUsage(errorLog, workspaceId, connectedUserId);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("routing", "error");

                log.error("Failed to log routing error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    private void checkRequestScope(Map<String, String> tags, String model, @Nullable Long connectedUserId) {
        if (connectedUserId == null) {
            checkBudget(tags, model);
            checkRateLimits(tags);

            return;
        }

        if (tags != null && (tags.containsKey("workspace_id") || tags.containsKey("project_id"))) {
            throw new IllegalArgumentException(
                "Embedded requests must not carry a 'workspace_id' or 'project_id' tag");
        }
    }

    private void persistUsage(AiLlmUsage usage, @Nullable Long workspaceId, @Nullable Long connectedUserId) {
        if (connectedUserId == null) {
            aiGatewayRequestLogService.create(usage, workspaceId);
        } else {
            aiGatewayRequestLogService.createForConnectedUser(usage, connectedUserId);
        }
    }

    private void enforcePostRequestBudget(Map<String, String> tags, @Nullable Long connectedUserId) {
        if (connectedUserId != null) {
            return;
        }

        enforcePostRequestBudget(tags);
    }

    private static @Nullable PlatformType resolvePlatformType(@Nullable Long connectedUserId) {
        return connectedUserId == null ? null : PlatformType.EMBEDDED;
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
                applicationEventPublisher.publishEvent(new AiGatewayBudgetExceededEvent(
                    workspaceId, model, budgetResult.currentSpend(), budgetResult.budgetAmount()));

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

    private void checkConnectedUserBudget(long connectedUserId, EmbeddedRequestSettings requestSettings) {
        BigDecimal cap = resolveBudgetCap(requestSettings);

        if (cap == null) {
            return;
        }

        Instant periodStart = currentBillingPeriodStart();

        Money currentSpend =
            aiGatewaySpendService.getTotalCostByConnectedUserId(connectedUserId, periodStart, Instant.now());
        Money capMoney = Money.usd(cap);

        if (currentSpend.compareTo(capMoney) >= 0) {
            throw new BudgetExceededException(
                "Budget cap exceeded for connected user " + connectedUserId +
                    ". Current spend: $" + currentSpend.amount() + " / Cap: $" + cap,
                capMoney, currentSpend);
        }
    }

    private static @Nullable BigDecimal resolveBudgetCap(EmbeddedRequestSettings requestSettings) {
        BigDecimal connectedUserCap = requestSettings.connectedUserSettings()
            .map(AiGatewayConnectedUserSettings::getBudgetCap)
            .orElse(null);

        if (connectedUserCap != null) {
            return connectedUserCap;
        }

        return requestSettings.embeddedSettings()
            .map(AiGatewayEmbeddedSettings::defaultConnectedUserBudgetCap)
            .orElse(null);
    }

    private static Instant currentBillingPeriodStart() {
        return ZonedDateTime.now(ZoneOffset.UTC)
            .withDayOfMonth(1)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant();
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
        AiGatewayChatCompletionRequest effectiveRequest, Long workspaceId, long startTime,
        @Nullable Long connectedUserId) {

        RoutedDeployments routedDeployments = null;
        ModelResolution modelResolution;

        try {
            if (effectiveRequest.routingPolicy() != null) {
                routedDeployments = selectRoutedDeployments(effectiveRequest);

                AiGatewayModelDeployment primaryDeployment = routedDeployments.orderedDeployments()
                    .get(0);
                AiModel primaryModel = routedDeployments.modelMap()
                    .get(primaryDeployment.getModelId());
                AiGatewayProvider primaryTenantProvider =
                    aiGatewayProviderService.getProvider(primaryModel.getProviderId());
                AiGatewayProvider primaryProvider = applyByokOverride(primaryTenantProvider, connectedUserId);

                modelResolution = new ModelResolution(primaryProvider, primaryModel);
            } else {
                modelResolution = resolveModel(effectiveRequest.model(), connectedUserId);
            }
        } catch (Exception exception) {
            try {
                persistUsage(
                    createErrorLog(effectiveRequest, startTime, exception, connectedUserId), workspaceId,
                    connectedUserId);
            } catch (Exception logException) {
                log.error("Failed to log streaming setup error for model '{}'. Original error: {}",
                    effectiveRequest.model(), exception.getMessage(), logException);
            }

            throw exception;
        }

        return new StreamModelResolution(modelResolution, routedDeployments);
    }

    private AiGatewayProvider applyByokOverride(AiGatewayProvider tenantProvider, @Nullable Long connectedUserId) {
        if (connectedUserId == null) {
            return tenantProvider;
        }

        Optional<AiGatewayProvider> connectedUserProvider = aiGatewayProviderService
            .fetchProviderByConnectedUserIdAndType(connectedUserId, tenantProvider.getType());

        return connectedUserProvider.filter(AiGatewayProvider::isEnabled)
            .orElseGet(() -> requireTenantProviderUsableByConnectedUser(tenantProvider, connectedUserId));
    }

    private AiGatewayProvider requireTenantProviderUsableByConnectedUser(
        AiGatewayProvider tenantProvider, long connectedUserId) {

        Long tenantProviderConnectedUserId = tenantProvider.getConnectedUserId();

        if (tenantProviderConnectedUserId != null && !tenantProviderConnectedUserId.equals(connectedUserId)) {
            throw new AiGatewayProviderScopeViolationException(
                "Provider " + tenantProvider.getId() + " is scoped to a different connected user",
                tenantProvider.getId(), connectedUserId, tenantProviderConnectedUserId);
        }

        return tenantProvider;
    }

    private ModelResolution resolveModel(String modelIdentifier, @Nullable Long connectedUserId) {
        String[] parts = modelIdentifier.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Model must be in format 'provider/model', got: " + modelIdentifier);
        }

        String providerTypeName = parts[0].toUpperCase()
            .replace("-", "_");
        String modelName = parts[1];

        AiGatewayProvider tenantProvider = resolveTenantProviderByTypeName(providerTypeName);

        AiModel model = aiModelService.getModel(tenantProvider.getId(), modelName);

        AiGatewayProvider provider = connectedUserId == null
            ? tenantProvider
            : aiGatewayProviderResolver.resolve(connectedUserId, tenantProvider.getType());

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
        long startTime, int inputTokens, int outputTokens, @Nullable Long connectedUserId) {

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

        requestLog.setUserId(connectedUserId);

        return requestLog;
    }

    private static void setApiKeyIdFromAuthentication(AiLlmUsage requestLog) {
        Long apiKeyId = resolveAuthenticatedApiKeyId();

        if (apiKeyId != null) {
            requestLog.setApiKeyId(apiKeyId);
        }
    }

    private static Long resolveAuthenticatedApiKeyId() {
        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null) {
            return null;
        }

        org.springframework.security.core.Authentication authentication = context.getAuthentication();

        if (authentication instanceof com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken token) {
            return token.getApiKeyId();
        }

        return null;
    }

    private static long resolveConnectedUserEnvironmentId(@Nullable Long connectedUserId) {
        if (connectedUserId == null) {
            return 0L;
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();

        if (securityContext
            .getAuthentication() instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
            return apiKeyAuthenticationToken.getEnvironmentId();
        }

        throw new AccessDeniedException(
            "Connected user " + connectedUserId + " is not authenticated into an environment");
    }

    private AiLlmUsage createErrorLog(
        AiGatewayChatCompletionRequest request, long startTime, Exception exception,
        @Nullable Long connectedUserId) {

        AiLlmUsage errorLog = new AiLlmUsage(
            UUID.randomUUID()
                .toString(),
            request.model());

        errorLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        errorLog.setStatus(resolveErrorStatus(exception));
        errorLog.setErrorMessage(exception.getMessage());

        setApiKeyIdFromAuthentication(errorLog);

        errorLog.setUserId(connectedUserId);

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

    private void processTracingHeaders(
        AiObservabilityTracingHeaders tracingHeaders,
        @Nullable Long workspaceId,
        AiGatewayChatCompletionRequest request,
        @Nullable AiGatewayChatCompletionResponse response,
        long startTime,
        boolean success,
        @Nullable Long connectedUserId) {

        if (workspaceId == null) {
            return;
        }

        try {
            String inputText = request.messages()
                .stream()
                .map(message -> message.content() != null ? message.content() : "")
                .reduce("", (accumulator, content) -> accumulator + content);
            String outputText = null;

            int inputTokens = 0;
            int outputTokens = 0;

            if (response != null && response.usage() != null) {
                inputTokens = response.usage()
                    .promptTokens();
                outputTokens = response.usage()
                    .completionTokens();
            }

            if (response != null && response.choices() != null && !response.choices()
                .isEmpty()) {

                AiGatewayChatCompletionResponse.Choice firstChoice = response.choices()
                    .getFirst();

                if (firstChoice.message() != null) {
                    outputText = firstChoice.message()
                        .content();
                }
            }

            int latencyMs = (int) (System.currentTimeMillis() - startTime);

            String[] modelParts = request.model()
                .split("/", 2);
            String providerName = modelParts.length > 0 ? modelParts[0] : request.model();
            String modelName = modelParts.length > 1 ? modelParts[1] : request.model();

            BigDecimal cost = calculateTraceCost(request.model(), inputTokens, outputTokens);

            AiGatewayProject project = resolveProject(request.tags());

            AiObservabilityTrace trace = resolveOrCreateTrace(
                tracingHeaders, workspaceId, modelName, inputText, outputText, inputTokens, outputTokens,
                latencyMs, cost, success, project);

            AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

            span.setStartTime(Instant.ofEpochMilli(startTime));
            span.setName(tracingHeaders.spanName() != null ? tracingHeaders.spanName() : modelName);
            span.setModel(modelName);
            span.setProvider(providerName);
            span.setInput(inputText);
            span.setOutput(outputText);
            span.setInputTokens(inputTokens);
            span.setOutputTokens(outputTokens);
            span.setLatencyMs(latencyMs);
            span.setCost(cost);

            Instant spanEndTime = Instant.now();

            if (tracingHeaders.parentSpanId() != null) {
                try {
                    span.setParentSpanId(Long.parseLong(tracingHeaders.parentSpanId()));
                } catch (NumberFormatException numberFormatException) {
                    log.warn("Invalid parentSpanId header value '{}' — ignoring",
                        tracingHeaders.parentSpanId());
                }
            }

            applyConnectedUserSpanAttribute(span, connectedUserId);

            span.close(spanEndTime, success ? AiObservabilitySpanStatus.COMPLETED : AiObservabilitySpanStatus.ERROR);

            aiObservabilitySpanService.create(span);

            if (trace.getStatus() == AiObservabilityTraceStatus.COMPLETED
                || trace.getStatus() == AiObservabilityTraceStatus.ERROR) {

                try {
                    publishTraceCompletedEvent(trace, request.model());
                } catch (Exception publishException) {
                    log.error(
                        "Failed to publish trace-completed event for model '{}' (traceId={})",
                        request.model(), trace.getId(), publishException);
                }
            }

        } catch (Exception exception) {
            log.error(
                "Failed to persist trace/span for model '{}' — tracing data will be missing",
                request.model(), exception);
        }
    }

    private void publishTraceCompletedEvent(AiObservabilityTrace trace, String modelName) {
        applicationEventPublisher.publishEvent(new AiGatewayTraceCompletedEvent(
            workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()),
            trace.getId(),
            trace.getExternalTraceId(),
            modelName,
            trace.getTotalInputTokens(),
            trace.getTotalOutputTokens(),
            trace.getTotalLatencyMs(),
            trace.getTotalCost(),
            trace.getStatus() == AiObservabilityTraceStatus.COMPLETED,
            Instant.now()));
    }

    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private void processStreamingTracingHeaders(
        AiObservabilityTracingHeaders tracingHeaders,
        @Nullable Long workspaceId,
        AiGatewayChatCompletionRequest request,
        AiModel model,
        AiGatewayProvider provider,
        int inputTokens,
        int outputTokens,
        long startTime,
        boolean success,
        @Nullable String outputText,
        @Nullable AtomicLong traceIdHolder,
        @Nullable Long connectedUserId) {

        if (workspaceId == null) {
            return;
        }

        try {
            String inputText = request.messages()
                .stream()
                .map(message -> message.content() != null ? message.content() : "")
                .reduce("", (accumulator, content) -> accumulator + content);

            int latencyMs = (int) (System.currentTimeMillis() - startTime);

            BigDecimal cost = calculateTraceCost(request.model(), inputTokens, outputTokens);

            AiGatewayProject project = resolveProject(request.tags());

            AiObservabilityTrace trace = resolveOrCreateTrace(
                tracingHeaders, workspaceId, model.getName(), inputText, outputText, inputTokens, outputTokens,
                latencyMs, cost, success, project);

            if (traceIdHolder != null && trace.getId() != null) {
                traceIdHolder.set(trace.getId());
            }

            AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

            span.setStartTime(Instant.ofEpochMilli(startTime));
            span.setName(tracingHeaders.spanName() != null ? tracingHeaders.spanName() : model.getName());
            span.setModel(model.getName());
            span.setProvider(provider.getType()
                .name());
            span.setInput(inputText);
            span.setOutput(outputText);
            span.setInputTokens(inputTokens);
            span.setOutputTokens(outputTokens);
            span.setLatencyMs(latencyMs);
            span.setCost(cost);

            Instant spanEndTime = Instant.now();

            if (tracingHeaders.parentSpanId() != null) {
                try {
                    span.setParentSpanId(Long.parseLong(tracingHeaders.parentSpanId()));
                } catch (NumberFormatException numberFormatException) {
                    log.warn("Invalid parentSpanId header value '{}' — ignoring",
                        tracingHeaders.parentSpanId());
                }
            }

            applyConnectedUserSpanAttribute(span, connectedUserId);

            span.close(spanEndTime, success ? AiObservabilitySpanStatus.COMPLETED : AiObservabilitySpanStatus.ERROR);

            transactionTemplate.executeWithoutResult(
                status -> aiObservabilitySpanService.create(span));
        } catch (Exception exception) {
            log.error("Failed to process streaming tracing headers for model '{}' — tracing data will be missing",
                request.model(), exception);
        }
    }

    private static void applyConnectedUserSpanAttribute(AiObservabilitySpan span, @Nullable Long connectedUserId) {
        if (connectedUserId != null) {
            span.setMetadata("{\"connectedUserId\":" + connectedUserId + "}");
        }
    }

    private void processEmbeddingTracingHeaders(
        AiObservabilityTracingHeaders tracingHeaders,
        @Nullable Long workspaceId,
        AiGatewayEmbeddingRequest request,
        int inputTokens,
        long startTime,
        boolean success) {

        if (workspaceId == null) {
            return;
        }

        try {
            String inputText = String.join(", ", request.input());

            int latencyMs = (int) (System.currentTimeMillis() - startTime);

            String[] modelParts = request.model()
                .split("/", 2);
            String providerName = modelParts.length > 0 ? modelParts[0] : request.model();
            String modelName = modelParts.length > 1 ? modelParts[1] : request.model();

            BigDecimal cost = calculateTraceCost(request.model(), inputTokens, 0);

            AiObservabilityTrace trace = resolveOrCreateTrace(
                tracingHeaders, workspaceId, modelName, inputText, null, inputTokens, 0,
                latencyMs, cost, success, null);

            AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

            span.setStartTime(Instant.ofEpochMilli(startTime));
            span.setName(tracingHeaders.spanName() != null ? tracingHeaders.spanName() : modelName);
            span.setModel(modelName);
            span.setProvider(providerName);
            span.setInput(inputText);
            span.setInputTokens(inputTokens);
            span.setLatencyMs(latencyMs);
            span.setCost(cost);

            Instant spanEndTime = Instant.now();

            if (tracingHeaders.parentSpanId() != null) {
                try {
                    span.setParentSpanId(Long.parseLong(tracingHeaders.parentSpanId()));
                } catch (NumberFormatException numberFormatException) {
                    log.warn("Invalid parentSpanId header value '{}' — ignoring",
                        tracingHeaders.parentSpanId());
                }
            }

            span.close(spanEndTime, success ? AiObservabilitySpanStatus.COMPLETED : AiObservabilitySpanStatus.ERROR);

            aiObservabilitySpanService.create(span);
        } catch (Exception exception) {
            log.error("Failed to process tracing headers for embedding model '{}' — tracing data will be missing",
                request.model(), exception);
        }
    }

    private AiObservabilityTrace resolveOrCreateTrace(
        AiObservabilityTracingHeaders tracingHeaders,
        Long workspaceId,
        String modelName,
        String inputText,
        @Nullable String outputText,
        int inputTokens,
        int outputTokens,
        int latencyMs,
        @Nullable BigDecimal cost,
        boolean success,
        @Nullable AiGatewayProject project) {

        String externalTraceId = tracingHeaders.traceId();

        if (tracingHeaders.hasExternalTraceId()) {
            AiObservabilityTrace existingTrace = workspaceAiObservabilityTraceService
                .findByExternalTraceId(workspaceId, externalTraceId)
                .orElse(null);

            if (existingTrace != null) {
                Integer existingInputTokens = existingTrace.getTotalInputTokens();
                Integer existingOutputTokens = existingTrace.getTotalOutputTokens();
                Integer existingLatencyMs = existingTrace.getTotalLatencyMs();
                BigDecimal existingCost = existingTrace.getTotalCost();

                existingTrace.setTotalInputTokens(
                    (existingInputTokens != null ? existingInputTokens : 0) + inputTokens);
                existingTrace.setTotalOutputTokens(
                    (existingOutputTokens != null ? existingOutputTokens : 0) + outputTokens);
                existingTrace.setTotalLatencyMs(
                    (existingLatencyMs != null ? existingLatencyMs : 0) + latencyMs);

                if (cost != null) {
                    existingTrace.setTotalCost(
                        existingCost != null ? existingCost.add(cost) : cost);
                }

                if (!success) {
                    existingTrace.setStatus(AiObservabilityTraceStatus.ERROR);
                }

                aiObservabilityTraceService.update(existingTrace);

                return existingTrace;
            }
        }

        boolean redact = isPiiRedactionEnabled(workspaceId);

        AiObservabilityTrace trace = new AiObservabilityTrace(tracingHeaders.source());

        trace.setExternalTraceId(externalTraceId);
        trace.setName(tracingHeaders.spanName() != null ? tracingHeaders.spanName() : modelName);
        trace.setInput(redact ? redactedDigest(inputText) : inputText);
        trace.setOutput(redact ? redactedDigest(outputText) : outputText);
        trace.setPiiRedacted(redact);
        trace.setApiKeyId(resolveAuthenticatedApiKeyId());
        trace.setTotalInputTokens(inputTokens);
        trace.setTotalOutputTokens(outputTokens);
        trace.setTotalLatencyMs(latencyMs);
        trace.setTotalCost(cost);
        trace.setUserId(tracingHeaders.userId());
        trace.setStatus(success ? AiObservabilityTraceStatus.COMPLETED : AiObservabilityTraceStatus.ERROR);

        if (!tracingHeaders.metadata()
            .isEmpty()) {

            StringBuilder metadataJson = new StringBuilder("{");
            boolean first = true;

            for (Map.Entry<String, String> entry : tracingHeaders.metadata()
                .entrySet()) {

                if (!first) {
                    metadataJson.append(",");
                }

                metadataJson.append("\"")
                    .append(entry.getKey()
                        .replace("\"", "\\\""))
                    .append("\":\"")
                    .append(entry.getValue()
                        .replace("\"", "\\\""))
                    .append("\"");
                first = false;
            }

            metadataJson.append("}");

            trace.setMetadata(metadataJson.toString());
        }

        if (project != null) {
            trace.setProjectId(project.getId());
        }

        if (tracingHeaders.sessionId() != null) {
            AiObservabilitySession session = workspaceAiObservabilitySessionService.getOrCreateSessionByExternalId(
                workspaceId, tracingHeaders.sessionId(), project != null ? project.getId() : null,
                tracingHeaders.userId());

            trace.setSessionId(session.getId());
        }

        if (!tracingHeaders.tagNames()
            .isEmpty()) {

            List<com.bytechef.platform.tag.domain.Tag> savedTags = tagService.save(
                tracingHeaders.tagNames()
                    .stream()
                    .map(com.bytechef.platform.tag.domain.Tag::new)
                    .toList());

            Set<AiObservabilityTraceTag> traceTags = savedTags.stream()
                .map(tag -> new AiObservabilityTraceTag(tag.getId()))
                .collect(Collectors.toCollection(HashSet::new));

            trace.setTags(traceTags);
        }

        if (tracingHeaders.hasExternalTraceId()) {
            try {
                workspaceAiObservabilityTraceService.createInWorkspace(trace, workspaceId);
            } catch (org.springframework.dao.DuplicateKeyException duplicate) {
                if (!AiGatewayConstraintMatchers.matchesConstraint(
                    duplicate, TRACE_DEDUP_INDEX, TRACE_DEDUP_INDEX_PATTERN)) {

                    throw duplicate;
                }

                AiObservabilityTrace winner = workspaceAiObservabilityTraceService
                    .findByExternalTraceId(workspaceId, externalTraceId)
                    .orElseThrow(() -> duplicate);

                mergeIntoExistingTrace(winner, trace);

                aiObservabilityTraceService.update(winner);

                return winner;
            }
        } else {
            workspaceAiObservabilityTraceService.createInWorkspace(trace, workspaceId);
        }

        return trace;
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

    private boolean isPiiRedactionEnabled(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return false;
        }

        return aiGuardrailsWorkspaceSettingsService.fetchSettings(workspaceId)
            .map(settings -> Boolean.TRUE.equals(settings.redactPii()))
            .orElse(false);
    }

    private static String redactedDigest(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(bytes);

            StringBuilder hex = new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return String.format("[redacted len=%d sha256=%s]", bytes.length, hex.substring(0, 16));
        } catch (java.security.NoSuchAlgorithmException noSha256) {
            return String.format("[redacted len=%d]", bytes.length);
        }
    }

    private static void mergeIntoExistingTrace(AiObservabilityTrace winner, AiObservabilityTrace current) {
        Integer existingInputTokens = winner.getTotalInputTokens();
        Integer existingOutputTokens = winner.getTotalOutputTokens();
        Integer existingLatencyMs = winner.getTotalLatencyMs();
        BigDecimal existingCost = winner.getTotalCost();

        Integer addInputTokens = current.getTotalInputTokens();
        Integer addOutputTokens = current.getTotalOutputTokens();
        Integer addLatencyMs = current.getTotalLatencyMs();
        BigDecimal addCost = current.getTotalCost();

        winner.setTotalInputTokens(
            (existingInputTokens != null ? existingInputTokens : 0) + (addInputTokens != null ? addInputTokens : 0));
        winner.setTotalOutputTokens(
            (existingOutputTokens != null ? existingOutputTokens : 0) +
                (addOutputTokens != null ? addOutputTokens : 0));
        winner.setTotalLatencyMs(
            (existingLatencyMs != null ? existingLatencyMs : 0) + (addLatencyMs != null ? addLatencyMs : 0));

        if (addCost != null) {
            winner.setTotalCost(existingCost != null ? existingCost.add(addCost) : addCost);
        }

        if (current.getStatus() == AiObservabilityTraceStatus.ERROR) {
            winner.setStatus(AiObservabilityTraceStatus.ERROR);
        }
    }

    @Nullable
    private BigDecimal calculateTraceCost(String modelIdentifier, int inputTokens, int outputTokens) {
        try {
            ModelResolution modelResolution = resolveModel(modelIdentifier, null);

            return aiGatewayCostCalculator.calculateCost(
                modelResolution.model(), inputTokens, outputTokens);
        } catch (Exception exception) {
            log.error(
                "Failed to calculate cost for trace — model '{}'; persisting null (unknown) cost. " +
                    "Cost dashboards and cost-based alert rules MUST exclude null cost rows.",
                modelIdentifier, exception);

            return null;
        }
    }

    static String validateExternalUrl(String url) {
        AiGatewayUrlValidator.validateExternalUrl(url);

        return url;
    }

    private record ModelResolution(AiGatewayProvider provider, AiModel model) {
    }

    private record EmbeddedRequestSettings(
        Optional<AiGatewayEmbeddedSettings> embeddedSettings,
        Optional<AiGatewayConnectedUserSettings> connectedUserSettings) {

        private static final EmbeddedRequestSettings NONE =
            new EmbeddedRequestSettings(Optional.empty(), Optional.empty());
    }
}
