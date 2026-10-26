/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import static com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType.ANTHROPIC;
import static com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType.OPENAI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayProviderResolverTest {

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    private AiGatewayProviderResolver aiGatewayProviderResolver;

    @BeforeEach
    void setUp() {
        aiGatewayProviderResolver = new AiGatewayProviderResolverImpl(aiGatewayProviderService);
    }

    @Test
    void testResolveTenantProviderThrowsWhenNoEnabledProviderOfTheTypeExists() {
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        assertThatThrownBy(() -> aiGatewayProviderResolver.resolveTenantProvider(ANTHROPIC))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testResolveTenantProviderReturnsTheEnabledProviderOfTheType() {
        AiGatewayProvider tenantProvider = newProvider(7L, OPENAI, true);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(tenantProvider));

        assertThat(aiGatewayProviderResolver.resolveTenantProvider(OPENAI)).isEqualTo(tenantProvider);
    }

    private static AiGatewayProvider newProvider(
        long id, AiGatewayProviderType type, boolean enabled) {
        AiGatewayProvider provider = new AiGatewayProvider("provider-" + id, type, "sk-test-" + id);

        provider.setEnabled(enabled);

        setIdViaReflection(provider, id);

        return provider;
    }

    private static void setIdViaReflection(AiGatewayProvider provider, long id) {
        try {
            Field idField = AiGatewayProvider.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(provider, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id on test provider", reflectiveOperationException);
        }
    }
}
