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
 * Unit tests for {@link WorkspaceAiGatewayProviderFacadeImpl}. Focus on the workspace-ownership guard and the
 * connection-probe failure paths. The happy-path connection probe is deliberately out of scope here — it crosses the
 * network and belongs in an int-test.
 *
 * @author Ivica Cardic
 * @version ee
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
        // TransactionTemplate's execute() asks the manager for a status, runs the callback inline, then commits.
        // A SimpleTransactionStatus stub satisfies it without standing up a real DataSource — exactly what we
        // want from a unit test of facade-level orchestration.
        lenient().when(transactionManager.getTransaction(any()))
            .thenReturn(new SimpleTransactionStatus());

        workspaceAiGatewayProviderFacade = new WorkspaceAiGatewayProviderFacadeImpl(
            aiGatewayProviderService, workspaceAiGatewayProviderService, transactionManager);
    }

    @Test
    void testTestConnectionReturnsFailureWhenWorkspaceDoesNotOwnProvider() {
        // A wrong-workspace test-connection request must NOT propagate a raw IAE up to the global exception
        // handler — the IAE message would echo "Provider 999 does not belong to workspace 1" into a 400,
        // leaking cross-workspace provider existence and breaking the documented "always returns a structured
        // ProviderConnectionResult" UI contract. The facade catches the IAE inline and returns a coarse
        // non-disambiguating failure result.
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

        // baseUrl intentionally left null.

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
        // SSRF probes (private-host base urls) must surface in dashboards even though the UI just shows a generic
        // "Failed to reach provider" — operators correlating a backend incident with a flood of failed UI tests
        // need WARN-level entries with workspaceId/providerId. A regression that downgrades to DEBUG silently
        // hides cross-tenant attack patterns. The literal IP is the SUBJECT of the test, not a smell.
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
        // 127.0.0.1 must be rejected by AiObservabilityUrlValidator — this test covers that the facade wires the SSRF
        // check before issuing any outbound request. Without this guard, a workspace admin could point baseUrl at a
        // private host and exfiltrate the Authorization header. The literal IP is the SUBJECT of the test, not a
        // smell — PMD's AvoidUsingHardCodedIP is suppressed accordingly.
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        provider.setBaseUrl("http://127.0.0.1");

        ReflectionTestUtils.setField(provider, "id", 5L);

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L)).thenReturn(List.of(provider));
        when(aiGatewayProviderService.getProvider(5L)).thenReturn(provider);

        ProviderConnectionResult result = workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 5L);

        assertThat(result.ok()).isFalse();
        // The user-facing message must be coarse: revealing the SSRF validator's exact reason (e.g. "host
        // 127.0.0.1 resolves to a loopback address") would let an attacker enumerate the internal blocklist
        // past the SSRF guard. Full detail stays in the warn log server-side.
        assertThat(result.errorMessage()).contains("not externally routable");
        assertThat(result.errorMessage()).doesNotContain("127.0.0.1");
    }

    /**
     * Pins the failure-reason taxonomy. Each branch returns a distinct coarse user-facing string. A regression that
     * swaps two branches' strings, or accidentally lets, e.g., a TLS handshake error fall through into the generic
     * branch (re-leaking certificate-chain detail through the exception's {@code toString()}), would silently corrupt
     * the operator-facing error taxonomy. {@code isEqualTo} (not {@code contains}) locks the exact wire shape.
     */
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
        // Pinned by isEqualTo so a regression letting the SSLException fall through into the generic branch
        // (which would re-leak the certificate-chain detail via toString()) fails this test instead of silently
        // changing the operator-facing taxonomy.
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
