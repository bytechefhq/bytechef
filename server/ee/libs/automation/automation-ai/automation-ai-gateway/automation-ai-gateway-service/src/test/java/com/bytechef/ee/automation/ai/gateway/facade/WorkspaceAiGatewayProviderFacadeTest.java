/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.dto.ProviderConnectionResult;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link WorkspaceAiGatewayProviderFacadeImpl}. Focus on the workspace-ownership guard and the
 * connection-probe failure paths. The happy-path connection probe is deliberately out of scope here — it crosses the
 * network and belongs in an int-test.
 *
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceAiGatewayProviderFacadeTest {

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    private WorkspaceAiGatewayProviderFacade workspaceAiGatewayProviderFacade;

    @BeforeEach
    void setUp() {
        workspaceAiGatewayProviderFacade = new WorkspaceAiGatewayProviderFacadeImpl(
            aiGatewayProviderService, workspaceAiGatewayProviderService);
    }

    @Test
    void testVerifyWorkspaceOwnershipRejectsProviderInDifferentWorkspace() {
        // Workspace 1 owns provider 5; we attempt to test connection for provider 999 under workspace 1.
        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(new WorkspaceAiGatewayProvider(5L, 1L)));

        assertThatThrownBy(() -> workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to workspace");
    }

    @Test
    void testTestWorkspaceProviderConnectionReturnsFailureWhenBaseUrlMissing() {
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        // baseUrl intentionally left null.

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(new WorkspaceAiGatewayProvider(5L, 1L)));
        when(aiGatewayProviderService.getProvider(5L)).thenReturn(provider);

        ProviderConnectionResult result = workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 5L);

        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).contains("no baseUrl configured");
    }

    @Test
    void testTestWorkspaceProviderConnectionRejectsPrivateBaseUrlViaSsrfGuard() {
        // 127.0.0.1 must be rejected by AiGatewayUrlValidator — this test covers that the facade wires the SSRF
        // check before issuing any outbound request. Without this guard, a workspace admin could point baseUrl at a
        // private host and exfiltrate the Authorization header.
        AiGatewayProvider provider = new AiGatewayProvider("openai", AiGatewayProviderType.OPENAI, "sk-123");

        provider.setBaseUrl("http://127.0.0.1");

        when(workspaceAiGatewayProviderService.getWorkspaceProviders(1L))
            .thenReturn(List.of(new WorkspaceAiGatewayProvider(5L, 1L)));
        when(aiGatewayProviderService.getProvider(5L)).thenReturn(provider);

        ProviderConnectionResult result = workspaceAiGatewayProviderFacade.testWorkspaceProviderConnection(1L, 5L);

        assertThat(result.ok()).isFalse();
        assertThat(result.errorMessage()).contains("baseUrl rejected");
    }
}
