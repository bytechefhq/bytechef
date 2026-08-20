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
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins {@link AiAgentChannelOwnershipResolver}. {@code updateAgentChannel} and {@code deleteAgentChannel} are keyed on
 * a channel id and nothing else, so this walk is the only thing standing between a caller and another workspace's agent
 * configuration.
 *
 * <p>
 * {@link #testChannelOfAForeignAgentResolvesToTheForeignWorkspace()} is the case the gate exists for: the answer must
 * be the workspace the channel's agent actually lives in, never the caller's and never a fallback.
 *
 * @author Ivica Cardic
 */
class AiAgentChannelOwnershipResolverTest {

    private static final long CALLER_WORKSPACE_ID = 42L;
    private static final long FOREIGN_WORKSPACE_ID = 99L;

    private final AiAgentChannelRepository aiAgentChannelRepository = mock(AiAgentChannelRepository.class);
    private final AiAgentRepository aiAgentRepository = mock(AiAgentRepository.class);
    private final AiAgentChannelOwnershipResolver resolver = new AiAgentChannelOwnershipResolver(
        aiAgentChannelRepository, aiAgentRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("AiAgentChannel");
    }

    @Test
    void testResolvesWorkspaceViaAgent() {
        givenChannel(1L, 7L, CALLER_WORKSPACE_ID);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(CALLER_WORKSPACE_ID);
    }

    @Test
    void testChannelOfAForeignAgentResolvesToTheForeignWorkspace() {
        givenChannel(1L, 7L, FOREIGN_WORKSPACE_ID);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId())
                .hasValue(FOREIGN_WORKSPACE_ID)
                .isNotEqualTo(OptionalLong.of(CALLER_WORKSPACE_ID));
    }

    @Test
    void testWorkspacelessAgentIsUnknown() {
        givenChannel(1L, 7L, null);

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testUnknownChannelIsUnknown() {
        when(aiAgentChannelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testDanglingAgentIsUnknown() {
        AiAgentChannel channel = new AiAgentChannel(7L, "webhook");

        when(aiAgentChannelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(aiAgentRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
    }

    /**
     * The denial carried through the real CE {@code PermissionServiceImpl}: an agent with no workspace yields
     * {@code false} for the same call that returns {@code true} when the agent has one, so the {@code false} is
     * attributable to the resolver's fail-closed branch rather than to the authentication guard above it.
     *
     * <p>
     * CE has no workspace membership — every workspace-owned resource is shared there — so CE is the wrong place to
     * assert that a <em>foreign</em> workspace is denied; that half is the resolved-workspace mismatch pinned by
     * {@code PermissionServiceResourceTest} in the EE module, keyed on the workspace id this resolver returns.
     */
    @Test
    void testWorkspacelessAgentIsDeniedThroughTheEvaluator() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("user", null, List.of()));

        PermissionServiceImpl permissionService = new PermissionServiceImpl(
            mock(UserService.class), List.of(resolver), List.of(), mock(ResourceVisibilityResolver.class));

        givenChannel(1L, 7L, CALLER_WORKSPACE_ID);

        assertThat(permissionService.hasResourceScope(1L, "AiAgentChannel", "AGENT_EDIT")).isTrue();

        givenChannel(1L, 7L, null);

        assertThat(permissionService.hasResourceScope(1L, "AiAgentChannel", "AGENT_EDIT")).isFalse();
    }

    private void givenChannel(long channelId, long agentId, Long workspaceId) {
        AiAgentChannel channel = new AiAgentChannel(agentId, "webhook");
        AiAgent agent = new AiAgent();

        agent.setWorkspaceId(workspaceId);

        when(aiAgentChannelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(aiAgentRepository.findById(agentId)).thenReturn(Optional.of(agent));
    }
}
