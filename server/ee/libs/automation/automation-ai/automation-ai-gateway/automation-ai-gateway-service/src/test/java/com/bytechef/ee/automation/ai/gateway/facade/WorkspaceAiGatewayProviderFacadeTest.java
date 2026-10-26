/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.dto.ProviderConnectionResult;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceAiGatewayProviderFacadeTest {

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private WorkspaceAiGatewayProviderFacade workspaceAiGatewayProviderFacade;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any()))
            .thenReturn(new SimpleTransactionStatus());

        workspaceAiGatewayProviderFacade = new WorkspaceAiGatewayProviderFacadeImpl(
            aiGatewayProviderService, workspaceAiGatewayProviderService, transactionManager);
    }

    @Test
    void testTestConnectionReturnsFailureWhenWorkspaceDoesNotOwnProvider() {
        AiGatewayProvider ownedProvider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        ReflectionTestUtils.setField(ownedProvider, "id", 5L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L)).thenReturn(List.of(ownedProvider));

        ProviderConnectionResult result = workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 999L);

        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Provider not found");
    }

    @Test
    void testTestWorkspaceProviderConnectionReturnsFailureWhenBaseUrlMissing() {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        ReflectionTestUtils.setField(provider, "id", 5L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L)).thenReturn(List.of(provider));
        when(aiGatewayProviderService.getProvider(5L)).thenReturn(provider);

        ProviderConnectionResult result = workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 5L);

        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).contains("no baseUrl configured");
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void testSsrfFailureLogsAtWarnLevelForOpsCorrelation() {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        provider.setBaseUrl("http://127.0.0.1");

        ReflectionTestUtils.setField(provider, "id", 5L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L)).thenReturn(List.of(provider));
        when(aiGatewayProviderService.getProvider(5L)).thenReturn(provider);

        ListAppender<ILoggingEvent> appender = attachListAppender(WorkspaceAiGatewayProviderFacadeImpl.class);

        try {
            ProviderConnectionResult result = workspaceAiGatewayProviderFacade
                .testWorkspaceProviderConnection(1L, 5L);

            assertThat(result.ok()).isFalse();

            assertThat(appender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("workspaceId=1");
                    assertThat(event.getFormattedMessage()).contains("providerId=5");
                });
        } finally {
            detachListAppender(WorkspaceAiGatewayProviderFacadeImpl.class, appender);
        }
    }

    private static ListAppender<ILoggingEvent> attachListAppender(Class<?> targetClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(targetClass);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachListAppender(Class<?> targetClass, ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(targetClass);

        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void testTestWorkspaceProviderConnectionRejectsPrivateBaseUrlViaSsrfGuard() {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        provider.setBaseUrl("http://127.0.0.1");

        ReflectionTestUtils.setField(provider, "id", 5L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L)).thenReturn(List.of(provider));
        when(aiGatewayProviderService.getProvider(5L)).thenReturn(provider);

        ProviderConnectionResult result = workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 5L);

        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).contains("not externally routable");
        assertThat(result.errorMessage()).doesNotContain("127.0.0.1");
    }

    @Test
    void testCoarseFailureReasonMapsUnknownHostExceptionToHostString() {
        assertThat(WorkspaceAiGatewayProviderFacadeImpl.coarseFailureReason(
            new java.net.UnknownHostException("provider.example.com"))).isEqualTo("Could not resolve provider host");
    }

    @Test
    void testCoarseFailureReasonMapsConnectExceptionToConnectString() {
        assertThat(WorkspaceAiGatewayProviderFacadeImpl.coarseFailureReason(
            new java.net.ConnectException("Connection refused"))).isEqualTo("Could not connect to provider");
    }

    @Test
    void testCoarseFailureReasonMapsHttpTimeoutToTimeoutString() {
        assertThat(WorkspaceAiGatewayProviderFacadeImpl.coarseFailureReason(
            new java.net.http.HttpTimeoutException("read timeout")))
                .isEqualTo("Provider connection timed out");
    }

    @Test
    void testCoarseFailureReasonMapsSocketTimeoutToSameTimeoutString() {
        assertThat(WorkspaceAiGatewayProviderFacadeImpl.coarseFailureReason(
            new java.net.SocketTimeoutException("socket timeout")))
                .isEqualTo("Provider connection timed out");
    }

    @Test
    void testCoarseFailureReasonMapsSslExceptionToTlsString() {
        assertThat(WorkspaceAiGatewayProviderFacadeImpl.coarseFailureReason(
            new javax.net.ssl.SSLException("certificate path validation failed for CN=provider.example.com")))
                .isEqualTo("TLS handshake with provider failed");
    }

    @Test
    void testCoarseFailureReasonFallsThroughToGenericForUnmappedException() {
        assertThat(WorkspaceAiGatewayProviderFacadeImpl.coarseFailureReason(new RuntimeException("anything else")))
            .isEqualTo("Could not reach provider");
    }
}
