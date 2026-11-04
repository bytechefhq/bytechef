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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.WorkspaceApiKeyIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.configuration.facade.ClusterElementPropertyFacade;
import com.bytechef.platform.configuration.facade.ClusterElementPropertyFacadeImpl;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(ClusterElementPropertyFacadeImpl.class)
@WorkspaceApiKeyIntTestConfigurationSharedMocks
class ConnectionOwnershipResolverIntTest {

    private static final long MCP_COMPONENT_ID = 9103L;
    private static final long UNKNOWN_CONNECTION_ID = 9102L;
    private static final long WORKSPACE_CONNECTION_ID = 9101L;

    @MockitoBean
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ClusterElementPropertyFacade clusterElementPropertyFacade;

    @Autowired
    private ConnectionOwnershipResolver connectionOwnershipResolver;

    @Autowired
    private List<ResourceOwnershipResolver> resourceOwnershipResolvers;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("connection-ownership"));

        workspaceId = workspace.getId();

        workspaceConnectionService.create(WORKSPACE_CONNECTION_ID, workspaceId);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        workspaceConnectionRepository.deleteAll();
        workspaceRepository.deleteById(workspaceId);
    }

    @Test
    void testResourceTypeMatchesTheTokenTheConnectionGuardsName() {
        assertThat(connectionOwnershipResolver.resourceType()).isEqualTo("Connection");
        assertThat(resourceOwnershipResolvers)
            .filteredOn(resourceOwnershipResolver -> "Connection".equals(resourceOwnershipResolver.resourceType()))
            .containsExactly(connectionOwnershipResolver);
    }

    @Test
    void testResolveOwnerReturnsTheWorkspaceOfAWorkspaceConnection() {
        assertThat(connectionOwnershipResolver.resolveOwner(WORKSPACE_CONNECTION_ID))
            .isEqualTo(ResourceOwner.ofWorkspace(workspaceId));
    }

    @Test
    void testResolveOwnerFailsClosedForAConnectionOutsideAnyWorkspace() {
        assertThat(connectionOwnershipResolver.resolveOwner(UNKNOWN_CONNECTION_ID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testGetClusterElementOptionsAllowsAMissingConnection() {
        authenticate(AuthorityConstants.USER);

        getClusterElementOptions(null);

        verifyClusterElementOptionsExecuted(null);
    }

    @Test
    void testGetClusterElementOptionsRejectsAConnectionOutsideAnyWorkspace() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> getClusterElementOptions(UNKNOWN_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetClusterElementOptionsLetsATenantAdminUseAConnectionOutsideAnyWorkspace() {
        authenticate(AuthorityConstants.ADMIN);

        getClusterElementOptions(UNKNOWN_CONNECTION_ID);

        verifyClusterElementOptionsExecuted(UNKNOWN_CONNECTION_ID);
    }

    @Test
    void testGetClusterElementDynamicPropertiesRejectsAConnectionOutsideAnyWorkspace() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> clusterElementPropertyFacade.getClusterElementDynamicProperties(
            "component", 1, "clusterElement", "property", Map.of(), List.of(), MCP_COMPONENT_ID,
            UNKNOWN_CONNECTION_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "user@localhost.com", null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private void getClusterElementOptions(Long connectionId) {
        clusterElementPropertyFacade.getClusterElementOptions(
            "component", 1, "clusterElement", "property", Map.of(), List.of(), MCP_COMPONENT_ID, connectionId);
    }

    private void verifyClusterElementOptionsExecuted(Long connectionId) {
        verify(clusterElementDefinitionFacade).executeOptions(
            "component", 1, "clusterElement", "property", Map.of(), Map.of(), List.of(), null, connectionId,
            Map.of(), Map.of());
    }
}
