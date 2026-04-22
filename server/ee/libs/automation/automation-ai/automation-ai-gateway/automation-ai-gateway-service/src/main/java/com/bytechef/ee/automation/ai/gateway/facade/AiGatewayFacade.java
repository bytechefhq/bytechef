/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker;
import com.bytechef.ee.automation.ai.gateway.cache.AiGatewayResponseCache;
import com.bytechef.ee.automation.ai.gateway.compression.AiGatewayContextCompressor;
import com.bytechef.ee.automation.ai.gateway.cost.AiGatewayCostCalculator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRequestLog;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceTag;
import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.automation.ai.gateway.domain.Money;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayEmbeddingRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayEmbeddingResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiObservabilityTracingHeaders;
import com.bytechef.ee.automation.ai.gateway.dto.AiPromptHeaders;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.automation.ai.gateway.event.AiGatewayTraceCompletedEvent;
import com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.reliability.AiGatewayRetryHandler;
import com.bytechef.ee.automation.ai.gateway.routing.AiGatewayRouter;
import com.bytechef.ee.automation.ai.gateway.routing.AiGatewayRoutingContext;
import com.bytechef.ee.automation.ai.gateway.security.AiGatewayUrlValidator;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelDeploymentService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRequestLogService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayTagService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySessionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptService;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
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
 * Central facade for LLM Gateway chat completion and embedding operations.
 *
 * <p>
 * Request lifecycle:
 * <ol>
 * <li>Pre-request budget check</li>
 * <li>Cache lookup (direct path only)</li>
 * <li>Model resolution (provider/name parsing)</li>
 * <li>Context compression (if messages exceed 85% of context window)</li>
 * <li>Prompt building (with multimodal content support)</li>
 * <li>LLM call (direct or via routing/retry)</li>
 * <li>Cost calculation</li>
 * <li>Request logging</li>
 * <li>Post-request budget enforcement</li>
 * </ol>
 *
 * @version ee
 */
@Component
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "BX_UNBOXING_IMMEDIATELY_REBOXED", "EI", "PREDICTABLE_RANDOM", "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT"
})
public class AiGatewayFacade {

    private static final double CONTEXT_WINDOW_USAGE_RATIO = 0.85;
    private static final BigDecimal DEFAULT_FALLBACK_COST_PER_M_TOKENS = new BigDecimal("10.00");
    private static final Logger logger = LoggerFactory.getLogger(AiGatewayFacade.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final AiEvalExecutor aiEvalExecutor;
    private final AiGatewayBudgetChecker aiGatewayBudgetChecker;
    private final AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    private final AiGatewayContextCompressor aiGatewayContextCompressor;
    private final AiGatewayCostCalculator aiGatewayCostCalculator;
    private final AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory;
    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;
    private final AiGatewayModelService aiGatewayModelService;
    private final AiGatewayProjectService aiGatewayProjectService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final AiGatewayRequestLogService aiGatewayRequestLogService;
    private final AiGatewayResponseCache aiGatewayResponseCache;
    private final AiGatewayRetryHandler aiGatewayRetryHandler;
    private final AiGatewayRouter aiGatewayRouter;
    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;
    private final AiPromptService aiPromptService;
    private final AiPromptVersionService aiPromptVersionService;
    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final AiGatewayTagService aiGatewayTagService;
    private final AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    // Optional so lightweight app variants without actuator (no MeterRegistry bean) still start. Resolved via
    // ObjectProvider rather than @Nullable injection so tests can supply a real provider without wiring Spring.
    private final ObjectProvider<AiGatewayMetrics> aiGatewayMetricsProvider;
    private final ApplicationEventPublisher applicationEventPublisher;
    @Nullable
    private final PermissionService permissionService;
    private final TransactionTemplate transactionTemplate;

    public AiGatewayFacade(
        AiEvalExecutor aiEvalExecutor,
        AiGatewayBudgetChecker aiGatewayBudgetChecker,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayChatModelFactory aiGatewayChatModelFactory,
        AiGatewayContextCompressor aiGatewayContextCompressor,
        AiGatewayCostCalculator aiGatewayCostCalculator,
        AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory,
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService,
        AiGatewayModelService aiGatewayModelService,
        AiGatewayProjectService aiGatewayProjectService,
        AiGatewayProviderService aiGatewayProviderService,
        AiGatewayRequestLogService aiGatewayRequestLogService,
        AiGatewayResponseCache aiGatewayResponseCache,
        AiGatewayRetryHandler aiGatewayRetryHandler,
        AiGatewayRouter aiGatewayRouter,
        AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService,
        AiPromptService aiPromptService,
        AiPromptVersionService aiPromptVersionService,
        AiObservabilitySessionService aiObservabilitySessionService,
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilityTraceService aiObservabilityTraceService,
        AiGatewayTagService aiGatewayTagService,
        AiGatewayWorkspaceSettingsService aiGatewayWorkspaceSettingsService,
        ObjectProvider<AiGatewayMetrics> aiGatewayMetricsProvider,
        ApplicationEventPublisher applicationEventPublisher,
        @Nullable PermissionService permissionService,
        PlatformTransactionManager transactionManager) {

        this.aiEvalExecutor = aiEvalExecutor;
        this.aiGatewayBudgetChecker = aiGatewayBudgetChecker;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
        this.aiGatewayContextCompressor = aiGatewayContextCompressor;
        this.aiGatewayCostCalculator = aiGatewayCostCalculator;
        this.aiGatewayEmbeddingModelFactory = aiGatewayEmbeddingModelFactory;
        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
        this.aiGatewayModelService = aiGatewayModelService;
        this.aiGatewayProjectService = aiGatewayProjectService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.aiGatewayRequestLogService = aiGatewayRequestLogService;
        this.aiGatewayResponseCache = aiGatewayResponseCache;
        this.aiGatewayRetryHandler = aiGatewayRetryHandler;
        this.aiGatewayRouter = aiGatewayRouter;
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
        this.aiPromptService = aiPromptService;
        this.aiPromptVersionService = aiPromptVersionService;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.aiGatewayTagService = aiGatewayTagService;
        this.aiGatewayWorkspaceSettingsService = aiGatewayWorkspaceSettingsService;
        this.aiGatewayMetricsProvider = aiGatewayMetricsProvider;
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

    /**
     * Validates that the currently-authenticated caller is a member of {@code workspaceId}. Any API-key-authenticated
     * request supplies {@code workspace_id} in the request-body {@code tags}; without this check the caller could bill
     * another workspace's budget, consume its rate limits, or write to its observability traces.
     *
     * <p>
     * Fails closed: if {@link PermissionService} is not wired or there is no authenticated SecurityContext, throws
     * {@link AccessDeniedException}. No silent bypass — a misconfigured deployment or an async thread that failed to
     * propagate SecurityContext MUST surface as an authorization error rather than letting the request through. If the
     * caller is a trusted internal component with no security context (e.g. scheduler, background job), it must set a
     * service-account authentication into {@link SecurityContextHolder} before invoking the facade.
     *
     * <p>
     * Returns silently on: {@code workspaceId == null} (legitimately anonymous request), the principal has global
     * {@code ROLE_ADMIN} (tenant admin bypass), or the principal has a workspace role.
     */
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders) {

        return chatCompletion(request, tracingHeaders, null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders) {

        if (tracingHeaders == null) {
            tracingHeaders = new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());
        }

        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());

        ResolvedPrompt resolvedPrompt = resolvePrompt(promptHeaders, workspaceId, request);

        if (resolvedPrompt != null) {
            request = prependSystemMessage(request, resolvedPrompt.content());
        }

        long startTime = System.currentTimeMillis();

        AiGatewayChatCompletionResponse response;
        boolean success = true;

        request = applyRoutingPolicyPrecedence(request);

        try {
            if (request.routingPolicy() != null) {
                response = chatCompletionWithRouting(request);
            } else {
                response = chatCompletionDirect(request);
            }
        } catch (Exception exception) {
            success = false;

            processTracingHeaders(
                tracingHeaders, workspaceId, request, null, startTime, false, resolvedPrompt);

            throw exception;
        }

        processTracingHeaders(tracingHeaders, workspaceId, request, response, startTime, success, resolvedPrompt);

        return withGatewayMetadata(response, request, startTime);
    }

