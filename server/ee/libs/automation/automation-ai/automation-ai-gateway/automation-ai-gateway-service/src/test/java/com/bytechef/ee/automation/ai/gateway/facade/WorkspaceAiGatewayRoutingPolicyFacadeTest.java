/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayRoutingPolicyService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link WorkspaceAiGatewayRoutingPolicyFacadeImpl}. Focus on the workspace-ownership guard — the rest
 * of the facade is a thin CRUD forwarder.
 *
 * @version ee
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkspaceAiGatewayRoutingPolicyFacadeTest {

    @Mock
    private AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @Mock
    private WorkspaceAiGatewayRoutingPolicyService workspaceAiGatewayRoutingPolicyService;

    private WorkspaceAiGatewayRoutingPolicyFacadeImpl workspaceAiGatewayRoutingPolicyFacade;

    @BeforeEach
    void setUp() {
        workspaceAiGatewayRoutingPolicyFacade = new WorkspaceAiGatewayRoutingPolicyFacadeImpl(
            aiGatewayRoutingPolicyService, workspaceAiGatewayRoutingPolicyService);
    }

    @Test
    void testDeleteWorkspaceRoutingPolicyRejectsPolicyInDifferentWorkspace() {
        // Workspace 1 owns policy 5 only; attempt to delete policy 999 must fail before delete() is invoked.
        when(workspaceAiGatewayRoutingPolicyService.getWorkspaceRoutingPolicies(1L))
            .thenReturn(List.of(workspaceRoutingPolicy()));

        assertThatThrownBy(() -> workspaceAiGatewayRoutingPolicyFacade.deleteWorkspaceRoutingPolicy(1L, 999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to workspace");

        verify(aiGatewayRoutingPolicyService, never()).delete(999L);
        verify(workspaceAiGatewayRoutingPolicyService, never()).removeRoutingPolicyFromWorkspace(999L);
    }

    @Test
    void testUpdateWorkspaceRoutingPolicyDelegatesOnlyAfterOwnershipCheckPasses() {
        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy("p", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(policy, "id", 5L);

        when(workspaceAiGatewayRoutingPolicyService.getWorkspaceRoutingPolicies(1L)).thenReturn(List.of(policy));
        when(aiGatewayRoutingPolicyService.getRoutingPolicy(5L)).thenReturn(policy);
        when(aiGatewayRoutingPolicyService.update(policy)).thenReturn(policy);

        // Config must match the sealed AiGatewayRoutingStrategyConfig hierarchy — needs a "strategyType" discriminator.
        AiGatewayRoutingPolicy updated = workspaceAiGatewayRoutingPolicyFacade.updateWorkspaceRoutingPolicy(
            1L, 5L, "new-name", AiGatewayRoutingStrategyType.COST_OPTIMIZED, "fallback-model",
            "{\"strategyType\":\"COST_OPTIMIZED\",\"maxCostPerRequest\":null}", true);

        assertThat(updated).isSameAs(policy);
        assertThat(policy.getName()).isEqualTo("new-name");
        assertThat(policy.getStrategy()).isEqualTo(AiGatewayRoutingStrategyType.COST_OPTIMIZED);
        assertThat(policy.isEnabled()).isTrue();

        verify(aiGatewayRoutingPolicyService).update(policy);
    }

    @Test
    void testGetWorkspaceRoutingPoliciesReturnsEmptyListWithoutTouchingPolicyService() {
        when(workspaceAiGatewayRoutingPolicyService.getWorkspaceRoutingPolicies(1L))
            .thenReturn(List.of());

        List<AiGatewayRoutingPolicy> policies =
            workspaceAiGatewayRoutingPolicyFacade.getWorkspaceRoutingPolicies(1L);

        assertThat(policies).isEmpty();

        // The workspace service now returns the policies themselves (they carry their own workspace_id), so the
        // facade must pass them straight through — never re-fetch them by id, which for an empty workspace would
        // fire an empty-id SELECT against the underlying repository.
        verify(aiGatewayRoutingPolicyService, never()).getRoutingPolicies(any());
    }

    /**
     * A routing policy owned by workspace 1, id 5. Ownership is now carried by the policy's own {@code workspace_id}
     * column, so the workspace service hands back the policies themselves rather than membership rows.
     */
    private static AiGatewayRoutingPolicy workspaceRoutingPolicy() {
        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy("p", AiGatewayRoutingStrategyType.SIMPLE);

        ReflectionTestUtils.setField(policy, "id", 5L);

        policy.setWorkspaceId(1L);

        return policy;
    }
}
