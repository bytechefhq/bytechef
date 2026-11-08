/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker;
import com.bytechef.ee.automation.ai.gateway.budget.AiGatewayBudgetChecker.BudgetCheckResult;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptService;
import com.bytechef.ee.platform.ai.gateway.cache.AiGatewayResponseCache;
import com.bytechef.ee.platform.ai.gateway.compression.AiGatewayContextCompressor;
import com.bytechef.ee.platform.ai.gateway.cost.AiGatewayCostCalculator;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayConnectedUserSettings;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderScopeViolationException;
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
import com.bytechef.ee.platform.ai.gateway.dto.AiPromptHeaders;
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
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityTracingHeaders;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySessionService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionType;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.authentication.CredentialType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.security.access.AccessDeniedException;
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
    private AiEvalExecutor aiEvalExecutor;

    @Mock
    private AiGatewayBudgetChecker aiGatewayBudgetChecker;

    @Mock
    private AiGatewayChatModelFactory aiGatewayChatModelFactory;

    @Mock
    private AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService;

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
    private AiGatewaySpendService aiGatewaySpendService;

    @Mock
    private PromptComplexityScorer promptComplexityScorer;

    @Mock
    private WorkspaceAiPromptService workspaceAiPromptService;

    @Mock
    private AiPromptVersionService aiPromptVersionService;

    @Mock
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Mock
    private WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @Mock
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Mock
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Mock
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private PermissionService permissionService;

    @Mock
    private AiGatewayEmbeddedSettingsService embeddedSettingsService;

    @Mock
    private ObjectProvider<AiGatewayEmbeddedSettingsService> embeddedSettingsServiceProvider;

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
            .when(embeddedSettingsServiceProvider.getIfAvailable())
            .thenReturn(embeddedSettingsService);

        org.mockito.Mockito.lenient()
            .when(aiGatewayProviderResolver.resolveTenantProvider(any()))
            .thenAnswer(invocation -> {
                AiGatewayProviderType type = invocation.getArgument(0);

                return aiGatewayProviderService.getEnabledProviders()
                    .stream()
                    .filter(provider -> provider.getConnectedUserId() == null)
                    .filter(provider -> provider.getType() == type)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No enabled provider found for type: " + type));
            });
        org.mockito.Mockito.lenient()
            .when(aiGatewayProviderResolver.resolve(anyLong(), any()))
            .thenAnswer(invocation -> {
                long connectedUserId = invocation.getArgument(0);
                AiGatewayProviderType type = invocation.getArgument(1);

                return aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(connectedUserId, type)
                    .filter(AiGatewayProvider::isEnabled)
                    .orElseGet(() -> aiGatewayProviderResolver.resolveTenantProvider(type));
            });

        aiGatewayFacade = buildFacade(guardrails(false));
    }

    private AiGatewayFacadeImpl buildFacade(
        com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails guardrails) {

        return new AiGatewayFacadeImpl(
            aiEvalExecutor, aiGatewayBudgetChecker, aiGatewayRateLimitChecker,
            aiGatewayChatModelFactory, aiGatewayConnectedUserSettingsService, aiGatewayContextCompressor,
            aiGatewayCostCalculator,
            guardrails,
            aiGatewayEmbeddingModelFactory, aiGatewayModelDeploymentService,
            aiModelService, workspaceAiGatewayProjectService, aiGatewayProviderService,
            aiGatewayProviderResolver,
            aiGatewayRequestLogService, aiGatewayResponseCache, aiGatewayRetryHandler,
            aiGatewayRouter, aiGatewayRoutingPolicyService, aiGatewaySpendService, promptComplexityScorer,
            workspaceAiPromptService, aiPromptVersionService, aiObservabilitySessionService,
            workspaceAiObservabilitySessionService, workspaceAiObservabilityTraceService,
            aiObservabilitySpanService, aiObservabilityTraceService,
            mock(com.bytechef.platform.tag.service.TagService.class),
            mock(com.bytechef.ee.automation.ai.gateway.service.AiGatewayWorkspaceSettingsService.class),
            mock(com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService.class),
            emptyMetricsProvider(),
            embeddedSettingsServiceProvider,
            new org.springframework.context.support.GenericApplicationContext(),
            permissionService,
            transactionManager);
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

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

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

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertEquals("Sure, reaching out to bob@acme.io shortly.", response.choices()
            .get(0)
            .message()
            .content());
    }

    @Test
    void testChatCompletionTracesSpanWithScannedNotRestoredOutput() {
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
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 300L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

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

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "span-test-trace-1", null, "span-test-span", null, "user-1", Map.of(), List.of());

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request, tracingHeaders);

        assertEquals("Sure, reaching out to bob@acme.io shortly.", response.choices()
            .get(0)
            .message()
            .content());

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(aiObservabilitySpanService).create(spanCaptor.capture());

        String spanOutput = spanCaptor.getValue()
            .getOutput();

        assertFalse(spanOutput.contains("bob@acme.io"),
            "span output must not carry the restored real PII value, got: " + spanOutput);
        assertTrue(PiiToken.pattern()
            .matcher(spanOutput)
            .find(), "expected the span output to still carry this exchange's own PII token, got: " + spanOutput);
    }

    @Test
    void testChatCompletionClosesTokenSessionOnDownstreamError() {
        com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails mockGuardrails =
            mock(com.bytechef.ee.automation.ai.gateway.guardrail.AiGatewayGuardrails.class);
        PiiTokenSession session = PiiTokenSession.create();

        when(mockGuardrails.newTokenSession()).thenReturn(session);
        when(mockGuardrails.apply(any(), any(), any(), any(), eq(session))).thenAnswer(invocation -> {
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

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> facade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

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

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        verify(aiGatewayRequestLogService).create(any(), any());
    }

    @Test
    void testChatCompletionReturnsCachedResponseOnCacheHit() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(true);
        when(aiGatewayResponseCache.computeCacheKey(any(), any())).thenReturn("test-cache-key");

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
        when(aiGatewayResponseCache.computeCacheKey(any(), any())).thenReturn("test-cache-key");
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

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
            .when(aiGatewayResponseCache.computeCacheKey(any(), any()))
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

        AiGatewayChatCompletionResponse response = facade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

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

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null))
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

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null))
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

        AiGatewayEmbeddingResponse response = aiGatewayFacade.embedding(request, null);

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

        assertThrows(BudgetExceededException.class, () -> aiGatewayFacade.embedding(request, null));

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

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.embedding(request, null));

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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

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
            IllegalStateException.class, () -> aiGatewayFacade.chatCompletion(request,
                new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of())));

        assertTrue(exception.getMessage()
            .contains("No deployments configured"));
    }

    @Test
    void testChatCompletionWithTracingHeadersCreatesTraceAndSpan() {
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
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 100L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "test-trace-1", null, "test-span", null, "user-1", Map.of(), List.of());

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, tracingHeaders);

        assertNotNull(response);

        verify(workspaceAiObservabilityTraceService).createInWorkspace(any(AiObservabilityTrace.class), anyLong());
        verify(aiObservabilitySpanService).create(any(AiObservabilitySpan.class));
        verify(aiEvalExecutor).evaluateTrace(100L, 1L);
    }

    @Test
    void testChatCompletionFailureDoesNotDispatchOnlineEval() {
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
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 101L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "test-trace-2", null, "test-span", null, "user-1", Map.of(), List.of());

        assertThrows(RuntimeException.class, () -> aiGatewayFacade.chatCompletion(request, tracingHeaders));

        verify(aiObservabilitySpanService).create(any(AiObservabilitySpan.class));
        verify(aiEvalExecutor, never()).evaluateTrace(anyLong(), any());
    }

    @Test
    void testChatCompletionWithPromptHeadersPrependsResolvedPromptAndStampsSpan() {
        Map<String, String> tags = Map.of("workspace_id", "1", "customer", "Ada");

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4",
            List.of(
                new AiGatewayChatMessage("system", "ignored"), new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, tags);

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiPrompt prompt = new AiPrompt("greeting");

        ReflectionTestUtils.setField(prompt, "id", 7L);

        AiPromptVersion promptVersion = new AiPromptVersion(
            7L, 1, AiPromptVersionType.TEXT, "Greet {{customer}} politely", "tester");

        ReflectionTestUtils.setField(promptVersion, "id", 70L);

        when(workspaceAiPromptService.getPromptByName(eq(1L), any(), eq("greeting")))
            .thenReturn(Optional.of(prompt));
        when(aiPromptVersionService.getActiveVersion(7L, "staging")).thenReturn(Optional.of(promptVersion));

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 300L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "prompt-trace-1", null, "prompt-span", null, null, Map.of(), List.of());

        aiGatewayFacade.chatCompletion(request, tracingHeaders, new AiPromptHeaders("greeting", "staging"));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);

        verify(chatModel).call(promptCaptor.capture());

        List<Message> sentMessages = promptCaptor.getValue()
            .getInstructions();

        assertThat(sentMessages).hasSize(2);
        assertThat(sentMessages.getFirst()
            .getText()).isEqualTo("Greet Ada politely");

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(aiObservabilitySpanService).create(spanCaptor.capture());

        AiObservabilitySpan span = spanCaptor.getValue();

        assertEquals(7L, span.getPromptId());
        assertEquals(70L, span.getPromptVersionId());
    }

    @Test
    void testChatCompletionWithUnknownPromptNameFailsBeforeCallingModel() {
        AiGatewayChatCompletionRequest request = createDefaultRequest();

        when(aiGatewayBudgetChecker.checkBudget(1L)).thenReturn(BudgetCheckResult.allowed());
        when(workspaceAiPromptService.getPromptByName(eq(1L), any(), eq("missing")))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalArgumentException.class,
            () -> aiGatewayFacade.chatCompletion(request, null, new AiPromptHeaders("missing", null)));

        verify(aiGatewayChatModelFactory, never()).getChatModel(any());
    }

    @Test
    void testChatCompletionPersistsExperimentSourceWhenTracingHeadersOverrideIt() {
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
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 200L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "experiment-trace-1", null, "experiment-replay", null, null, Map.of(), List.of(),
            AiObservabilityTraceSource.EXPERIMENT);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, tracingHeaders);

        assertNotNull(response);

        ArgumentCaptor<AiObservabilityTrace> traceCaptor = ArgumentCaptor.forClass(AiObservabilityTrace.class);

        verify(workspaceAiObservabilityTraceService).createInWorkspace(traceCaptor.capture(), anyLong());

        AiObservabilityTrace persistedTrace = traceCaptor.getValue();

        assertEquals(AiObservabilityTraceSource.EXPERIMENT, persistedTrace.getSource());
    }

    @Test
    void testChatCompletionDefaultsToApiSourceWhenTracingHeadersOmitIt() {
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
        when(workspaceAiObservabilityTraceService.findByExternalTraceId(any(), anyString()))
            .thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 201L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "api-trace-1", null, "test-span", null, "user-1", Map.of(), List.of());

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, tracingHeaders);

        assertNotNull(response);

        ArgumentCaptor<AiObservabilityTrace> traceCaptor = ArgumentCaptor.forClass(AiObservabilityTrace.class);

        verify(workspaceAiObservabilityTraceService).createInWorkspace(traceCaptor.capture(), anyLong());

        AiObservabilityTrace persistedTrace = traceCaptor.getValue();

        assertEquals(AiObservabilityTraceSource.API, persistedTrace.getSource());
    }

    @Test
    void testChatCompletionWithSameTraceIdUpdatesExistingTrace() {
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

        AiObservabilityTrace existingTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        ReflectionTestUtils.setField(existingTrace, "id", 42L);

        existingTrace.setExternalTraceId("test-trace-1");
        existingTrace.setTotalInputTokens(10);
        existingTrace.setTotalOutputTokens(5);
        existingTrace.setTotalLatencyMs(100);
        existingTrace.setTotalCost(BigDecimal.ONE);

        when(workspaceAiObservabilityTraceService.findByExternalTraceId(1L, "test-trace-1"))
            .thenReturn(Optional.of(existingTrace));

        AiObservabilityTracingHeaders tracingHeaders = new AiObservabilityTracingHeaders(
            "test-trace-1", null, "test-span", null, "user-1", Map.of(), List.of());

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, tracingHeaders);

        assertNotNull(response);

        verify(workspaceAiObservabilityTraceService, never()).createInWorkspace(any(AiObservabilityTrace.class),
            anyLong());
        verify(aiObservabilityTraceService).update(any(AiObservabilityTrace.class));
        verify(aiObservabilitySpanService).create(any(AiObservabilitySpan.class));
    }

    @Test
    void testEmbeddedDefaultPolicyIsResolvedForAKnownConnectedUser() {
        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 42L, null, null)));

        assertThat(((AiGatewayFacadeImpl) aiGatewayFacade).resolveEmbeddedDefaultRoutingPolicyId(
            5L, ENVIRONMENT_ID))
                .isEqualTo(42L);
    }

    @Test
    void testNoEmbeddedSettingsYieldsNoEmbeddedDefault() {
        when(embeddedSettingsService.find(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        assertThat(((AiGatewayFacadeImpl) aiGatewayFacade).resolveEmbeddedDefaultRoutingPolicyId(
            5L, ENVIRONMENT_ID))
                .isNull();
    }

    @Test
    void testNullConnectedUserIdSkipsEmbeddedResolutionEntirely() {
        assertThat(((AiGatewayFacadeImpl) aiGatewayFacade).resolveEmbeddedDefaultRoutingPolicyId(
            null, ENVIRONMENT_ID))
                .isNull();

        verifyNoInteractions(embeddedSettingsService);
    }

    @Test
    void testChatCompletionThreadsConnectedUserIdIntoEmbeddedRoutingPrecedence() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

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

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 42L, null, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(42L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null, 5L);

        assertNotNull(response);

        verify(embeddedSettingsService).find(ENVIRONMENT_ID);
    }

    @Test
    void testChatCompletionStreamThreadsConnectedUserIdIntoEmbeddedRoutingPrecedence() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserStreamingRequest();

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse streamChunk = mockStreamChunk("Hello ");

        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 42L, null, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(42L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null, null, 5L, null))
            .expectNextCount(1)
            .verifyComplete();

        verify(embeddedSettingsService).find(ENVIRONMENT_ID);
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

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null, null, null, null))
            .expectNextCount(1)
            .verifyComplete();

        verify(aiGatewayRoutingPolicyService).getRoutingPolicy(99L);
    }

    @Test
    void testChatCompletionStreamPrefersTheModelDefaultOverTheEmbeddedDefault() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserStreamingRequest();

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

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null, null, 5L, null))
            .expectNextCount(1)
            .verifyComplete();

        verify(aiGatewayRoutingPolicyService).getRoutingPolicy(99L);
        verify(embeddedSettingsService).find(ENVIRONMENT_ID);
    }

    @Test
    void testModelDefaultBelongingToAWorkspaceFallsBackToDirectRoutingForEmbeddedTraffic() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        model.setDefaultRoutingPolicyId(99L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayRoutingPolicy otherCustomersPolicy = new AiGatewayRoutingPolicy(
            "other-customers-policy", AiGatewayRoutingStrategyType.SIMPLE);

        otherCustomersPolicy.setWorkspaceId(7L);

        ReflectionTestUtils.setField(otherCustomersPolicy, "id", 99L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicy(99L)).thenReturn(otherCustomersPolicy);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null, null, 5L);

        assertThat(response).isNotNull();

        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicyByName(any());
        verify(aiGatewayRouter, never()).route(any(), any(), any());
    }

    @Test
    void testConnectedUserPolicyWinsOverTheModelDefaultAndTheEmbeddedDefault() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        model.setDefaultRoutingPolicyId(99L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayRoutingPolicy connectedUserPolicy = new AiGatewayRoutingPolicy(
            "connected-user-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(connectedUserPolicy, "id", 7L);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 7L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(7L))
            .thenReturn(connectedUserPolicy)
            .thenThrow(new IllegalArgumentException("policy not found"));

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null, null, 5L);

        assertNotNull(response);

        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicy(99L);
        verify(embeddedSettingsService).find(anyLong());
    }

    @Test
    void testDisabledConnectedUserPolicyFallsThroughToTheEmbeddedDefault() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

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

        AiGatewayRoutingPolicy connectedUserPolicy = new AiGatewayRoutingPolicy(
            "connected-user-policy", AiGatewayRoutingStrategyType.SIMPLE);

        connectedUserPolicy.setEnabled(false);

        ReflectionTestUtils.setField(connectedUserPolicy, "id", 7L);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 7L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(7L)).thenReturn(connectedUserPolicy);

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 42L, null, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(42L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null, null, 5L);

        assertNotNull(response);

        verify(aiGatewayRoutingPolicyService).getRoutingPolicy(42L);
    }

    @Test
    void testNullConnectedUserIdSkipsConnectedUserPolicyResolutionEntirely() {
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);

        verifyNoInteractions(aiGatewayConnectedUserSettingsService);
        verifyNoInteractions(embeddedSettingsService);
    }

    @Test
    void testChatCompletionAttributesSpendToResolvedConnectedUser() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

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

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 42L, null, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(42L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null, 5L);

        assertNotNull(response);

        ArgumentCaptor<AiLlmUsage> requestLogCaptor = ArgumentCaptor.forClass(AiLlmUsage.class);

        verify(aiGatewayRequestLogService).createForConnectedUser(requestLogCaptor.capture(), anyLong());

        assertEquals(5L, requestLogCaptor.getValue()
            .getUserId());

    }

    @Test
    void testChatCompletionAttributesSpendWhenRequestCarriesExplicitRoutingPolicy() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "my-routing-policy", null, null, null, null);

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "my-routing-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("my-routing-policy"))
            .thenReturn(routingPolicy);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 10L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(10L)).thenReturn(routingPolicy);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
        when(aiGatewayProviderService.getProvider(provider.getId())).thenReturn(provider);
        when(aiGatewayRouter.route(any(), any(), any())).thenReturn(deployment);

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        when(aiGatewayRetryHandler.executeWithRetry(any(), any())).thenAnswer(invocation -> {
            List<AiGatewayModelDeployment> deployments = invocation.getArgument(0);
            Function<AiGatewayModelDeployment, AiGatewayChatCompletionResponse> action = invocation.getArgument(1);

            return action.apply(deployments.get(0));
        });

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null, 5L);

        assertNotNull(response);

        ArgumentCaptor<AiLlmUsage> requestLogCaptor = ArgumentCaptor.forClass(AiLlmUsage.class);

        verify(aiGatewayRequestLogService).createForConnectedUser(requestLogCaptor.capture(), anyLong());

        assertEquals(5L, requestLogCaptor.getValue()
            .getUserId());

        verify(embeddedSettingsService).find(ENVIRONMENT_ID);
    }

    @Test
    void testChatCompletionUsesConnectedUsersOwnProviderOverTheTenantProviderOnTheRoutedPath() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "my-routing-policy", null, null, null, null);

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "my-routing-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("my-routing-policy"))
            .thenReturn(routingPolicy);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 10L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(10L)).thenReturn(routingPolicy);

        AiGatewayProvider tenantProvider = createProvider();
        AiModel model = createModel(tenantProvider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        AiGatewayProvider connectedUserProvider = new AiGatewayProvider(
            "Connected user's OpenAI", AiGatewayProviderType.OPENAI, "byok-api-key");

        connectedUserProvider.setConnectedUserId(5L);
        ReflectionTestUtils.setField(connectedUserProvider, "id", 2L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
        when(aiGatewayProviderService.getProvider(tenantProvider.getId())).thenReturn(tenantProvider);
        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(5L, AiGatewayProviderType.OPENAI))
            .thenReturn(Optional.of(connectedUserProvider));
        when(aiGatewayRouter.route(any(), any(), any())).thenReturn(deployment);

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        when(aiGatewayRetryHandler.executeWithRetry(any(), any())).thenAnswer(invocation -> {
            List<AiGatewayModelDeployment> deployments = invocation.getArgument(0);
            Function<AiGatewayModelDeployment, AiGatewayChatCompletionResponse> action = invocation.getArgument(1);

            return action.apply(deployments.get(0));
        });

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null, 5L);

        assertNotNull(response);

        verify(aiGatewayChatModelFactory)
            .getChatModel(argThat(provider -> provider.getId()
                .equals(connectedUserProvider.getId())));

        ArgumentCaptor<AiLlmUsage> requestLogCaptor = ArgumentCaptor.forClass(AiLlmUsage.class);

        verify(aiGatewayRequestLogService).createForConnectedUser(requestLogCaptor.capture(), anyLong());

        assertEquals(
            AiGatewayProviderType.OPENAI.name(), requestLogCaptor.getValue()
                .getRoutedProvider());
    }

    @Test
    void testChatCompletionRejectsATenantProviderScopedToADifferentConnectedUser() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "my-routing-policy", null, null, null, null);

        AiGatewayRoutingPolicy routingPolicy = new AiGatewayRoutingPolicy(
            "my-routing-policy", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(routingPolicy, "id", 10L);

        when(aiGatewayRoutingPolicyService.getRoutingPolicyByName("my-routing-policy"))
            .thenReturn(routingPolicy);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 10L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(10L)).thenReturn(routingPolicy);

        AiGatewayProvider otherConnectedUsersProvider = new AiGatewayProvider(
            "Connected user 99's OpenAI", AiGatewayProviderType.OPENAI, "other-customer-api-key");

        otherConnectedUsersProvider.setConnectedUserId(99L);
        ReflectionTestUtils.setField(otherConnectedUsersProvider, "id", 3L);

        AiModel model = createModel(otherConnectedUsersProvider);
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(10L, 1L);

        ReflectionTestUtils.setField(deployment, "id", 100L);

        when(aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(10L))
            .thenReturn(List.of(deployment));
        when(aiModelService.getModel(1L)).thenReturn(model);
        when(aiGatewayProviderService.getProvider(otherConnectedUsersProvider.getId()))
            .thenReturn(otherConnectedUsersProvider);
        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(5L, AiGatewayProviderType.OPENAI))
            .thenReturn(Optional.empty());
        when(aiGatewayRouter.route(any(), any(), any())).thenReturn(deployment);

        when(aiGatewayRetryHandler.executeWithRetry(any(), any())).thenAnswer(invocation -> {
            List<AiGatewayModelDeployment> deployments = invocation.getArgument(0);
            Function<AiGatewayModelDeployment, AiGatewayChatCompletionResponse> action = invocation.getArgument(1);

            return action.apply(deployments.get(0));
        });

        assertThrows(AiGatewayProviderScopeViolationException.class,
            () -> aiGatewayFacade.chatCompletion(
                request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null,
                5L));

        verifyNoInteractions(aiGatewayChatModelFactory);
    }

    @Test
    void testEmbeddedRequestNamingAPlanItIsNotAssignedIsRejected() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, "premium-plan", null, null, null, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> aiGatewayFacade.chatCompletion(request, null, null, 5L));

        assertEquals("Routing policy not found: premium-plan", exception.getMessage());

        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicyByName(any());
        verifyNoInteractions(aiGatewayModelDeploymentService, aiGatewayChatModelFactory);
    }

    @Test
    void testChatCompletionStreamRequestNamingAPlanItIsNotAssignedIsRejected() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, true, "premium-plan", null, null, null, null);

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null, null, 5L, null))
            .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
                && "Routing policy not found: premium-plan".equals(throwable.getMessage()))
            .verify();

        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicyByName(any());
        verifyNoInteractions(aiGatewayModelDeploymentService, aiGatewayChatModelFactory);
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);
        assertEquals("Routed response", response.choices()
            .get(0)
            .message()
            .content());
    }

    @Test
    void testUnknownPlanAndAnUnassignedPlanProduceIdenticalErrors() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayRoutingPolicy assignedPlan =
            new AiGatewayRoutingPolicy("assigned-plan", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(assignedPlan, "id", 12L);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 12L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(12L)).thenReturn(assignedPlan);

        IllegalArgumentException missingPlanException = assertThrows(IllegalArgumentException.class,
            () -> aiGatewayFacade.chatCompletion(requestNamingPlan("missing-plan"), null, null, 5L));
        IllegalArgumentException unassignedPlanException = assertThrows(IllegalArgumentException.class,
            () -> aiGatewayFacade.chatCompletion(requestNamingPlan("other-plan"), null, null, 5L));

        assertEquals("Routing policy not found: missing-plan", missingPlanException.getMessage());
        assertEquals("Routing policy not found: other-plan", unassignedPlanException.getMessage());
    }

    @Test
    void testEmbeddedRequestNamingItsAssignedPlanIsRejectedWhenThatPlanIsWorkspaceScoped() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayRoutingPolicy assignedPlan =
            new AiGatewayRoutingPolicy("assigned-plan", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(assignedPlan, "id", 12L);
        assignedPlan.setWorkspaceId(1L);

        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, 12L, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(12L)).thenReturn(assignedPlan);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> aiGatewayFacade.chatCompletion(requestNamingPlan("assigned-plan"), null, null, 5L));

        assertEquals("Routing policy not found: assigned-plan", exception.getMessage());

        verifyNoInteractions(aiGatewayModelDeploymentService, aiGatewayChatModelFactory);
    }

    private static AiGatewayChatCompletionRequest requestNamingPlan(String planName) {
        return new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, planName, null, null, null, null);
    }

    @Test
    void testChatCompletionUsesConnectedUsersOwnProviderOverTheTenantProviderOnTheDirectPath() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider tenantProvider = createProvider();
        AiModel model = createModel(tenantProvider);

        AiGatewayProvider connectedUserProvider = new AiGatewayProvider(
            "Connected user's OpenAI", AiGatewayProviderType.OPENAI, "byok-api-key");

        connectedUserProvider.setConnectedUserId(5L);
        ReflectionTestUtils.setField(connectedUserProvider, "id", 2L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(tenantProvider));
        when(aiModelService.getModel(tenantProvider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(5L, AiGatewayProviderType.OPENAI))
            .thenReturn(Optional.of(connectedUserProvider));

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null, 5L);

        assertNotNull(response);

        verify(aiGatewayChatModelFactory)
            .getChatModel(argThat(provider -> provider.getId()
                .equals(connectedUserProvider.getId())));
    }

    @Test
    void testChatCompletionAttributesSpendWhenModelHasItsOwnDefaultRoutingPolicy() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        model.setDefaultRoutingPolicyId(99L);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(99L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse chatResponse = mockChatResponse();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(BigDecimal.ZERO);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(
            request, new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()), null, 5L);

        assertNotNull(response);

        ArgumentCaptor<AiLlmUsage> requestLogCaptor = ArgumentCaptor.forClass(AiLlmUsage.class);

        verify(aiGatewayRequestLogService).createForConnectedUser(requestLogCaptor.capture(), anyLong());

        assertEquals(5L, requestLogCaptor.getValue()
            .getUserId());

        verify(embeddedSettingsService).find(ENVIRONMENT_ID);
    }

    @Test
    void testChatCompletionWithoutConnectedUserIdLeavesSpendAttributionNull() {
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

        doAnswer(invocation -> {
            AiObservabilityTrace trace = invocation.getArgument(0);

            ReflectionTestUtils.setField(trace, "id", 100L);

            return null;
        }).when(workspaceAiObservabilityTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);

        ArgumentCaptor<AiLlmUsage> requestLogCaptor = ArgumentCaptor.forClass(AiLlmUsage.class);

        verify(aiGatewayRequestLogService).create(requestLogCaptor.capture(), any());

        assertNull(requestLogCaptor.getValue()
            .getUserId());

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(aiObservabilitySpanService).create(spanCaptor.capture());

        assertNull(spanCaptor.getValue()
            .getMetadata());

        verifyNoInteractions(embeddedSettingsService);
    }

    @Test
    void testChatCompletionStreamAttributesSpendToResolvedConnectedUser() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserStreamingRequest();

        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse streamChunk = mockStreamChunk("Hello ");

        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, 42L, null, null)));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(42L))
            .thenThrow(new IllegalArgumentException("policy not found"));

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null, null, 7L, null))
            .expectNextCount(1)
            .verifyComplete();

        ArgumentCaptor<AiLlmUsage> requestLogCaptor = ArgumentCaptor.forClass(AiLlmUsage.class);

        verify(aiGatewayRequestLogService).createForConnectedUser(requestLogCaptor.capture(), anyLong());

        assertEquals(7L, requestLogCaptor.getValue()
            .getUserId());
    }

    @Test
    void testConnectedUserOverBudgetCapIsRejectedBeforeRoutingOrTheLlmCall() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(
                    ENVIRONMENT_ID, null, null, null, null, null, null, null, new BigDecimal("50.00"))));
        when(aiGatewaySpendService.getTotalCostByConnectedUserId(eq(5L), any(), any()))
            .thenReturn(Money.usd(new BigDecimal("100.00")));

        assertThrows(BudgetExceededException.class,
            () -> aiGatewayFacade.chatCompletion(request, null, null, 5L));

        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicy(anyLong());
        verify(aiModelService, never()).getModel(anyLong(), anyString());
        verifyNoInteractions(aiGatewayChatModelFactory);
    }

    @Test
    void testConnectedUserUnderBudgetCapProceedsNormally() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);
        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(
                    ENVIRONMENT_ID, null, null, null, null, null, null, null, new BigDecimal("50.00"))));
        when(aiGatewaySpendService.getTotalCostByConnectedUserId(eq(5L), any(), any()))
            .thenReturn(Money.usd(new BigDecimal("10.00")));

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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null, null, 5L);

        assertNotNull(response);
    }

    @Test
    void testNoConnectedUserBudgetCapConfiguredNeverQueriesSpend() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(aiGatewayResponseCache.shouldCache(any())).thenReturn(false);
        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(ENVIRONMENT_ID, null, null, null, null, null, null, null, null)));

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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null, null, 5L);

        assertNotNull(response);

        verifyNoInteractions(aiGatewaySpendService);
    }

    @Test
    void testConnectedUserBudgetCapNeverAppliesToAutomationTraffic() {
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

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request,
            new AiObservabilityTracingHeaders(null, null, null, null, null, Map.of(), List.of()));

        assertNotNull(response);

        verifyNoInteractions(embeddedSettingsService);
        verifyNoInteractions(aiGatewaySpendService);
    }

    @Test
    void testConnectedUsersOwnCapOverridesTheEmbeddedDefaultCap() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(
                    ENVIRONMENT_ID, null, null, null, null, null, null, null, new BigDecimal("50.00"))));
        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, null, new BigDecimal("5.00"))));
        when(aiGatewaySpendService.getTotalCostByConnectedUserId(eq(5L), any(), any()))
            .thenReturn(Money.usd(new BigDecimal("10.00")));

        assertThrows(BudgetExceededException.class,
            () -> aiGatewayFacade.chatCompletion(createConnectedUserRequest(), null, null, 5L));

        verifyNoInteractions(aiGatewayChatModelFactory);
    }

    @Test
    void testEmbeddedDefaultCapAppliesWhenTheConnectedUserSetsNoCap() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        when(embeddedSettingsService.find(ENVIRONMENT_ID))
            .thenReturn(Optional.of(
                new AiGatewayEmbeddedSettings(
                    ENVIRONMENT_ID, null, null, null, null, null, null, null, new BigDecimal("50.00"))));
        when(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(5L))
            .thenReturn(Optional.of(connectedUserSettings(5L, null, null)));
        when(aiGatewaySpendService.getTotalCostByConnectedUserId(eq(5L), any(), any()))
            .thenReturn(Money.usd(new BigDecimal("100.00")));

        assertThrows(BudgetExceededException.class,
            () -> aiGatewayFacade.chatCompletion(request, null, null, 5L));

        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicy(anyLong());
        verifyNoInteractions(aiGatewayChatModelFactory);
    }

    @Test
    void testConnectedUserChatCompletionWithoutWorkspaceTagSkipsTheWorkspaceChecks() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        stubDirectChatCompletion(request);

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null, null, 5L);

        assertNotNull(response);

        verifyNoInteractions(aiGatewayBudgetChecker, aiGatewayRateLimitChecker, permissionService);
    }

    @Test
    void testConnectedUserChatCompletionStreamWithoutWorkspaceTagSkipsTheWorkspaceChecks() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserStreamingRequest();

        stubDirectChatCompletionStream(request);

        StepVerifier.create(aiGatewayFacade.chatCompletionStream(request, null, null, 5L, null))
            .expectNextCount(1)
            .verifyComplete();

        verifyNoInteractions(aiGatewayBudgetChecker, aiGatewayRateLimitChecker, permissionService);
    }

    @Test
    void testConnectedUserChatCompletionCarryingAWorkspaceTagIsRejected() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createDefaultRequest();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request, null, null, 5L));

        assertEquals(
            "Embedded requests must not carry a 'workspace_id' or 'project_id' tag", exception.getMessage());

        verifyNoInteractions(aiGatewayBudgetChecker, aiGatewayChatModelFactory);
    }

    @Test
    void testConnectedUserChatCompletionCarryingAProjectTagIsRejected() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, Map.of("project_id", "3"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> aiGatewayFacade.chatCompletion(request, null, null, 5L));

        assertEquals(
            "Embedded requests must not carry a 'workspace_id' or 'project_id' tag", exception.getMessage());

        verifyNoInteractions(aiGatewayChatModelFactory);
    }

    @Test
    void testConnectedUserEmbeddedSettingsAreReadForThePrincipalsOwnEnvironment() {
        authenticateApiKeyPrincipal(ENVIRONMENT_ID);

        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        stubDirectChatCompletion(request);

        aiGatewayFacade.chatCompletion(request, null, null, 5L);

        verify(embeddedSettingsService).find(ENVIRONMENT_ID);
        verify(embeddedSettingsService, never()).find(0L);
    }

    @Test
    void testConnectedUserWithoutAnAuthenticatedEnvironmentIsRejected() {
        AiGatewayChatCompletionRequest request = createConnectedUserRequest();

        assertThrows(AccessDeniedException.class, () -> aiGatewayFacade.chatCompletion(request, null, null, 5L));

        verifyNoInteractions(embeddedSettingsService, aiGatewayChatModelFactory);
    }

    private static void authenticateApiKeyPrincipal(long environmentId) {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestApiKeyAuthenticationToken(environmentId));
    }

    private static AiGatewayConnectedUserSettings connectedUserSettings(
        long connectedUserId, Long routingPolicyId, BigDecimal budgetCap) {

        AiGatewayConnectedUserSettings settings = new AiGatewayConnectedUserSettings(connectedUserId);

        settings.setRoutingPolicyId(routingPolicyId);
        settings.setBudgetCap(budgetCap);

        return settings;
    }

    private AiGatewayChatCompletionRequest createConnectedUserRequest() {
        return new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, false, null, null, null, null, null);
    }

    private AiGatewayChatCompletionRequest createConnectedUserStreamingRequest() {
        return new AiGatewayChatCompletionRequest(
            "openai/gpt-4", List.of(new AiGatewayChatMessage("user", "Hello")),
            null, null, null, true, null, null, null, null, null);
    }

    private void stubDirectChatCompletion(AiGatewayChatCompletionRequest request) {
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
    }

    private void stubDirectChatCompletionStream(AiGatewayChatCompletionRequest request) {
        AiGatewayProvider provider = createProvider();
        AiModel model = createModel(provider);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(provider));
        when(aiModelService.getModel(provider.getId(), "gpt-4")).thenReturn(model);
        when(aiGatewayContextCompressor.compress(any(), any(Integer.class))).thenReturn(request.messages());

        ChatModel chatModel = mock(ChatModel.class);

        when(aiGatewayChatModelFactory.getChatModel(any())).thenReturn(chatModel);

        ChatResponse streamChunk = mockStreamChunk("Hello ");

        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(streamChunk));
        when(aiGatewayCostCalculator.calculateCost(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new BigDecimal("0.01"));
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

    @SuppressWarnings("serial")
    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId) {
            super(environmentId, "test-tenant");
        }

        @Override
        public CredentialType getCredentialType() {
            return CredentialType.EMBEDDED_API_KEY;
        }

        @Override
        public PlatformType getPlatformType() {
            return PlatformType.EMBEDDED;
        }
    }
}