    /**
     * Resolves the effective routing policy with precedence: request-specified → model default. Workspace and project
     * defaults would extend this chain once their resolution paths are wired; for now, model-level default is the
     * single fallback.
     */
    private AiGatewayChatCompletionRequest applyRoutingPolicyPrecedence(AiGatewayChatCompletionRequest request) {
        if (request.routingPolicy() != null) {
            return request;
        }

        Long modelDefaultPolicyId = resolveModelDefaultRoutingPolicyId(request.model());

        if (modelDefaultPolicyId == null) {
            return request;
        }

        try {
            AiGatewayRoutingPolicy policy = aiGatewayRoutingPolicyService.getRoutingPolicy(modelDefaultPolicyId);

            return new AiGatewayChatCompletionRequest(
                request.model(), request.messages(), request.temperature(), request.maxTokens(), request.topP(),
                request.stream(), policy.getName(), request.cache(), request.toolChoice(), request.tools(),
                request.tags());
        } catch (IllegalArgumentException missingPolicy) {
            // Model references a deleted policy — fall through to direct routing rather than 500.
            logger.warn(
                "Model '{}' has default_routing_policy_id={} but policy not found; falling back to direct routing",
                request.model(), modelDefaultPolicyId);

            return request;
        }
    }

    private Long resolveModelDefaultRoutingPolicyId(String modelIdentifier) {
        try {
            ModelResolution resolution = resolveModel(modelIdentifier);

            return resolution.model() != null ? resolution.model()
                .getDefaultRoutingPolicyId() : null;
        } catch (IllegalArgumentException resolveFailure) {
            // Known, caller-caused: bad model identifier, unknown provider, or disabled provider. Fall through to
            // direct routing; the downstream call will surface a clearer error. Log at WARN with enough context for
            // operators to spot a misconfigured default — a silently-missed policy means billing rates and routing
            // rules the user expected aren't applied.
            logger.warn(
                "Could not resolve default routing policy for model '{}' (reason: {}); " +
                    "falling through to direct routing. Verify the provider is enabled and the model id format is " +
                    "'provider/model'.",
                modelIdentifier, resolveFailure.getMessage());

            return null;
        }
        // Intentionally do NOT catch other RuntimeExceptions here — a DB/transport failure during model lookup is
        // infrastructure-level and must bubble up so the request fails fast rather than silently routing direct.
    }

    /**
     * Returns a copy of {@code response} with observability metadata populated so the public REST controller can emit
     * {@code x-gateway-*} headers. When the response is null or already has metadata (e.g. from a deeper populator),
     * returns the original unchanged.
     */
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

