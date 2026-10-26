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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker.BudgetCheckResult;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.platform.ai.gateway.cache.AiGatewayResponseCache;
import com.bytechef.ee.platform.ai.gateway.compression.AiGatewayContextCompressor;
import com.bytechef.ee.platform.ai.gateway.cost.AiGatewayCostCalculator;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.gateway.domain.BudgetExceededException;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayContentBlock;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayContentBlockType;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingResponse;
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
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayFacadeTest {

    private static final long ENVIRONMENT_ID = 1L;

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
    private AiModelService aiModelService;

    @Mock
    private WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private AiGatewayProviderResolver aiGatewayProviderResolver;

    @Mock
    private AiLlmUsageService aiGatewayRequestLogService;

    @Mock
    private AiGatewayResponseCache aiGatewayResponseCache;

    @Mock
    private AiGatewayRetryHandler aiGatewayRetryHandler;

    @Mock
    private AiGatewayRouter aiGatewayRouter;

    @Mock
    private AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @Mock
    private PromptComplexityScorer promptComplexityScorer;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private PermissionService permissionService;

    private AiGatewayFacade aiGatewayFacade;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        org.mockito.Mockito.lenient()
            .when(permissionService.isTenantAdmin())
            .thenReturn(true);

        org.mockito.Mockito.lenient()
            .when(aiGatewayProviderResolver.resolveTenantProvider(any()))
            .thenAnswer(invocation -> {
                AiGatewayProviderType type = invocation.getArgument(0);

                return aiGatewayProviderService.getEnabledProviders()
                    .stream()
                    .filter(provider -> provider.getType() == type)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No enabled provider found for type: " + type));
            });

        aiGatewayFacade = buildFacade(guardrails(false));
    }

    private AiGatewayFacadeImpl buildFacade(
        com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails guardrails) {
        return new AiGatewayFacadeImpl(
            aiGatewayBudgetChecker, aiGatewayRateLimitChecker, aiGatewayChatModelFactory, aiGatewayContextCompressor,
            aiGatewayCostCalculator, guardrails, aiGatewayEmbeddingModelFactory, aiGatewayModelDeploymentService,
            aiModelService, workspaceAiGatewayProjectService, aiGatewayProviderService, aiGatewayProviderResolver,
            aiGatewayRequestLogService, aiGatewayResponseCache, aiGatewayRetryHandler, aiGatewayRouter,
            aiGatewayRoutingPolicyService, promptComplexityScorer,
            mock(com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService.class),
            emptyMetricsProvider(), permissionService, transactionManager);
    }

    private static com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails guardrails(
        boolean responseScanEnabled) {
        com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService settingsService =
            mock(com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService.class);

        com.bytechef.ee.platform.ai.guardrails.AiGuardrails aiGuardrails =
            new com.bytechef.ee.platform.ai.guardrails.AiGuardrails(
                settingsService, null, null, null, true, false, "", false, false, responseScanEnabled, false);

        return new com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails(
            aiGuardrails, null, settingsService, null, null, null, false, false);
    }

    private static com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails
        guardrailsWithPiiTokenizationAndResponseScan() {
        com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService settingsService =
            mock(com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService.class);

        com.bytechef.ee.platform.ai.guardrails.AiGuardrails aiGuardrails =
            new com.bytechef.ee.platform.ai.guardrails.AiGuardrails(
                settingsService, null, null, null, true, false, "", false, false, true, false);

        return new com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails(
            aiGuardrails, null, settingsService, null, null, null, false, false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private static
        ObjectProvider<com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics>
        emptyMetricsProvider() {
        ObjectProvider<com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics> provider =
            (ObjectProvider<com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics>) mock(
                ObjectProvider.class);

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
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request));

        assertEquals(
            "Request for model 'openai/gpt-4' is missing required 'workspace_id' tag", exception.getMessage());
    }

    @Test
    void testChatCompletionThrowsWhenTagsAreNull() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request));

        assertEquals(
            "Request for model 'openai/gpt-4' is missing required 'workspace_id' tag", exception.getMessage());
    }

    @Test
    void testChatCompletionThrowsWhenWorkspaceIdNotANumber() {
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, Map.of("workspace_id", "not-a-number"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request));

        assertEquals("Invalid workspace_id tag: not-a-number", exception.getMessage());
    }

    @Test
    void testChatCompletionThrowsWhenBudgetExceeded() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(
            BudgetCheckResult.rejected(new BigDecimal("110"), new BigDecimal("100"), BigDecimal.valueOf(110)));

        BudgetExceededException exception = assertThrows(
            BudgetExceededException.class, () -> aiGatewayFacade.chatCompletion(request));

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
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request));

        assertEquals("Model must be in format 'provider/model', got: gpt-4-no-slash", exception.getMessage());
    }

    @Test
    void testResolveModelThrowsWhenProviderTypeNotFound() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request));

        assertEquals("No enabled provider found for type: OPENAI", exception.getMessage());
    }

    @Test
    void testChatCompletionDirectSuccess() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        assertNotNull(response);
        assertEquals("openai/gpt-4", response.model());
        assertEquals(1, response.choices()
            .size());
        assertEquals("Hello", response.choices()
            .get(0)
            .message()
            .content());

        verify(aiGatewayRequestLogService).create(any(), any());
    }

    @Test
    void testChatCompletionRedactsResponseWhenResponseScanningEnabled() {
        AiGatewayFacade facade = buildFacade(guardrails(true));

        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        ChatResponse chatResponse = mockChatResponse("Contact bob@acme.io");

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request);

        assertEquals("Contact [REDACTED_EMAIL_ADDRESS]", response.choices()
            .get(0)
            .message()
            .content());
    }

    @Test
    void testChatCompletionTokenizesRequestAndRestoresRoundTrippedTokenInResponse() {
        AiGatewayFacade facade = buildFacade(guardrailsWithPiiTokenizationAndResponseScan());

        Map<String, String> tags = Map.of("workspace_id", "1");
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Contact bob@acme.io")),
            null, null, null, false, null, null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt sentPrompt = invocation.getArgument(0);
            String sentText = sentPrompt.getInstructions()
                .stream()
                .map(Message::getText)
                .collect(Collectors.joining(" "));

            Matcher matcher = PiiToken.pattern()
                .matcher(sentText);

            assertTrue(matcher.find(), "expected the sent prompt to carry a PII token, got: " + sentText);

            return mockChatResponse("Sure, reaching out to " + matcher.group() + " shortly.");
        });

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request);

        assertEquals("Sure, reaching out to bob@acme.io shortly.", response.choices()
            .get(0)
            .message()
            .content());
    }

    @Test
    void testChatCompletionClosesTokenSessionOnDownstreamError() {
        com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails mockGuardrails =
            mock(com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails.class);
        PiiTokenSession session = PiiTokenSession.create();

        when(mockGuardrails.newTokenSession()).thenReturn(session);
        when(mockGuardrails.apply(any(), any(), any(), eq(session))).thenAnswer(invocation -> {
            session.tokenFor("EMAIL", "bob@acme.io");

            return invocation.getArgument(0);
        });

        AiGatewayFacade facade = buildFacade(mockGuardrails);

        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API call failed"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> facade.chatCompletion(request));

        assertEquals("API call failed", thrown.getMessage());
        assertEquals(0, session.size());
    }

    @Test
    void testChatCompletionDirectErrorCreatesErrorLog() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API call failed"));

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.chatCompletion(request));

        verify(aiGatewayRequestLogService).create(any(), any());
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

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
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        assertNotNull(response);

        verify(aiGatewayResponseCache).put(anyString(), any(AiGatewayChatCompletionResponse.class));
    }

    @Test
    void testChatCompletionSkipsCacheWhenRequestContainsPiiToken() {
        AiGatewayFacade facade = buildFacade(guardrailsWithPiiTokenizationAndResponseScan());

        Map<String, String> tags = Map.of("workspace_id", "1");
        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Contact bob@acme.io")),
            null, null, null, false, null, null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(true);
        org.mockito.Mockito.lenient()
            .when(aiGatewayResponseCache.computeCacheKey(any()))
            .thenReturn("pii-bearing-request-key");

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse("Sure, I'll reach out.");

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request);

        assertNotNull(response);

        verify(aiGatewayResponseCache, never()).put(anyString(), any(AiGatewayChatCompletionResponse.class));
        verify(aiGatewayResponseCache, never()).get(anyString());
    }

    @Test
    void testChatCompletionErrorLoggingResilienceStillThrowsOriginalException() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API call failed"));
        doThrow(new RuntimeException("DB error")).when(aiGatewayRequestLogService)
            .create(any(), any());

        RuntimeException exception = assertThrows(
            RuntimeException.class, () -> aiGatewayFacade.chatCompletion(request));

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
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class)))
            .thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        assertNotNull(response);

        verify(aiGatewayRequestLogService).create(any(), any());
    }

    @Test
    void testChatCompletionStreamSuccess() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse streamChunk = mockStreamChunk("Hello ");
        ChatResponse streamChunkFinal = mockStreamChunk("World");

        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk, streamChunkFinal));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));

        Flux<AiGatewayChatCompletionResponse> responseFlux =
            aiGatewayFacade.chatCompletionStream(request);

        StepVerifier.create(responseFlux)
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void testChatCompletionStreamBudgetCheckBeforeStreaming() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(
            BudgetCheckResult.rejected(new BigDecimal("110"), new BigDecimal("100"), BigDecimal.valueOf(110)));

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request))
            .expectError(BudgetExceededException.class)
            .verify();

        verify(aiGatewayChatModelFactory, never()).getChatModel(any());
    }

    @Test
    void testChatCompletionStreamModelResolutionErrorCreatesLog() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request))
            .expectError(IllegalArgumentException.class)
            .verify();

        verify(aiGatewayRequestLogService).create(any(), any());
    }

    @Test
    void testChatCompletionStreamWithRoutingPolicyStreamsFromRoutedDeployment() {
        Map<String, String> tags = Map.of("workspace_id", "1");

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, true, "my-routing-policy", null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "my-routing-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("my-routing-policy"))
            .thenReturn(routingPolicy);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
        when(aiGatewayProviderService.getProvider(provider.getId())).thenReturn(provider);
        when(aiGatewayRequestLogService.getAverageLatencyByModel(any(Instant.class)))
            .thenReturn(Map.of());
        when(aiGatewayRouter.route(any(), any(), any())).thenReturn(deployment);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse streamChunk = mockStreamChunk("Hello ");
        ChatResponse streamChunkFinal = mockStreamChunk("World");

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk, streamChunkFinal));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));

        when(aiGatewayRetryHandler.<ChatResponse>executeStreamWithRetry(any(), any()))
            .thenAnswer(invocation -> {
                List<AiGatewayModelDeployment> deployments = invocation.getArgument(0);
                Function<AiGatewayModelDeployment, Flux<ChatResponse>> action = invocation.getArgument(1);

                return action.apply(deployments.get(0));
            });

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request))
            .expectNextCount(2)
            .verifyComplete();

        verify(aiGatewayRetryHandler).executeStreamWithRetry(any(), any());
        verify(aiGatewayRouter).route(any(), any(), any());
    }

    @Test
    void testEmbeddingSuccess() {
        AiGatewayEmbeddingRequest request = createEmbeddingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);

        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        when(aiGatewayEmbeddingModelFactory.getEmbeddingModel(any())).thenReturn(embeddingModel);

        EmbeddingResponse embeddingResponse = mockEmbeddingResponse();

        when(embeddingModel.call(any(org.springframework.ai.embedding.EmbeddingRequest.class)))
            .thenReturn(embeddingResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.001"));

        AiGatewayEmbeddingResponse response = aiGatewayFacade.embedding(request);

        assertNotNull(response);
        assertEquals("list", response.object());
        assertEquals("openai/gpt-4", response.model());
        assertFalse(response.data()
            .isEmpty());

        verify(aiGatewayRequestLogService).create(any(), any());
    }

    @Test
    void testEmbeddingBudgetCheckBeforeExecution() {
        AiGatewayEmbeddingRequest request = createEmbeddingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(
            BudgetCheckResult.rejected(new BigDecimal("110"), new BigDecimal("100"), BigDecimal.valueOf(110)));

        assertThrows(BudgetExceededException.class, () -> aiGatewayFacade.embedding(request));

        verify(aiGatewayEmbeddingModelFactory, never()).getEmbeddingModel(any());
    }

    @Test
    void testEmbeddingErrorCreatesErrorLog() {
        AiGatewayEmbeddingRequest request = createEmbeddingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);

        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        when(aiGatewayEmbeddingModelFactory.getEmbeddingModel(any())).thenReturn(embeddingModel);
        when(embeddingModel.call(any(org.springframework.ai.embedding.EmbeddingRequest.class)))
            .thenThrow(new RuntimeException("Embedding API failed"));

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.embedding(request));

        verify(aiGatewayRequestLogService).create(any(), any());
    }

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
        AiModel model = createModel(provider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        assertNotNull(response);
        assertEquals("Routed response", response.choices()
            .get(0)
            .message()
            .content());

        verify(aiGatewayRouter).route(any(), any(), any());
        verify(aiGatewayRetryHandler).executeWithRetry(any(), any());
    }

    @Test
    void testChatCompletionWithRoutingFallsBackToMostCapableTierWhenScorerThrows() {
        Map<String, String> tags = Map.of("workspace_id", "1");

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "my-routing-policy", null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "my-routing-policy", AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("my-routing-policy"))
            .thenReturn(routingPolicy);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
        when(aiGatewayProviderService.getProvider(provider.getId())).thenReturn(provider);
        when(aiGatewayRequestLogService.getAverageLatencyByModel(any(Instant.class)))
            .thenReturn(Map.of());

        when(promptComplexityScorer.score(any())).thenThrow(new RuntimeException("scorer boom"));

        when(aiGatewayRouter.route(any(), any(), any())).thenReturn(deployment);
        when(aiGatewayRetryHandler.executeWithRetry(any(), any())).thenAnswer(invocation -> {
            AiGatewayChatCompletionResponse mockResponse = new AiGatewayChatCompletionResponse(
                "test-id", "chat.completion", 1000L, "openai/gpt-4",
                List.of(new AiGatewayChatCompletionResponse.Choice(
                    0, new AiGatewayChatMessage("assistant", "Routed response"), "stop")),
                null);

            return mockResponse;
        });

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        assertNotNull(response);

        ArgumentCaptor<AiGatewayRoutingContext> contextCaptor = ArgumentCaptor.forClass(AiGatewayRoutingContext.class);

        verify(aiGatewayRouter).route(any(), any(), contextCaptor.capture());

        assertEquals(1.0, contextCaptor.getValue()
            .promptComplexityScore(), 0.0001);
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
            IllegalStateException.class, () -> aiGatewayFacade.chatCompletion(request));

        assertTrue(exception.getMessage()
            .contains("No deployments configured"));
    }

    @Test
    void testChatCompletionStreamResolvesTheModelDefaultRoutingPolicy() {
        AiGatewayChatCompletionRequest request = createStreamingRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        model.setDefaultRoutingPolicyId(99L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse streamChunk = mockStreamChunk("Hello ");

        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(99L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request))
            .expectNextCount(1)
            .verifyComplete();

        verify(aiGatewayRoutingPolicyService).getRoutingPolicy(99L);
    }

    @Test
    void testChatCompletionWithRoutingPolicyIsUnaffectedForAutomationTraffic() {
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
        AiModel model = createModel(provider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request);

        assertNotNull(response);
        assertEquals("Routed response", response.choices()
            .get(0)
            .message()
            .content());
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

    private AiModel createModel(AiGatewayProvider provider) {
        AiModel model = new AiModel(provider.getId(), "gpt-4");

        model.setContextWindow(128000);

        ReflectionTestUtils.setField(model, "id", 1L);

        return model;
    }

    private ChatResponse mockChatResponse() {
        return mockChatResponse("Hello");
    }

    private ChatResponse mockChatResponse(String text) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = new AssistantMessage(text);

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
