/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.agent.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins {@link AiAgentOwnershipResolver}, whose whole job is to answer which workspace an agent id belongs to so the
 * {@code 'AiAgent'} gates on {@code AiAgentFacadeImpl} can be evaluated against it.
 *
 * <p>
 * {@link #testWorkspacelessAgentIsUnknown()} is the one that matters. {@code ai_agent.workspace_id} is nullable in the
 * schema, so an agent with no workspace is a reachable row shape rather than a hypothetical one — and a resolver that
 * substituted any default for it would hand every member of that default workspace a foreign agent while every gate
 * test stayed green. {@link #testWorkspacelessAgentIsDeniedThroughTheEvaluator()} carries the same case through the
 * real {@code PermissionServiceImpl} so the fail-closed claim is not just a property of this class in isolation.
 *
 * @author Ivica Cardic
 */
class AiAgentOwnershipResolverTest {

    private final AiAgentRepository aiAgentRepository = mock(AiAgentRepository.class);
    private final AiAgentOwnershipResolver resolver = new AiAgentOwnershipResolver(aiAgentRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("AiAgent");
    }

    @Test
    void testResolvesOwningWorkspace() {
        when(aiAgentRepository.findById(1L)).thenReturn(Optional.of(agent(42L)));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    /**
     * An agent in someone else's workspace resolves to <em>that</em> workspace, never to the caller's — which is what
     * makes the scope check downstream a real boundary rather than a formality.
     */
    @Test
    void testForeignAgentResolvesToItsOwnWorkspace() {
        when(aiAgentRepository.findById(1L)).thenReturn(Optional.of(agent(99L)));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId())
                .hasValue(99L)
                .isNotEqualTo(java.util.OptionalLong.of(42L));
    }

    @Test
    void testWorkspacelessAgentIsUnknown() {
        when(aiAgentRepository.findById(1L)).thenReturn(Optional.of(agent(null)));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner(1L)
            .ownerUserId()).isEmpty();
    }

    @Test
    void testUnknownAgentIsUnknown() {
        when(aiAgentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }

    /**
     * The fail-closed claim carried through the real CE {@code PermissionServiceImpl} rather than asserted about this
     * resolver alone: an authenticated non-admin gets {@code false} for a workspace-less agent. The authenticated
     * context is what makes this discriminating — without it the call would return {@code false} from the
     * {@code isAuthenticated()} guard and prove nothing about the resolver.
     */
    @Test
    void testWorkspacelessAgentIsDeniedThroughTheEvaluator() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("user", null, List.of()));

        PermissionServiceImpl permissionService = new PermissionServiceImpl(
            mock(UserService.class), List.of(resolver), List.of(), mock(ResourceVisibilityResolver.class));

        when(aiAgentRepository.findById(1L)).thenReturn(Optional.of(agent(42L)));

        assertThat(permissionService.hasResourceScope(1L, "AiAgent", "AGENT_EDIT"))
            .as("an agent that does have a workspace passes, so the denial below is about the null and nothing else")
            .isTrue();

        when(aiAgentRepository.findById(1L)).thenReturn(Optional.of(agent(null)));

        assertThat(permissionService.hasResourceScope(1L, "AiAgent", "AGENT_EDIT")).isFalse();
    }

    private static AiAgent agent(Long workspaceId) {
        AiAgent aiAgent = new AiAgent();

        aiAgent.setWorkspaceId(workspaceId);

        return aiAgent;
    }
}