    /**
     * Re-runs the budget check after a successful response so the soft-limit warning header reflects the post-request
     * state (the pre-request check could have been below threshold while this call's cost just pushed it over). Returns
     * the remaining USD allowance when the workspace is in warning state, {@code null} otherwise. Defensive: never
     * throws — observability metadata isn't worth failing the user request for.
     */
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
            logger.warn(
                "Budget warning probe failed for workspace tag {} — budget warning header will be omitted for this response",
                tags.get("workspace_id"), probeFailure);
        }

        return null;
    }

    /**
     * Extracts the provider slug from a {@code provider/model} identifier such as {@code "openai/gpt-4"}. Returns
     * {@code null} when the identifier has no provider prefix so the header is simply omitted rather than misleading.
     */
    private static String providerFromModel(String modelIdentifier) {
        if (modelIdentifier == null) {
            return null;
        }

        int slash = modelIdentifier.indexOf('/');

        return slash > 0 ? modelIdentifier.substring(0, slash) : null;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders) {

        return chatCompletionStream(request, tracingHeaders, null, null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders) {

        return chatCompletionStream(request, tracingHeaders, promptHeaders, null);
    }

    /**
     * Streaming overload that reports the created trace's internal id back to the caller via {@code traceIdHolder}. The
     * holder is populated inside {@code doOnComplete} (after the trace row is persisted and before the downstream
     * Flux's terminal signal propagates), so a caller that appends a final "totals" chunk via
     * {@code concatWith(Flux.defer(...))} can read {@code traceIdHolder.get()} in the defer and include it in the final
     * chunk. Pass {@code null} if the caller does not need the trace id.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders, @Nullable AtomicLong traceIdHolder) {

        return Flux.defer(() -> chatCompletionStreamInternal(request, tracingHeaders, promptHeaders, traceIdHolder));
    }

    private Flux<AiGatewayChatCompletionResponse> chatCompletionStreamInternal(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders, @Nullable AtomicLong traceIdHolder) {

        AiObservabilityTracingHeaders effectiveTracingHeaders = tracingHeaders != null
            ? tracingHeaders
            : new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());

        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());

        ResolvedPrompt resolvedPrompt = resolvePrompt(promptHeaders, workspaceId, request);

        AiGatewayChatCompletionRequest effectiveRequest = resolvedPrompt != null
            ? prependSystemMessage(request, resolvedPrompt.content())
            : request;

        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution;

        try {
            modelResolution = resolveModel(effectiveRequest.model());
        } catch (Exception exception) {
            try {
                aiGatewayRequestLogService.create(createErrorLog(effectiveRequest, startTime, exception));
            } catch (Exception logException) {
                logger.error("Failed to log streaming setup error for model '{}'. Original error: {}",
                    effectiveRequest.model(), exception.getMessage(), logException);
            }

            throw exception;
        }

        AiGatewayProvider provider = modelResolution.provider();
        AiGatewayModel model = modelResolution.model();

        AiGatewayProject project = resolveProject(effectiveRequest.tags());

        ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

        AiGatewayChatCompletionRequest processedRequest = compressMessages(effectiveRequest, model, project);

        Prompt prompt = buildPrompt(processedRequest, model.getName());

        AtomicLong streamInputTokens = new AtomicLong(0);
        AtomicLong streamOutputTokens = new AtomicLong(0);
        AtomicReference<Throwable> streamError = new AtomicReference<>();
        StringBuilder streamOutputContent = new StringBuilder();

        return chatModel.stream(prompt)
            .map(chatResponse -> {
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

                AiGatewayChatMessage delta = new AiGatewayChatMessage(
                    AiGatewayChatRole.ASSISTANT, chunkText);

                AiGatewayChatCompletionResponse.Choice choice =
                    new AiGatewayChatCompletionResponse.Choice(0, delta,
                        generation.getMetadata()
                            .getFinishReason());

                return new AiGatewayChatCompletionResponse(
                    UUID.randomUUID()
                        .toString(),
                    "chat.completion.chunk",
                    System.currentTimeMillis() / 1000, effectiveRequest.model(), List.of(choice), null);
            })
            .doOnError(streamError::set)
            .doFinally(signalType -> finalizeStreamRequest(
                signalType, streamInputTokens, streamOutputTokens, streamError, streamOutputContent,
                effectiveRequest, effectiveTracingHeaders, workspaceId, model, provider, project, startTime,
                traceIdHolder));
    }

    private void finalizeStreamRequest(
        reactor.core.publisher.SignalType signalType, AtomicLong streamInputTokens, AtomicLong streamOutputTokens,
        AtomicReference<Throwable> streamError, StringBuilder streamOutputContent,
        AiGatewayChatCompletionRequest effectiveRequest, AiObservabilityTracingHeaders effectiveTracingHeaders,
        Long workspaceId, AiGatewayModel model, AiGatewayProvider provider, AiGatewayProject project,
        long startTime, @Nullable AtomicLong traceIdHolder) {

        int inputTokens = (int) Math.min(streamInputTokens.get(), Integer.MAX_VALUE);
        int outputTokens = (int) Math.min(streamOutputTokens.get(), Integer.MAX_VALUE);

        AiGatewayRequestLog requestLog = new AiGatewayRequestLog(
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

        // On CANCEL, provider-side usage often hasn't been emitted yet, so input/output tokens are partial. Persist
        // cost
        // as null (unknown) rather than $0 so cost dashboards and alert rules exclude the row rather than summing it as
        // zero. Completed and errored streams still record cost — use the model's configured rates as a fallback when
        // the calculator itself fails.
        if (signalType == reactor.core.publisher.SignalType.CANCEL) {
            requestLog.setCost(null);
        } else {
            try {
                BigDecimal cost = aiGatewayCostCalculator.calculateCost(
                    model, inputTokens, outputTokens);

                requestLog.setCost(cost);
            } catch (IllegalStateException illegalStateException) {
                logger.error("Failed to calculate cost for streaming model '{}' — " +
                    "using model's configured rates as fallback to prevent budget bypass",
                    effectiveRequest.model(), illegalStateException);

                BigDecimal fallbackCost = calculateFallbackCost(model, inputTokens, outputTokens);

                requestLog.setCost(fallbackCost);
            }
        }

        setWorkspaceIdFromTags(requestLog, effectiveRequest.tags());
        setProjectIdFromProject(requestLog, project);
        setApiKeyIdFromAuthentication(requestLog);

        try {
            transactionTemplate.executeWithoutResult(
                status -> aiGatewayRequestLogService.create(requestLog));
        } catch (Exception exception) {
            recordRequestLogPersistFailure("streaming", "success");

            logger.error("Failed to persist request log for streaming model '{}'. " +
                "Alert on ai_gateway.request_log.persist_failure{{kind=streaming}}.",
                effectiveRequest.model(), exception);
        }

        boolean streamSuccess = streamError.get() == null;

        String accumulatedOutput = streamOutputContent.toString();

        processStreamingTracingHeaders(
            effectiveTracingHeaders, workspaceId, effectiveRequest, model, provider, inputTokens,
            outputTokens,
            startTime, streamSuccess, accumulatedOutput.isEmpty() ? null : accumulatedOutput, traceIdHolder);

        enforcePostRequestBudget(effectiveRequest.tags());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiGatewayEmbeddingResponse embedding(
        AiGatewayEmbeddingRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders) {

        if (tracingHeaders == null) {
            tracingHeaders = new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of());
        }

        checkBudget(request.tags(), request.model());
        checkRateLimits(request.tags());

        Long workspaceId = resolveWorkspaceIdFromTags(request.tags());

        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution = resolveModel(request.model());

        AiGatewayProvider provider = modelResolution.provider();
        AiGatewayModel model = modelResolution.model();

        EmbeddingModel embeddingModel = aiGatewayEmbeddingModelFactory.getEmbeddingModel(provider);

        EmbeddingRequest embeddingRequest = new EmbeddingRequest(
            request.input(),
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

            AiGatewayRequestLog requestLog = new AiGatewayRequestLog(
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

            setWorkspaceIdFromTags(requestLog, request.tags());
            setApiKeyIdFromAuthentication(requestLog);

            try {
                aiGatewayRequestLogService.create(requestLog);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("embedding", "success");

                logger.error("Failed to persist request log for embedding model '{}' — " +
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
            AiGatewayRequestLog errorLog = new AiGatewayRequestLog(
                UUID.randomUUID()
                    .toString(),
                request.model());

            errorLog.setRoutedModel(model.getName());
            errorLog.setRoutedProvider(provider.getType()
                .name());
            errorLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            errorLog.setStatus(resolveErrorStatus(exception));
            errorLog.setErrorMessage(exception.getMessage());

            setWorkspaceIdFromTags(errorLog, request.tags());
            setApiKeyIdFromAuthentication(errorLog);

            try {
                aiGatewayRequestLogService.create(errorLog);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("embedding", "error");

                logger.error("Failed to log embedding error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            processEmbeddingTracingHeaders(
                tracingHeaders, workspaceId, request, 0, startTime, false);

            throw exception;
        }
    }

    private AiGatewayChatCompletionResponse chatCompletionDirect(AiGatewayChatCompletionRequest request) {
        if (isWorkspaceCachingEnabled(request.tags()) && aiGatewayResponseCache.shouldCache(request)) {
            String cacheKey = aiGatewayResponseCache.computeCacheKey(request);
            AiGatewayChatCompletionResponse cached = aiGatewayResponseCache.get(cacheKey);

            if (cached != null) {
                return cached;
            }
        }

        long startTime = System.currentTimeMillis();

        ModelResolution modelResolution = resolveModel(request.model());

        AiGatewayProvider provider = modelResolution.provider();
        AiGatewayModel model = modelResolution.model();

        AiGatewayProject project = resolveProject(request.tags());

        ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

        AiGatewayChatCompletionRequest processedRequest = compressMessages(request, model, project);

        Prompt prompt = buildPrompt(processedRequest, model.getName());

        try {
            ChatResponse chatResponse = chatModel.call(prompt);

            int[] tokenCounts = extractTokenCounts(chatResponse);

            AiGatewayRequestLog requestLog = createSuccessLog(
                request, model, provider, startTime, tokenCounts[0], tokenCounts[1]);

            setProjectIdFromProject(requestLog, project);

            try {
                aiGatewayRequestLogService.create(requestLog);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("chat", "success");

                logger.error("Failed to persist request log for chat completion model '{}' — " +
                    "cost of ${} will be missing from spend tracking. " +
                    "Alert on ai_gateway.request_log.persist_failure{{kind=chat}}.",
                    request.model(), requestLog.getCost(), logException);
            }

            enforcePostRequestBudget(request.tags());

            AiGatewayChatCompletionResponse response = toResponse(chatResponse, request.model());

            if (isWorkspaceCachingEnabled(request.tags()) && aiGatewayResponseCache.shouldCache(request)) {
                String cacheKey = aiGatewayResponseCache.computeCacheKey(request);

                try {
                    aiGatewayResponseCache.put(cacheKey, response);
                } catch (Exception cacheException) {
                    logger.warn(
                        "Failed to cache chat completion response for model '{}' (key={}); " +
                            "request already succeeded, continuing",
                        request.model(), cacheKey, cacheException);
                }
            }

            return response;
        } catch (Exception exception) {
            try {
                AiGatewayRequestLog errorLog = createErrorLog(request, startTime, exception);

                setProjectIdFromProject(errorLog, project);

                aiGatewayRequestLogService.create(errorLog);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("chat", "error");

                logger.error("Failed to log chat completion error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    private AiGatewayChatCompletionResponse chatCompletionWithRouting(
        AiGatewayChatCompletionRequest request) {

        long startTime = System.currentTimeMillis();

        AiGatewayRoutingPolicy routingPolicy =
            aiGatewayRoutingPolicyService.getRoutingPolicyByName(request.routingPolicy());

        List<AiGatewayModelDeployment> deployments =
            aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(routingPolicy.getId());

        if (deployments.isEmpty()) {
            throw new IllegalStateException(
                "No deployments configured for routing policy: " + routingPolicy.getName());
        }

        Map<Long, AiGatewayModel> modelMap = deployments.stream()
            .map(AiGatewayModelDeployment::getModelId)
            .distinct()
            .collect(Collectors.toMap(
                modelId -> modelId,
                modelId -> aiGatewayModelService.getModel(modelId)));

        List<AiGatewayModelDeployment> enabledDeployments = deployments.stream()
            .filter(AiGatewayModelDeployment::isEnabled)
            .toList();

        if (enabledDeployments.isEmpty()) {
            throw new IllegalStateException(
                "No enabled deployments for routing policy: " + routingPolicy.getName());
        }

        Map<String, Double> latencyByModelName = aiGatewayRequestLogService.getAverageLatencyByModel(
            Instant.now()
                .minus(Duration.ofHours(1)));

        Map<Long, Double> averageLatencyByModelId = modelMap.entrySet()
            .stream()
            .filter(entry -> latencyByModelName.containsKey(entry.getValue()
                .getName()))
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> latencyByModelName.get(entry.getValue()
                .getName())));

        double promptComplexityScore = estimatePromptComplexity(request);

        AiGatewayProject project = resolveProject(request.tags());

        Map<String, String> tags = request.tags() != null ? request.tags() : Map.of();

        Map<Long, String> providerTypeByModelId = modelMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    AiGatewayProvider provider =
                        aiGatewayProviderService.getProvider(entry.getValue()
                            .getProviderId());

                    return provider.getType()
                        .name();
                }));

        AiGatewayRoutingContext routingContext = new AiGatewayRoutingContext(
            averageLatencyByModelId, modelMap, promptComplexityScore, providerTypeByModelId, tags);

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
            return aiGatewayRetryHandler.executeWithRetry(orderedDeployments, deployment -> {
                AiGatewayModel model = modelMap.get(deployment.getModelId());
                AiGatewayProvider provider = aiGatewayProviderService.getProvider(model.getProviderId());

                ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

                AiGatewayChatCompletionRequest processedRequest = compressMessages(request, model, project);

                Prompt prompt = buildPrompt(processedRequest, model.getName());

                ChatResponse chatResponse = chatModel.call(prompt);

                int[] tokenCounts = extractTokenCounts(chatResponse);

                AiGatewayRequestLog requestLog = createSuccessLog(
                    request, model, provider, startTime, tokenCounts[0], tokenCounts[1]);

                setProjectIdFromProject(requestLog, project);

                requestLog.setRoutingPolicyId(routingPolicy.getId());
                requestLog.setRoutingStrategy(routingPolicy.getStrategy());

                try {
                    aiGatewayRequestLogService.create(requestLog);
                } catch (Exception logException) {
                    recordRequestLogPersistFailure("routing", "success");

                    logger.error("Failed to persist request log for routed model '{}' — " +
                        "cost of ${} will be missing from spend tracking. " +
                        "Alert on ai_gateway.request_log.persist_failure{{kind=routing}}.",
                        request.model(), requestLog.getCost(), logException);
                }

                enforcePostRequestBudget(request.tags());

                return toResponse(chatResponse, request.model());
            });
        } catch (Exception exception) {
            try {
                AiGatewayRequestLog errorLog = createErrorLog(request, startTime, exception);

                setProjectIdFromProject(errorLog, project);

                errorLog.setRoutingPolicyId(routingPolicy.getId());
                errorLog.setRoutingStrategy(routingPolicy.getStrategy());

                aiGatewayRequestLogService.create(errorLog);
            } catch (Exception logException) {
                recordRequestLogPersistFailure("routing", "error");

                logger.error("Failed to log routing error for model '{}'. Original error: {}",
                    request.model(), exception.getMessage(), logException);
            }

            throw exception;
        }
    }

    /**
     * Estimates a coarse [0.2, 0.8] complexity score from the aggregate message length. Used by routing strategies that
     * weigh cheap/small models vs. expensive/large models so long prompts preferentially route to bigger models.
     * Extracted from {@code chatCompletionWithRouting} to keep that method under the Checkstyle length limit.
     */
    private static double estimatePromptComplexity(AiGatewayChatCompletionRequest request) {
        int totalChars = request.messages()
            .stream()
            .mapToInt(message -> message.content() != null ? message.content()
                .length() : 0)
            .sum();
        int estimatedTokens = totalChars / 4;

        if (estimatedTokens < 100) {
            return 0.2;
        }

        if (estimatedTokens < 500) {
            return 0.5;
        }

        return 0.8;
    }

    private void checkBudget(Map<String, String> tags, String model) {
        if (tags == null || !tags.containsKey("workspace_id")) {
            throw new IllegalArgumentException(
                "Request for model '" + model + "' is missing required 'workspace_id' tag");
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            // Enforce that the authenticated caller actually belongs to the workspace they're trying to charge.
            // Without this check a caller with any valid API key could spoof workspace_id in the request tags to
            // bill another tenant's budget or evade their own.
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
                logger.warn(
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

            // Workspace membership was already validated in checkBudget for chat/embedding paths; re-run for callers
            // that skipped the budget check.
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
            logger.error(
                "Missing workspace_id tag during post-request budget enforcement — " +
                    "this indicates a bug in the request pipeline");

            return;
        }

        try {
            long workspaceId = Long.parseLong(tags.get("workspace_id"));

            aiGatewayBudgetChecker.recordSpendAndEnforce(workspaceId);
        } catch (RuntimeException exception) {
            // Emit a metric so operators can alert on the silent HARD→SOFT degradation. A DB outage here lets
            // requests continue at full cost for the duration of the outage without any user-visible signal.
            AiGatewayMetrics metrics = aiGatewayMetricsProvider.getIfAvailable();

            if (metrics != null) {
                metrics.incrementPostRequestBudgetFailure();
            }

            logger.error(
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

            List<OpenAiApi.FunctionTool> functionTools = request.tools()
                .stream()
                .map(tool -> new OpenAiApi.FunctionTool(
                    new OpenAiApi.FunctionTool.Function(
                        tool.function()
                            .description(),
                        tool.function()
                            .name(),
                        tool.function()
                            .parameters(),
                        null)))
                .toList();

            openAiOptionsBuilder.tools(functionTools);

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

    private ModelResolution resolveModel(String modelIdentifier) {
        String[] parts = modelIdentifier.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Model must be in format 'provider/model', got: " + modelIdentifier);
        }

        String providerTypeName = parts[0].toUpperCase()
            .replace("-", "_");
        String modelName = parts[1];

        List<AiGatewayProvider> providers = aiGatewayProviderService.getEnabledProviders();

        AiGatewayProvider provider = providers.stream()
            .filter(existingProvider -> existingProvider.getType()
                .name()
                .equalsIgnoreCase(providerTypeName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No enabled provider found for type: " + providerTypeName));

        AiGatewayModel model = aiGatewayModelService.getModel(provider.getId(), modelName);

        return new ModelResolution(provider, model);
    }

    private AiGatewayChatCompletionRequest compressMessages(
        AiGatewayChatCompletionRequest request, AiGatewayModel model,
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
                                byte[] imageBytes = java.util.Base64.getDecoder()
                                    .decode(parts[1]);

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
                            byte[] documentBytes = java.util.Base64.getDecoder()
                                .decode(block.document()
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

        return aiGatewayProjectService.fetchProjectByWorkspaceIdAndSlug(
            Long.parseLong(workspaceId), projectSlug)
            .orElse(null);
    }

    private boolean resolveCompressionEnabled(@Nullable AiGatewayProject project) {
        if (project != null && project.getCompressionEnabled() != null) {
            return project.getCompressionEnabled();
        }

        return true; // system default
    }

    private void setProjectIdFromProject(AiGatewayRequestLog requestLog, @Nullable AiGatewayProject project) {
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

    private void setWorkspaceIdFromTags(AiGatewayRequestLog requestLog, Map<String, String> tags) {
        Long workspaceId = resolveWorkspaceIdFromTags(tags);

        if (workspaceId != null) {
            requestLog.setWorkspaceId(workspaceId);
        }
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

    private AiGatewayRequestLog createSuccessLog(
        AiGatewayChatCompletionRequest request, AiGatewayModel model, AiGatewayProvider provider,
        long startTime, int inputTokens, int outputTokens) {

        AiGatewayRequestLog requestLog = new AiGatewayRequestLog(
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

        setWorkspaceIdFromTags(requestLog, request.tags());
        setApiKeyIdFromAuthentication(requestLog);

        return requestLog;
    }

    /**
     * Reads the authenticated {@code AiGatewayApiKeyAuthenticationToken} from the security context (if any) and stamps
     * the api-key id on the request log. No-ops for non-API-key authentication (e.g. internal session-based callers).
     */
    private static void setApiKeyIdFromAuthentication(AiGatewayRequestLog requestLog) {
        Long apiKeyId = resolveAuthenticatedApiKeyId();

        if (apiKeyId != null) {
            requestLog.setApiKeyId(apiKeyId);
        }
    }

    /**
     * Returns the authenticated API key id from the security context, or {@code null} when the current call is not
     * authenticated via {@code AiGatewayApiKeyAuthenticationToken} (e.g. internal session-based callers, tests).
     */
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

    private AiGatewayRequestLog createErrorLog(
        AiGatewayChatCompletionRequest request, long startTime, Exception exception) {

        AiGatewayRequestLog errorLog = new AiGatewayRequestLog(
            UUID.randomUUID()
                .toString(),
            request.model());

        errorLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        errorLog.setStatus(resolveErrorStatus(exception));
        errorLog.setErrorMessage(exception.getMessage());

        setWorkspaceIdFromTags(errorLog, request.tags());
        setApiKeyIdFromAuthentication(errorLog);

        return errorLog;
    }

    private static BigDecimal calculateFallbackCost(AiGatewayModel model, int inputTokens, int outputTokens) {
        BigDecimal millionTokens = BigDecimal.valueOf(1_000_000);

        BigDecimal inputRate = model.getInputCostPerMTokens();
        BigDecimal outputRate = model.getOutputCostPerMTokens();

        if (inputRate == null || outputRate == null) {
            logger.error(
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
        @Nullable ResolvedPrompt resolvedPrompt) {

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
            span.setEndTime(Instant.now());
            span.setStatus(success ? AiObservabilitySpanStatus.COMPLETED : AiObservabilitySpanStatus.ERROR);

            if (tracingHeaders.parentSpanId() != null) {
                try {
                    span.setParentSpanId(Long.parseLong(tracingHeaders.parentSpanId()));
                } catch (NumberFormatException numberFormatException) {
                    logger.warn("Invalid parentSpanId header value '{}' — ignoring",
                        tracingHeaders.parentSpanId());
                }
            }

            if (resolvedPrompt != null) {
                span.setPromptId(resolvedPrompt.promptId());
                span.setPromptVersionId(resolvedPrompt.promptVersionId());
            }

            aiObservabilitySpanService.create(span);

            if (trace.getStatus() == AiObservabilityTraceStatus.COMPLETED
                || trace.getStatus() == AiObservabilityTraceStatus.ERROR) {

                try {
                    publishTraceCompletedEvent(trace, request.model());
                } catch (Exception publishException) {
                    logger.error(
                        "Failed to publish trace-completed event for model '{}' (traceId={})",
                        request.model(), trace.getId(), publishException);
                }
            }

            if (trace.getStatus() == AiObservabilityTraceStatus.COMPLETED) {
                try {
                    aiEvalExecutor.evaluateTrace(trace.getId(), workspaceId);
                } catch (Exception evalException) {
                    logger.error(
                        "Failed to dispatch eval for model '{}' (traceId={})",
                        request.model(), trace.getId(), evalException);
                }
            }
        } catch (Exception exception) {
            logger.error(
                "Failed to persist trace/span for model '{}' — tracing data will be missing",
                request.model(), exception);
        }
    }

    private void publishTraceCompletedEvent(AiObservabilityTrace trace, String modelName) {
        applicationEventPublisher.publishEvent(new AiGatewayTraceCompletedEvent(
            trace.getWorkspaceId(),
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
        AiGatewayModel model,
        AiGatewayProvider provider,
        int inputTokens,
        int outputTokens,
        long startTime,
        boolean success,
        @Nullable String outputText,
        @Nullable AtomicLong traceIdHolder) {

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
            span.setEndTime(Instant.now());
            span.setStatus(success ? AiObservabilitySpanStatus.COMPLETED : AiObservabilitySpanStatus.ERROR);

            if (tracingHeaders.parentSpanId() != null) {
                try {
                    span.setParentSpanId(Long.parseLong(tracingHeaders.parentSpanId()));
                } catch (NumberFormatException numberFormatException) {
                    logger.warn("Invalid parentSpanId header value '{}' — ignoring",
                        tracingHeaders.parentSpanId());
                }
            }

            transactionTemplate.executeWithoutResult(
                status -> aiObservabilitySpanService.create(span));
        } catch (Exception exception) {
            logger.error("Failed to process streaming tracing headers for model '{}' — tracing data will be missing",
                request.model(), exception);
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
            span.setEndTime(Instant.now());
            span.setStatus(success ? AiObservabilitySpanStatus.COMPLETED : AiObservabilitySpanStatus.ERROR);

            if (tracingHeaders.parentSpanId() != null) {
                try {
                    span.setParentSpanId(Long.parseLong(tracingHeaders.parentSpanId()));
                } catch (NumberFormatException numberFormatException) {
                    logger.warn("Invalid parentSpanId header value '{}' — ignoring",
                        tracingHeaders.parentSpanId());
                }
            }

            aiObservabilitySpanService.create(span);
        } catch (Exception exception) {
            logger.error("Failed to process tracing headers for embedding model '{}' — tracing data will be missing",
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
            AiObservabilityTrace existingTrace = aiObservabilityTraceService
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

        AiObservabilityTrace trace = new AiObservabilityTrace(workspaceId, AiObservabilityTraceSource.API);

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
            AiObservabilitySession session = aiObservabilitySessionService.getOrCreateSessionByExternalId(
                workspaceId, tracingHeaders.sessionId(), project != null ? project.getId() : null,
                tracingHeaders.userId());

            trace.setSessionId(session.getId());
        }

        if (!tracingHeaders.tagNames()
            .isEmpty()) {

            // Resolve incoming tag names against workspace-scoped AiGatewayTag rows; missing names are auto-created
            // so callers can attach freeform tags without first calling the management API. Same workspace-scoped
            // semantics as the management UI.
            Set<AiObservabilityTraceTag> traceTags = tracingHeaders.tagNames()
                .stream()
                .map(name -> aiGatewayTagService.findByWorkspaceIdAndName(workspaceId, name)
                    .orElseGet(() -> aiGatewayTagService.create(new AiGatewayTag(workspaceId, name))))
                .map(savedTag -> new AiObservabilityTraceTag(savedTag.getId()))
                .collect(Collectors.toCollection(HashSet::new));

            trace.setTags(traceTags);
        }

        // Under READ_COMMITTED two concurrent requests with the same external trace id can both see an empty
        // findByExternalTraceId and both attempt insert. The UNIQUE constraint turns the loser's insert into a
        // DataIntegrityViolationException; we resolve the race by re-finding and merging this request's usage onto
        // the winning row instead of bubbling a 500.
        if (tracingHeaders.hasExternalTraceId()) {
            try {
                aiObservabilityTraceService.create(trace);
            } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
                AiObservabilityTrace winner = aiObservabilityTraceService
                    .findByExternalTraceId(workspaceId, externalTraceId)
                    .orElseThrow(() -> duplicate);

                mergeIntoExistingTrace(winner, trace);

                aiObservabilityTraceService.update(winner);

                return winner;
            }
        } else {
            aiObservabilityTraceService.create(trace);
        }

        return trace;
    }

    /**
     * Returns {@code true} when the workspace's {@code cacheEnabled} setting is unset or true. Defaults to true so
     * workspaces without explicit settings keep the gateway's existing cache behavior. Callers must still gate on
     * {@link AiGatewayResponseCache#shouldCache} for the request-level checks (deterministic, non-streaming).
     */
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

    /**
     * Reads the workspace's {@code redactPii} setting (defaults to false). A missing workspace id is treated as "don't
     * redact" — workspace-unattributed traces are already at the edge of the observability contract.
     */
    private boolean isPiiRedactionEnabled(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return false;
        }

        return aiGatewayWorkspaceSettingsService.findByWorkspaceId(workspaceId)
            .map(settings -> Boolean.TRUE.equals(settings.redactPii()))
            .orElse(false);
    }

    /**
     * Returns a short placeholder that records the fact of content + its length + a SHA-256 digest so operators can
     * distinguish payloads without seeing them. Null/empty input stays null/empty.
     */
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
            // SHA-256 is guaranteed in JDK 8+; fall back to length-only rather than bubble.
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

    /**
     * Computes the cost of a trace. Returns {@code null} only when the cost cannot be computed; callers MUST treat
     * {@code null} as "unknown" and never as zero — cost-based dashboards and alert rules should filter/exclude rows
     * where cost is null rather than summing them as $0.
     */
    @Nullable
    private BigDecimal calculateTraceCost(String modelIdentifier, int inputTokens, int outputTokens) {
        try {
            ModelResolution modelResolution = resolveModel(modelIdentifier);

            return aiGatewayCostCalculator.calculateCost(
                modelResolution.model(), inputTokens, outputTokens);
        } catch (Exception exception) {
            logger.error(
                "Failed to calculate cost for trace — model '{}'; persisting null (unknown) cost. " +
                    "Cost dashboards and cost-based alert rules MUST exclude null cost rows.",
                modelIdentifier, exception);

            return null;
        }
    }

    /**
     * Validates that the URL points to a public, non-internal host and returns the input URL unchanged. Delegates to
     * {@link AiGatewayUrlValidator} which is the shared SSRF guard used across the gateway (webhook delivery, provider
     * connectivity tests, image/document URL resolution). Earlier revisions rewrote the URL to a resolved IP literal to
     * defeat DNS rebinding, but that broke HTTPS SNI and is now an accepted residual risk (see
     * {@link AiGatewayUrlValidator} Javadoc).
     */
    static String validateExternalUrl(String url) {
        AiGatewayUrlValidator.validateExternalUrl(url);

        return url;
    }

    private AiGatewayChatCompletionRequest prependSystemMessage(
        AiGatewayChatCompletionRequest request, String systemContent) {

        List<AiGatewayChatMessage> existingMessages = request.messages();

        List<AiGatewayChatMessage> updatedMessages = new ArrayList<>();

        updatedMessages.add(new AiGatewayChatMessage(AiGatewayChatRole.SYSTEM, systemContent));

        for (AiGatewayChatMessage message : existingMessages) {
            if (message.role() != AiGatewayChatRole.SYSTEM) {
                updatedMessages.add(message);
            }
        }

        return new AiGatewayChatCompletionRequest(
            request.model(), updatedMessages, request.temperature(), request.maxTokens(),
            request.topP(), request.stream(), request.routingPolicy(),
            request.cache(), request.toolChoice(), request.tools(), request.tags());
    }

    private ResolvedPrompt resolvePrompt(
        @Nullable AiPromptHeaders promptHeaders, Long workspaceId,
        AiGatewayChatCompletionRequest request) {

        if (promptHeaders == null || !promptHeaders.hasPromptEnabled()) {
            return null;
        }

        AiGatewayProject project = resolveProject(request.tags());

        Long projectId = project != null ? project.getId() : null;

        Optional<AiPrompt> promptOptional = aiPromptService.getPromptByName(
            workspaceId, projectId, promptHeaders.promptName());

        if (promptOptional.isEmpty()) {
            throw new IllegalArgumentException(
                "Prompt not found: " + promptHeaders.promptName());
        }

        AiPrompt prompt = promptOptional.get();

        String environment = promptHeaders.resolvedEnvironment();

        Optional<AiPromptVersion> activeVersionOptional =
            aiPromptVersionService.getActiveVersion(prompt.getId(), environment);

        if (activeVersionOptional.isEmpty()) {
            throw new IllegalArgumentException(
                "No active prompt version found for prompt '" + promptHeaders.promptName() +
                    "' in environment '" + environment + "'");
        }

        AiPromptVersion activeVersion = activeVersionOptional.get();

        Map<String, Object> requestVariables = request.tags() != null
            ? new java.util.HashMap<>(request.tags())
            : Map.of();

        String resolvedContent = substituteVariables(activeVersion.getContent(), requestVariables);

        return new ResolvedPrompt(prompt.getId(), activeVersion.getId(), resolvedContent);
    }

    private String substituteVariables(String content, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return content;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : matcher.group(0);

            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private record ModelResolution(AiGatewayProvider provider, AiGatewayModel model) {
    }

    private record ResolvedPrompt(Long promptId, Long promptVersionId, String content) {
    }
}
