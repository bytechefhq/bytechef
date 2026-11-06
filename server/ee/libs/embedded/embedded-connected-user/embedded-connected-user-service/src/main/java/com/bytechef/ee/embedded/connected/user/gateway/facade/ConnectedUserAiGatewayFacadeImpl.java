/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.gateway.facade;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayConnectedUserSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class ConnectedUserAiGatewayFacadeImpl implements ConnectedUserAiGatewayFacade {

    private final AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    ConnectedUserAiGatewayFacadeImpl(
        AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService,
        AiGatewayProviderService aiGatewayProviderService,
        AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService,
        ConnectedUserService connectedUserService) {

        this.aiGatewayConnectedUserSettingsService = aiGatewayConnectedUserSettingsService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
        this.connectedUserService = connectedUserService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void assignRoutingPolicy(long connectedUserId, long routingPolicyId) {
        requireConnectedUserExists(connectedUserId);

        AiGatewayRoutingPolicy policy = aiGatewayRoutingPolicyService.getRoutingPolicy(routingPolicyId);

        if (policy.getWorkspaceId() != null) {
            throw new IllegalArgumentException(
                "Routing policy " + routingPolicyId + " belongs to a workspace and cannot be assigned to a " +
                    "connected user");
        }

        aiGatewayConnectedUserSettingsService.assignRoutingPolicy(connectedUserId, routingPolicyId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider createProvider(long connectedUserId, AiGatewayProvider provider) {
        requireConnectedUserExists(connectedUserId);

        return aiGatewayProviderService.createConnectedUserProvider(provider, connectedUserId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteProvider(long connectedUserId, long providerId) {
        requireConnectedUserExists(connectedUserId);

        List<AiGatewayProvider> connectedUserProviders =
            aiGatewayProviderService.getProvidersByConnectedUserId(connectedUserId);

        boolean ownsProvider = connectedUserProviders.stream()
            .anyMatch(provider -> Objects.equals(provider.getId(), providerId));

        if (!ownsProvider) {
            throw new IllegalArgumentException("Provider not found: " + providerId);
        }

        aiGatewayProviderService.delete(providerId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void unassignRoutingPolicy(long connectedUserId) {
        requireConnectedUserExists(connectedUserId);

        aiGatewayConnectedUserSettingsService.unassignRoutingPolicy(connectedUserId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateBudgetCap(long connectedUserId, @Nullable BigDecimal budgetCap) {
        requireConnectedUserExists(connectedUserId);

        aiGatewayConnectedUserSettingsService.updateBudgetCap(connectedUserId, budgetCap);
    }

    private void requireConnectedUserExists(long connectedUserId) {
        if (connectedUserService.fetchConnectedUser(connectedUserId)
            .isEmpty()) {

            throw new IllegalArgumentException("Connected user not found: " + connectedUserId);
        }
    }
}
