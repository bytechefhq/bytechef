/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayConnectedUserSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserAiGatewayFacadeTest {

    private static final long CONNECTED_USER_ID = 5L;
    private static final long MISSING_CONNECTED_USER_ID = 777L;
    private static final long POLICY_ID = 10L;

    @Mock
    private AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Mock
    private AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @Mock
    private ConnectedUserService connectedUserService;

    private ConnectedUserAiGatewayFacadeImpl connectedUserAiGatewayFacade;

    @BeforeEach
    void setUp() {
        connectedUserAiGatewayFacade = new ConnectedUserAiGatewayFacadeImpl(
            aiGatewayConnectedUserSettingsService, aiGatewayProviderService, aiGatewayRoutingPolicyService,
            connectedUserService);
    }

    @Test
    void testAssignRoutingPolicyAssignsATenantLevelPolicy() {
        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(POLICY_ID)).thenReturn(policy(POLICY_ID));

        connectedUserAiGatewayFacade.assignRoutingPolicy(CONNECTED_USER_ID, POLICY_ID);

        verify(aiGatewayConnectedUserSettingsService).assignRoutingPolicy(CONNECTED_USER_ID, POLICY_ID);
    }

    @Test
    void testAssignRoutingPolicyRefusesAWorkspaceScopedPolicy() {
        AiGatewayRoutingPolicy workspacePolicy = policy(POLICY_ID);

        workspacePolicy.setWorkspaceId(42L);

        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(POLICY_ID)).thenReturn(workspacePolicy);

        assertThatThrownBy(() -> connectedUserAiGatewayFacade.assignRoutingPolicy(CONNECTED_USER_ID, POLICY_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Routing policy 10 belongs to a workspace and cannot be assigned to a connected user");

        verify(aiGatewayConnectedUserSettingsService, never()).assignRoutingPolicy(anyLong(), anyLong());
    }

    @Test
    void testEveryOperationRefusesAnUnknownConnectedUserIdenticallyAndTouchesNothing() {
        when(connectedUserService.fetchConnectedUser(MISSING_CONNECTED_USER_ID)).thenReturn(Optional.empty());

        List<ThrowingCallable> operations = List.of(
            () -> connectedUserAiGatewayFacade.assignRoutingPolicy(MISSING_CONNECTED_USER_ID, POLICY_ID),
            () -> connectedUserAiGatewayFacade.unassignRoutingPolicy(MISSING_CONNECTED_USER_ID),
            () -> connectedUserAiGatewayFacade.updateBudgetCap(MISSING_CONNECTED_USER_ID, BigDecimal.ONE),
            () -> connectedUserAiGatewayFacade.createProvider(
                MISSING_CONNECTED_USER_ID, new AiGatewayProvider("customer", AiGatewayProviderType.OPENAI, "sk")),
            () -> connectedUserAiGatewayFacade.deleteProvider(MISSING_CONNECTED_USER_ID, 3L));

        for (ThrowingCallable operation : operations) {
            assertThatThrownBy(operation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Connected user not found: " + MISSING_CONNECTED_USER_ID);
        }

        verifyNoInteractions(
            aiGatewayConnectedUserSettingsService, aiGatewayProviderService, aiGatewayRoutingPolicyService);
    }

    @Test
    void testUpdateBudgetCapDelegatesForAKnownConnectedUser() {
        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));

        connectedUserAiGatewayFacade.updateBudgetCap(CONNECTED_USER_ID, new BigDecimal("25.00"));

        verify(aiGatewayConnectedUserSettingsService).updateBudgetCap(CONNECTED_USER_ID, new BigDecimal("25.00"));
    }

    @Test
    void testCreateProviderCreatesAProviderOwnedByTheConnectedUser() {
        AiGatewayProvider provider = new AiGatewayProvider("customer", AiGatewayProviderType.OPENAI, "sk");

        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));
        when(aiGatewayProviderService.createConnectedUserProvider(provider, CONNECTED_USER_ID)).thenReturn(provider);

        assertThat(connectedUserAiGatewayFacade.createProvider(CONNECTED_USER_ID, provider)).isSameAs(provider);
    }

    @Test
    void testDeleteProviderRefusesAProviderTheConnectedUserDoesNotOwn() {
        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));
        when(aiGatewayProviderService.getProvidersByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(List.of(providerWithId(3L)));

        assertThatThrownBy(() -> connectedUserAiGatewayFacade.deleteProvider(CONNECTED_USER_ID, 4L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Provider not found: 4");

        verify(aiGatewayProviderService, never()).delete(anyLong());
    }

    @Test
    void testDeleteProviderDeletesTheConnectedUsersOwnProvider() {
        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));
        when(aiGatewayProviderService.getProvidersByConnectedUserId(CONNECTED_USER_ID))
            .thenReturn(List.of(providerWithId(3L)));

        connectedUserAiGatewayFacade.deleteProvider(CONNECTED_USER_ID, 3L);

        verify(aiGatewayProviderService).delete(3L);
    }

    @Test
    void testUnassignRoutingPolicyDelegatesForAKnownConnectedUser() {
        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(new ConnectedUser()));

        connectedUserAiGatewayFacade.unassignRoutingPolicy(CONNECTED_USER_ID);

        verify(aiGatewayConnectedUserSettingsService).unassignRoutingPolicy(CONNECTED_USER_ID);
    }

    private static AiGatewayRoutingPolicy policy(long id) {
        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy("plan-" + id, AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(policy, "id", id);

        return policy;
    }

    private static AiGatewayProvider providerWithId(long id) {
        AiGatewayProvider provider = new AiGatewayProvider("customer", AiGatewayProviderType.OPENAI, "sk");

        ReflectionTestUtils.setField(provider, "id", id);

        return provider;
    }
}
