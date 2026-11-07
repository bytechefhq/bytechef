/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicy;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
class McpOutboundRedactorProviderTest {

    private final AiGuardrails aiGuardrails = mock(AiGuardrails.class);
    private final SensitiveDataRedactor sensitiveDataRedactor = mock(SensitiveDataRedactor.class);

    @Test
    void testEmptyWhenTheWorkspaceHasOutboundRedactionOff() {
        when(aiGuardrails.resolveMcpOutboundPolicy(AiGuardrailsSettingsTarget.workspace(1L))).thenReturn(null);

        assertThat(newProvider().fetchRedactor(1L, "mcp_automation")).isEmpty();
    }

    @Test
    void testRedactsWithTheResolvedPolicysKindsAndThreshold() {
        when(aiGuardrails.resolveMcpOutboundPolicy(AiGuardrailsSettingsTarget.workspace(1L)))
            .thenReturn(new SensitiveDataPolicy(Set.of(SensitiveKind.PII), 0.7, true));
        when(
            sensitiveDataRedactor.redact(
                eq("bob@acme.io"), eq(Set.of(SensitiveKind.PII)), eq(0.7), any()))
                    .thenReturn("[REDACTED_EMAIL_ADDRESS]");

        Optional<McpOutboundRedactor> redactor = newProvider().fetchRedactor(1L, "mcp_automation");

        assertThat(redactor).isPresent();
        assertThat(redactor.get()
            .redact("bob@acme.io")).isEqualTo("[REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testPropagatesAPolicyResolutionFailureRatherThanReportingNoRedaction() {
        when(aiGuardrails.resolveMcpOutboundPolicy(AiGuardrailsSettingsTarget.workspace(1L)))
            .thenThrow(new IllegalStateException("connection reset"));

        McpOutboundRedactorProviderImpl provider = newProvider();

        assertThatExceptionOfType(IllegalStateException.class)
            .as("an empty Optional means this workspace redacts nothing, which a failed lookup must not claim")
            .isThrownBy(() -> provider.fetchRedactor(1L, "mcp_automation"));
    }

    @Test
    void testResolvesTheTenantDefaultForANullWorkspace() {
        when(aiGuardrails.resolveMcpOutboundPolicy(AiGuardrailsSettingsTarget.platform()))
            .thenReturn(new SensitiveDataPolicy(Set.of(SensitiveKind.PII), 0.4, true));

        assertThat(newProvider().fetchRedactor(null, "mcp_automation")).isPresent();
    }

    @Test
    void testRoutesTheEmbeddedSurfaceToTheEmbeddedScopeNotTheTenantDefault() {
        when(aiGuardrails.resolveEmbeddedMcpOutboundPolicy())
            .thenReturn(new SensitiveDataPolicy(Set.of(SensitiveKind.PII), 0.4, true));

        assertThat(newProvider().fetchRedactor(null, "mcp_embedded")).isPresent();

        verify(aiGuardrails, never()).resolveMcpOutboundPolicy(any(AiGuardrailsSettingsTarget.class));
    }

    @SuppressWarnings("unchecked")
    private McpOutboundRedactorProviderImpl newProvider() {
        return new McpOutboundRedactorProviderImpl(
            aiGuardrails, sensitiveDataRedactor, mock(ObjectProvider.class));
    }
}
