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
import java.util.Optional;
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

    private static final long CONNECTED_USER_ID = 5L;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    private AiGatewayProviderResolver aiGatewayProviderResolver;

    @BeforeEach
    void setUp() {
        aiGatewayProviderResolver = new AiGatewayProviderResolverImpl(aiGatewayProviderService);
    }

    @Test
    void testConnectedUserProviderWinsOverTheTenantProvider() {
        AiGatewayProvider customerProvider = newProvider(1L, OPENAI, CONNECTED_USER_ID, true);

        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(CONNECTED_USER_ID, OPENAI))
            .thenReturn(Optional.of(customerProvider));

        assertThat(aiGatewayProviderResolver.resolve(CONNECTED_USER_ID, OPENAI)).isEqualTo(customerProvider);
    }

    @Test
    void testFallsBackToTheTenantProviderWhenTheCustomerHasNone() {
        AiGatewayProvider tenantProvider = newProvider(2L, OPENAI, null, true);

        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(CONNECTED_USER_ID, OPENAI))
            .thenReturn(Optional.empty());
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(tenantProvider));

        assertThat(aiGatewayProviderResolver.resolve(CONNECTED_USER_ID, OPENAI)).isEqualTo(tenantProvider);
    }

    @Test
    void testFallsBackToTheTenantProviderWhenTheCustomerProviderIsDisabled() {
        AiGatewayProvider disabledCustomerProvider = newProvider(3L, OPENAI, CONNECTED_USER_ID, false);
        AiGatewayProvider tenantProvider = newProvider(4L, OPENAI, null, true);

        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(CONNECTED_USER_ID, OPENAI))
            .thenReturn(Optional.of(disabledCustomerProvider));
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(tenantProvider));

        assertThat(aiGatewayProviderResolver.resolve(CONNECTED_USER_ID, OPENAI))
            .as("a disabled connected-user provider must fall through to the tenant's provider rather than fail "
                + "the request — an operator turning off one BYOK credential must not break the customer's traffic")
            .isEqualTo(tenantProvider);
    }

    @Test
    void testTenantFallbackNeverPicksAnotherConnectedUsersProvider() {
        long otherConnectedUserId = 99L;
        AiGatewayProvider otherCustomerProvider = newProvider(5L, OPENAI, otherConnectedUserId, true);
        AiGatewayProvider tenantProvider = newProvider(6L, OPENAI, null, true);

        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(CONNECTED_USER_ID, OPENAI))
            .thenReturn(Optional.empty());
        when(aiGatewayProviderService.getEnabledProviders())
            .thenReturn(List.of(otherCustomerProvider, tenantProvider));

        assertThat(aiGatewayProviderResolver.resolve(CONNECTED_USER_ID, OPENAI))
            .as("the tenant fallback must never serve a DIFFERENT connected user's own BYOK credential — only a "
                + "provider bound to no connected user is a legitimate shared fallback")
            .isEqualTo(tenantProvider);
    }

    @Test
    void testThrowsWhenNeitherTheCustomerNorTheTenantHasAnEnabledProviderOfTheType() {
        when(aiGatewayProviderService.fetchProviderByConnectedUserIdAndType(CONNECTED_USER_ID, ANTHROPIC))
            .thenReturn(Optional.empty());
        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of());

        assertThatThrownBy(() -> aiGatewayProviderResolver.resolve(CONNECTED_USER_ID, ANTHROPIC))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testResolveTenantProviderReturnsTheEnabledNonConnectedUserProviderOfTheType() {
        AiGatewayProvider tenantProvider = newProvider(7L, OPENAI, null, true);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(tenantProvider));

        assertThat(aiGatewayProviderResolver.resolveTenantProvider(OPENAI)).isEqualTo(tenantProvider);
    }

    @Test
    void testResolveTenantProviderExcludesConnectedUserScopedProvidersAndThrowsWhenNoneRemain() {
        AiGatewayProvider connectedUserScopedProvider = newProvider(8L, OPENAI, CONNECTED_USER_ID, true);

        when(aiGatewayProviderService.getEnabledProviders()).thenReturn(List.of(connectedUserScopedProvider));

        assertThatThrownBy(() -> aiGatewayProviderResolver.resolveTenantProvider(OPENAI))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static AiGatewayProvider newProvider(
        long id, AiGatewayProviderType type, Long connectedUserId, boolean enabled) {

        AiGatewayProvider provider = new AiGatewayProvider("provider-" + id, type, "sk-test-" + id);

        provider.setConnectedUserId(connectedUserId);
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
