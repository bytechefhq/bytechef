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

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.WorkspaceApiKeyIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@WorkspaceApiKeyIntTestConfigurationSharedMocks
class WorkspaceOwnershipResolverIntTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private List<ResourceOwnershipResolver> resourceOwnershipResolvers;

    @Autowired
    private WorkspaceOwnershipResolver workspaceOwnershipResolver;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("workspace-ownership"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        workspaceRepository.deleteById(workspaceId);
    }

    @Test
    void testResourceTypeMatchesTheTokenTheWorkspaceGuardsName() {
        assertThat(workspaceOwnershipResolver.resourceType()).isEqualTo("Workspace");
        assertThat(resourceOwnershipResolvers)
            .filteredOn(resourceOwnershipResolver -> "Workspace".equals(resourceOwnershipResolver.resourceType()))
            .containsExactly(workspaceOwnershipResolver);
    }

    @Test
    void testResolveOwnerTreatsTheIdAsTheOwningWorkspace() {
        assertThat(workspaceOwnershipResolver.resolveOwner(workspaceId)).isEqualTo(
            ResourceOwner.ofWorkspace(workspaceId));
    }

    @Test
    void testResolveOwnerReportsNoOwningUser() {
        ResourceOwner resourceOwner = workspaceOwnershipResolver.resolveOwner(workspaceId);

        assertThat(resourceOwner.ownerUserId()).isEmpty();
    }

    @Test
    void testResolveOwnerFailsClosedForANonNumericId() {
        assertThat(workspaceOwnershipResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testWorkspaceScopeIsGrantedToANonAdminMemberThroughTheRegisteredResolver() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "member@localhost.com", null, List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));

        assertThat(permissionService.hasResourceScope(workspaceId, "Workspace", "API_KEY_VIEW")).isTrue();
    }
}
