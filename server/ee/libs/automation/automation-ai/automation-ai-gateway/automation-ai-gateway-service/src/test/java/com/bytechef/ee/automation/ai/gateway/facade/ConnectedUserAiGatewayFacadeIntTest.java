/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.gateway.facade.ConnectedUserAiGatewayFacade;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayConnectedUserSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
class ConnectedUserAiGatewayFacadeIntTest {

    @Autowired
    private AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService;

    @Autowired
    private AiGatewayProviderService aiGatewayProviderService;

    @Autowired
    private AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @Autowired
    private ConnectedUserAiGatewayFacade connectedUserAiGatewayFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Test
    void testTwoConnectedUsersShareOnePlanAndReassigningOneLeavesTheOtherAlone() {
        long environmentId = Environment.STAGING.ordinal();

        AiGatewayRoutingPolicy firstPlan = aiGatewayRoutingPolicyService.create(
            new AiGatewayRoutingPolicy("facade-it-first-plan", AiGatewayRoutingStrategyType.SIMPLE));
        AiGatewayRoutingPolicy secondPlan = aiGatewayRoutingPolicyService.create(
            new AiGatewayRoutingPolicy("facade-it-second-plan", AiGatewayRoutingStrategyType.SIMPLE));

        ConnectedUser firstCustomer = connectedUserService.createConnectedUser("facade-it-first", environmentId);
        ConnectedUser secondCustomer = connectedUserService.createConnectedUser("facade-it-second", environmentId);

        connectedUserAiGatewayFacade.assignRoutingPolicy(firstCustomer.getId(), firstPlan.getId());
        connectedUserAiGatewayFacade.assignRoutingPolicy(secondCustomer.getId(), firstPlan.getId());

        connectedUserAiGatewayFacade.assignRoutingPolicy(firstCustomer.getId(), secondPlan.getId());

        assertThat(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(firstCustomer.getId()))
            .hasValueSatisfying(settings -> assertThat(settings.getRoutingPolicyId()).isEqualTo(secondPlan.getId()));
        assertThat(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(secondCustomer.getId()))
            .as("Reassigning one customer must not move another customer on the same plan")
            .hasValueSatisfying(settings -> assertThat(settings.getRoutingPolicyId()).isEqualTo(firstPlan.getId()));
    }

    @Test
    void testAssigningAWorkspaceScopedPolicyIsRefusedAndWritesNothing() {
        AiGatewayRoutingPolicy workspacePolicy = aiGatewayRoutingPolicyService.create(
            new AiGatewayRoutingPolicy("facade-it-workspace-policy", AiGatewayRoutingStrategyType.SIMPLE));

        aiGatewayRoutingPolicyService.updateWorkspaceId(workspacePolicy.getId(), 909_778L);

        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "facade-it-workspace-user", Environment.STAGING.ordinal());

        assertThatThrownBy(
            () -> connectedUserAiGatewayFacade.assignRoutingPolicy(connectedUser.getId(), workspacePolicy.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("belongs to a workspace");

        assertThat(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(connectedUser.getId())).isEmpty();
    }

    @Test
    void testASecondProviderOfTheSameTypeIsRefusedWithADomainError() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "facade-it-provider-user", Environment.STAGING.ordinal());

        connectedUserAiGatewayFacade.createProvider(
            connectedUser.getId(),
            new AiGatewayProvider("facade-it-first-groq", AiGatewayProviderType.GROQ, "sk-test-" + UUID.randomUUID()));

        assertThatThrownBy(
            () -> connectedUserAiGatewayFacade.createProvider(
                connectedUser.getId(),
                new AiGatewayProvider(
                    "facade-it-second-groq", AiGatewayProviderType.GROQ, "sk-test-" + UUID.randomUUID())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("already has a provider of type GROQ");
    }

    @Test
    void testDeletingAConnectedUserDeletesItsSettingsAndDisablesButKeepsItsProviders() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "delete-settings-user", Environment.PRODUCTION.ordinal());

        AiGatewayRoutingPolicy plan = aiGatewayRoutingPolicyService.create(
            new AiGatewayRoutingPolicy("delete-settings-plan", AiGatewayRoutingStrategyType.SIMPLE));

        connectedUserAiGatewayFacade.assignRoutingPolicy(connectedUser.getId(), plan.getId());

        AiGatewayProvider provider = connectedUserAiGatewayFacade.createProvider(
            connectedUser.getId(),
            new AiGatewayProvider("delete-settings-groq", AiGatewayProviderType.GROQ, "sk-test-" + UUID.randomUUID()));

        connectedUserService.deleteConnectedUser(connectedUser.getId());

        assertThat(aiGatewayConnectedUserSettingsService.fetchByConnectedUserId(connectedUser.getId())).isEmpty();

        AiGatewayProvider reloadedProvider = aiGatewayProviderService.getProvider(provider.getId());

        assertThat(reloadedProvider.isEnabled()).isFalse();
        assertThat(reloadedProvider.getConnectedUserId()).isEqualTo(connectedUser.getId());
        assertThat(aiGatewayRoutingPolicyService.getRoutingPolicy(plan.getId())).isNotNull();
    }
}
