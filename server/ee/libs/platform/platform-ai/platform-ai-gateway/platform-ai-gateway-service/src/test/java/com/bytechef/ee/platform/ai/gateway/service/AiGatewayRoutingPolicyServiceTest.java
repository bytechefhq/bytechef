/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.gateway.repository.AiGatewayRoutingPolicyRepository;
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
class AiGatewayRoutingPolicyServiceTest {

    @Mock
    private AiGatewayModelDeploymentService aiGatewayModelDeploymentService;

    @Mock
    private AiGatewayRoutingPolicyRepository aiGatewayRoutingPolicyRepository;

    private AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @BeforeEach
    void setUp() {
        aiGatewayRoutingPolicyService = new AiGatewayRoutingPolicyServiceImpl(
            aiGatewayModelDeploymentService, aiGatewayRoutingPolicyRepository);
    }

    @Test
    void testGetDefaultRoutingPoliciesReturnsNullScopedPolicies() {
        AiGatewayRoutingPolicy defaultPolicy =
            new AiGatewayRoutingPolicy("default", AiGatewayRoutingStrategyType.SIMPLE);

        when(aiGatewayRoutingPolicyRepository.findAllByWorkspaceIdIsNull()).thenReturn(List.of(defaultPolicy));

        List<AiGatewayRoutingPolicy> policies = aiGatewayRoutingPolicyService.getDefaultRoutingPolicies();

        assertThat(policies).containsExactly(defaultPolicy);
    }

    @Test
    void testGetRoutingPoliciesByWorkspaceIdDoesNotReturnNullScopedPolicies() {
        when(aiGatewayRoutingPolicyRepository.findAllByWorkspaceId(1L)).thenReturn(List.of());

        assertThat(aiGatewayRoutingPolicyService.getRoutingPoliciesByWorkspaceId(1L)).isEmpty();

        verify(aiGatewayRoutingPolicyRepository, never()).findAllByWorkspaceIdIsNull();
    }

    @Test
    void testDeleteRemovesThePolicyAndItsDeployments() {

        aiGatewayRoutingPolicyService.delete(10L);

        verify(aiGatewayModelDeploymentService).deleteByRoutingPolicyId(10L);
        verify(aiGatewayRoutingPolicyRepository).deleteById(10L);
    }

    @Test
    void testUpdateWorkspaceIdAssignsThePolicyToAWorkspace() {
        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy("default", AiGatewayRoutingStrategyType.SIMPLE);

        when(aiGatewayRoutingPolicyRepository.findById(10L)).thenReturn(Optional.of(policy));

        aiGatewayRoutingPolicyService.updateWorkspaceId(10L, 1L);

        assertThat(policy.getWorkspaceId()).isEqualTo(1L);

        verify(aiGatewayRoutingPolicyRepository).save(policy);
    }

    @Test
    void testUpdateWorkspaceIdUnscopesThePolicy() {
        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy("default", AiGatewayRoutingStrategyType.SIMPLE);

        when(aiGatewayRoutingPolicyRepository.findById(10L)).thenReturn(Optional.of(policy));

        aiGatewayRoutingPolicyService.updateWorkspaceId(10L, null);

        assertThat(policy.getWorkspaceId()).isNull();

        verify(aiGatewayRoutingPolicyRepository).save(policy);
    }
}
