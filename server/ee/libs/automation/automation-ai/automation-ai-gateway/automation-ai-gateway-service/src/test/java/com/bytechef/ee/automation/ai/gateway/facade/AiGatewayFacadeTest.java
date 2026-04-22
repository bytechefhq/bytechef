/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker.BudgetCheckResult;
import com.bytechef.ee.automation.ai.gateway.cache.AiGatewayResponseCache;
import com.bytechef.ee.automation.ai.gateway.compression.AiGatewayContextCompressor;
import com.bytechef.ee.automation.ai.gateway.cost.AiGatewayCostCalculator;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayContentBlockType;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayEmbeddingRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayEmbeddingResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiObservabilityTracingHeaders;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.automation.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.reliability.AiGatewayRetryHandler;
import com.bytechef.ee.automation.ai.gateway.routing.AiGatewayRouter;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelDeploymentService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProjectService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRequestLogService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySessionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptService;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptVersionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayFacadeTest {

    @Mock
    private AiEvalExecutor aiEvalExecutor;

    @Mock
    private AiGatewayBudgetChecker aiGatewayBudgetChecker;

    @Mock
    private AiGatewayChatModelFactory aiGatewayChatModelFactory;

    @Mock
    private AiGatewayContextCompressor aiGatewayContextCompressor;

    @Mock
    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @Mock
    private AiGatewayCostCalculator aiGatewayCostCalculator;

    @Mock
    private AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory;

    @Mock
    private AiGatewayModelDeploymentService aiGatewayModelDeploymentService;

    @Mock
    private AiGatewayModelService aiGatewayModelService;

    @Mock
    private AiGatewayProjectService aiGatewayProjectService;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private AiGatewayRequestLogService aiGatewayRequestLogService;

    @Mock
    private AiGatewayResponseCache aiGatewayResponseCache;

    @Mock
    private AiGatewayRetryHandler aiGatewayRetryHandler;

    @Mock
    private AiGatewayRouter aiGatewayRouter;

    @Mock
    private AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @Mock
    private AiPromptService aiPromptService;

    @Mock
    private AiPromptVersionService aiPromptVersionService;

    @Mock
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Mock
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Mock
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private PermissionService permissionService;

    private AiGatewayFacade aiGatewayFacade;

    @BeforeEach
    void setUp() {
        // Seed a tenant-admin SecurityContext so validateWorkspaceAccess passes — tests don't exercise auth rules,
        // they exercise gateway behavior, so an admin bypass keeps the tests focused on the actual subject under test.
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        // lenient() — isTenantAdmin is read only on requests that actually hit validateWorkspaceAccess; tests that
        // short-circuit before reaching it (e.g. missing-tag validation) don't exercise this stub.
        org.mockito.Mockito.lenient()
            .when(permissionService.isTenantAdmin())
            .thenReturn(true);

        aiGatewayFacade = new AiGatewayFacade(
            aiEvalExecutor, aiGatewayBudgetChecker, aiGatewayRateLimitChecker,
            aiGatewayChatModelFactory, aiGatewayContextCompressor,
            aiGatewayCostCalculator, aiGatewayEmbeddingModelFactory, aiGatewayModelDeploymentService,
            aiGatewayModelService, aiGatewayProjectService, aiGatewayProviderService,
            aiGatewayRequestLogService, aiGatewayResponseCache, aiGatewayRetryHandler,
            aiGatewayRouter, aiGatewayRoutingPolicyService, aiPromptService,
            aiPromptVersionService, aiObservabilitySessionService,
            aiObservabilitySpanService, aiObservabilityTraceService,
            mock(com.bytechef.ee.automation.ai.gateway.service.AiGatewayTagService.class),
            mock(com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService.class),
            emptyMetricsProvider(),
            new org.springframework.context.support.GenericApplicationContext(),
            permissionService,
            transactionManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private static
        org.springframework.beans.factory.ObjectProvider<com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics>
        emptyMetricsProvider() {

        org.springframework.beans.factory.ObjectProvider<com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics> provider =
            (org.springframework.beans.factory.ObjectProvider<com.bytechef.ee.automation.ai.gateway.metrics.AiGatewayMetrics>) mock(
                org.springframework.beans.factory.ObjectProvider.class);

        // lenient() — some tests never reach a request-log catch site and Mockito strict mode flags the stubbing
        // as "unnecessary" even though it's semantically required by the production helper
        // recordRequestLogPersistFailure.
        org.mockito.Mockito.lenient()
            .when(provider.getIfAvailable())
            .thenReturn(null);

        return provider;
    }

    @Test
    void testChatCompletionThrowsWhenTagsMissingWorkspaceId() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, Map.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertEquals(
            "Request for model 'openai/gpt-4' is missing required 'workspace_id' tag", exception.getMessage());
    }

    @Test
    void testChatCompletionThrowsWhenTagsAreNull() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertEquals(
            "Request for model 'openai/gpt-4' is missing required 'workspace_id' tag", exception.getMessage());
    }

    @Test
    void testChatCompletionThrowsWhenWorkspaceIdNotANumber() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, Map.of("workspace_id", "not-a-number"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertEquals("Invalid workspace_id tag: not-a-number", exception.getMessage());
    }

    @Test
    void testChatCompletionThrowsWhenBudgetExceeded() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(
            BudgetCheckResult.rejected(new BigDecimal("110"), new BigDecimal("100"), BigDecimal.valueOf(110)));

        BudgetExceededException exception = assertThrows(
            BudgetExceededException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertNotNull(exception.getMessage());
        assertEquals(
            "Budget limit exceeded for workspace 1. Current spend: $110 / Budget: $100",
            exception.getMessage());
    }

    @Test
    void testResolveModelThrowsForMalformedModelString() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "gpt-4-no-slash", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, Map.of("workspace_id", "1"));

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertEquals("Model must be in format 'provider/model', got: gpt-4-no-slash", exception.getMessage());
    }

    @Test
    void testResolveModelThrowsWhenProviderTypeNotFound() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertEquals("No enabled provider found for type: OPENAI", exception.getMessage());
    }

    @Test
    void testChatCompletionDirectSuccess() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);
        assertEquals("openai/gpt-4", response.model());
        assertEquals(1, response.choices()
            .size());
        assertEquals("Hello", response.choices()
            .get(0)
            .message()
            .content());

        verify(aiGatewayRequestLogService).create(any());
    }

    @Test
    void testChatCompletionDirectErrorCreatesErrorLog() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API call failed"));

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        verify(aiGatewayRequestLogService).create(any());
    }

    @Test
    void testChatCompletionReturnsCachedResponseOnCacheHit() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(true);
        when(aiGatewayResponseCache.computeCacheKey(any())).thenReturn("test-cache-key");

        AiGatewayChatCompletionResponse cachedResponse = new AiGatewayChatCompletionResponse(
            "cached-id", "chat.completion", 1000L, "openai/gpt-4",
            List.of(new AiGatewayChatCompletionResponse.Choice(
                0, new AiGatewayChatMessage("assistant", "Cached response"), "stop")),
            null);

        when(aiGatewayResponseCache.get("test-cache-key")).thenReturn(cachedResponse);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertEquals("Cached response", response.choices()
            .get(0)
            .message()
            .content());

        verify(aiGatewayChatModelFactory, never()).getChatModel(any());
    }

    @Test
    void testChatCompletionCachesMissedResponseOnCacheMiss() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(true);
        when(aiGatewayResponseCache.computeCacheKey(any())).thenReturn("test-cache-key");
        when(aiGatewayResponseCache.get("test-cache-key")).thenReturn(null);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);

        verify(aiGatewayResponseCache).put(anyString(), any(AiGatewayChatCompletionResponse.class));
    }

    @Test
    void testChatCompletionErrorLoggingResilienceStillThrowsOriginalException() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API call failed"));
        doThrow(new RuntimeException("DB error")).when(aiGatewayRequestLogService)
            .create(any());

        RuntimeException exception = assertThrows(
            RuntimeException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertEquals("API call failed", exception.getMessage());
    }

    @Test
    void testChatCompletionWithUnsupportedContentBlockTypeLogsWarning() {
        Map<String, String> tags = Map.of("workspace_id", "1");
        AiGatewayChatMessage multimodalMessage = new AiGatewayChatMessage(
            AiGatewayChatRole.USER, null,
            List.of(new AiGatewayContentBlock(AiGatewayContentBlockType.TEXT, "fallback text", null, null)),
            null, null);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(multimodalMessage),
            null, null, null, false, null, null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class)))
            .thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);

        verify(aiGatewayRequestLogService).create(any());
    }

    // --- Issue 17: Streaming tests ---

    @Test
    void testChatCompletionStreamSuccess() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse streamChunk = mockStreamChunk("Hello ");
        ChatResponse streamChunkFinal = mockStreamChunk("World");

        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk, streamChunkFinal));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));

        Flux<AiGatewayChatCompletionResponse> responseFlux =
            aiGatewayFacade.chatCompletionStream(request, null);

        StepVerifier.create(responseFlux)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void testChatCompletionStreamBudgetCheckBeforeStreaming() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(
            BudgetCheckResult.rejected(new BigDecimal("110"), new BigDecimal("100"), BigDecimal.valueOf(110)));

        // chatCompletionStream returns Flux.defer(...) — the budget check fires on subscription, not on method call.
        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null))
            .expectError(BudgetExceededException.class)
            .verify();

        verify(aiGatewayChatModelFactory, never()).getChatModel(any());
    }

    @Test
    void testChatCompletionStreamModelResolutionErrorCreatesLog() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        // Same rationale as above — model resolution runs inside the deferred flux.
        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null))
            .expectError(IllegalArgumentException.class)
            .verify();

        verify(aiGatewayRequestLogService).create(any());
    }

    // --- Issue 18: Embedding tests ---

    @Test
    void testEmbeddingSuccess() {
        AiGatewayEmbeddingRequest request = createEmbeddingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);

        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        when(aiGatewayEmbeddingModelFactory.getEmbeddingModel(any())).thenReturn(embeddingModel);

        EmbeddingResponse embeddingResponse = mockEmbeddingResponse();

        when(embeddingModel.call(any(org.springframework.ai.embedding.EmbeddingRequest.class)))
            .thenReturn(embeddingResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.001"));

        AiGatewayEmbeddingResponse response = aiGatewayFacade.embedding(request, null);

        assertNotNull(response);
        assertEquals("list", response.object());
        assertEquals("openai/gpt-4", response.model());
        assertFalse(response.data()
            .isEmpty());

        verify(aiGatewayRequestLogService).create(any());
    }

    @Test
    void testEmbeddingBudgetCheckBeforeExecution() {
        AiGatewayEmbeddingRequest request = createEmbeddingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(
            BudgetCheckResult.rejected(new BigDecimal("110"), new BigDecimal("100"), BigDecimal.valueOf(110)));

        assertThrows(BudgetExceededException.class, () -> aiGatewayFacade.embedding(request, null));

        verify(aiGatewayEmbeddingModelFactory, never()).getEmbeddingModel(any());
    }

    @Test
    void testEmbeddingErrorCreatesErrorLog() {
        AiGatewayEmbeddingRequest request = createEmbeddingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);

        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        when(aiGatewayEmbeddingModelFactory.getEmbeddingModel(any())).thenReturn(embeddingModel);
        when(embeddingModel.call(any(org.springframework.ai.embedding.EmbeddingRequest.class)))
            .thenThrow(new RuntimeException("Embedding API failed"));

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.embedding(request, null));

        verify(aiGatewayRequestLogService).create(any());
    }

    // --- Issue 19: Routing tests ---

    @Test
    void testChatCompletionWithRoutingDelegatesToRetryHandler() {
        Map<String, String> tags = Map.of("workspace_id", "1");

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "my-routing-policy", null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "my-routing-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("my-routing-policy"))
            .thenReturn(routingPolicy);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiGatewayModelService.getModel(1L)).thenReturn(model);
        when(aiGatewayProviderService.getProvider(provider.getId())).thenReturn(provider);
        when(aiGatewayRequestLogService.getAverageLatencyByModel(any(Instant.class)))
            .thenReturn(Map.of());
        when(aiGatewayRouter.route(any(), any(), any())).thenReturn(deployment);
        when(aiGatewayRetryHandler.executeWithRetry(any(), any())).thenAnswer(invocation -> {
            AiGatewayChatCompletionResponse mockResponse = new AiGatewayChatCompletionResponse(
                "test-id", "chat.completion", 1000L, "openai/gpt-4",
                List.of(new AiGatewayChatCompletionResponse.Choice(
                    0, new AiGatewayChatMessage("assistant", "Routed response"), "stop")),
                null);

            return mockResponse;
        });

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);
        assertEquals("Routed response", response.choices()
            .get(0)
            .message()
            .content());

        verify(aiGatewayRouter).route(any(), any(), any());
        verify(aiGatewayRetryHandler).executeWithRetry(any(), any());
    }

    @Test
    void testChatCompletionWithRoutingThrowsWhenNoDeployments() {
        Map<String, String> tags = Map.of("workspace_id", "1");

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "empty-policy", null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "empty-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("empty-policy"))
            .thenReturn(routingPolicy);
        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertTrue(exception.getMessage()
            .contains("No deployments configured"));
    }

    // --- Tracing tests ---

    @Test
    void testChatCompletionWithTracingHeadersCreatesTraceAndSpan() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);
        when(aiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 100L);

            return null;
        }).when(aiObservabilityTraceService)
            .create(any(AiObservabilityTrace.class));

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "test-trace-1", null, "test-span", null, "user-1", Map.of(), List.of());

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, tracingHeaders);

        assertNotNull(response);

        verify(aiObservabilityTraceService).create(any(AiObservabilityTrace.class));
        verify(aiObservabilitySpanService).create(any(AiObservabilitySpan.class));
    }

    @Test
    void testChatCompletionWithSameTraceIdUpdatesExistingTrace() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiGatewayModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiGatewayModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiObservabilityTrace existingTrace = new AiObservabilityTrace(1L, AiObservabilityTraceSource.API);

        ReflectionTestUtils.setField(existingTrace, "id", 42L);

        existingTrace.setExternalTraceId("test-trace-1");
        existingTrace.setTotalInputTokens(10);
        existingTrace.setTotalOutputTokens(5);
        existingTrace.setTotalLatencyMs(100);
        existingTrace.setTotalCost(BigDecimal.ONE);

        when(aiObservabilityTraceService.findByExternalTraceId(1L, "test-trace-1"))
            .thenReturn(Optional.of(existingTrace));

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "test-trace-1", null, "test-span", null, "user-1", Map.of(), List.of());

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, tracingHeaders);

        assertNotNull(response);

        verify(aiObservabilityTraceService, never()).create(any(AiObservabilityTrace.class));
        verify(aiObservabilityTraceService).update(any(AiObservabilityTrace.class));
        verify(aiObservabilitySpanService).create(any(AiObservabilitySpan.class));
    }

    private AiGatewayChatCompletionRequest createStreamingRequest() {
        Map<String, String> tags = Map.of("workspace_id", "1");

        return new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, true, null, null, null, null, tags);
    }

    private AiGatewayEmbeddingRequest createEmbeddingRequest() {
        return new AiGatewayEmbeddingRequest(
            "openai/gpt-4", List.of("Hello world"), Map.of("workspace_id", "1"));
    }

    private ChatResponse mockStreamChunk(String content) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = new AssistantMessage(content);

        when(generation.getOutput()).thenReturn(assistantMessage);
        when(generation.getMetadata()).thenReturn(
            ChatGenerationMetadata.builder()
                .finishReason(null)
                .build());
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatResponse.getMetadata()).thenReturn(null);

        return chatResponse;
    }

    private EmbeddingResponse mockEmbeddingResponse() {
        EmbeddingResponse embeddingResponse = mock(EmbeddingResponse.class);
        Embedding embedding = mock(Embedding.class);

        when(embedding.getOutput()).thenReturn(new float[] {
            0.1f, 0.2f, 0.3f
        });
        when(embedding.getIndex()).thenReturn(0);
        when(embeddingResponse.getResults()).thenReturn(List.of(embedding));

        EmbeddingResponseMetadata metadata = mock(EmbeddingResponseMetadata.class);
        Usage usage = mock(Usage.class);

        when(usage.getPromptTokens()).thenReturn(5);
        when(metadata.getUsage()).thenReturn(usage);
        when(embeddingResponse.getMetadata()).thenReturn(metadata);

        return embeddingResponse;
    }

    private AiGatewayChatCompletionRequest createDefaultRequest() {
        Map<String, String> tags = Map.of("workspace_id", "1");

        return new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, tags);
    }

    private AiGatewayProvider createProvider() {
        AiGatewayProvider provider = new AiGatewayProvider(
            "OpenAI", AiGatewayProviderType.OPENAI, "test-api-key");

        ReflectionTestUtils.setField(provider, "id", 1L);

        return provider;
    }

    private AiGatewayModel createModel(AiGatewayProvider provider) {
        AiGatewayModel model = new AiGatewayModel(provider.getId(), "gpt-4");

        model.setContextWindow(128000);

        ReflectionTestUtils.setField(model, "id", 1L);

        return model;
    }

    private ChatResponse mockChatResponse() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = new AssistantMessage("Hello");

        when(generation.getOutput()).thenReturn(assistantMessage);
        when(generation.getMetadata()).thenReturn(
            ChatGenerationMetadata.builder()
                .finishReason("stop")
                .build());
        when(chatResponse.getResult()).thenReturn(generation);
        when(chatResponse.getMetadata()).thenReturn(mock(ChatResponseMetadata.class));

        return chatResponse;
    }
}
