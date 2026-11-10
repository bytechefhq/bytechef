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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ConnectionOwnershipResolverIntTest {

    private static final long CONNECTION_ID = 1001L;
    private static final String OWNER_LOGIN = "owner@localhost.com";
    private static final long OWNER_USER_ID = 10L;

    @Autowired
    private ConnectionOwnershipResolver connectionOwnershipResolver;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private List<ResourceOwnershipResolver> resourceOwnershipResolvers;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("connection-ownership"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        workspaceConnectionRepository.deleteAll();
        workspaceRepository.deleteById(workspaceId);
    }

    @Test
    void testResourceType() {
        assertThat(connectionOwnershipResolver.resourceType()).isEqualTo("Connection");
        assertThat(resourceOwnershipResolvers)
            .filteredOn(resourceOwnershipResolver -> "Connection".equals(resourceOwnershipResolver.resourceType()))
            .containsExactly(connectionOwnershipResolver);
    }

    @Test
    void testResolvesTheWorkspaceAndTheOwnerOfAWorkspaceConnection() {
        stubConnection(PlatformType.AUTOMATION, OWNER_LOGIN);
        stubOwner();

        workspaceConnectionRepository.save(new WorkspaceConnection(CONNECTION_ID, workspaceId));

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
        assertThat(resourceOwner.ownerUserId()).hasValue(OWNER_USER_ID);
    }

    @Test
    void testResolvesOnlyTheOwnerOfAConnectionInNoWorkspace() {
        stubConnection(PlatformType.AUTOMATION, OWNER_LOGIN);
        stubOwner();

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner.workspaceId()).isEmpty();
        assertThat(resourceOwner.ownerUserId()).hasValue(OWNER_USER_ID);
    }

    @Test
    void testAConnectionWithoutCreatorHasNoOwner() {
        stubConnection(PlatformType.AUTOMATION, null);

        workspaceConnectionRepository.save(new WorkspaceConnection(CONNECTION_ID, workspaceId));

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
        assertThat(resourceOwner.ownerUserId()).isEmpty();
    }

    @Test
    void testACreatorMatchingNoUserHasNoOwner() {
        stubConnection(PlatformType.AUTOMATION, OWNER_LOGIN);

        when(userService.fetchUserByLogin(OWNER_LOGIN)).thenReturn(Optional.empty());

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner.ownerUserId()).isEmpty();
    }

    @Test
    void testAnUnsupportedUserLookupLeavesTheWorkspaceIntact() {
        stubConnection(PlatformType.AUTOMATION, OWNER_LOGIN);

        when(userService.fetchUserByLogin(OWNER_LOGIN)).thenThrow(new UnsupportedOperationException());

        workspaceConnectionRepository.save(new WorkspaceConnection(CONNECTION_ID, workspaceId));

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
        assertThat(resourceOwner.ownerUserId()).isEmpty();
    }

    @Test
    void testAnEmbeddedConnectionIsUnknown() {
        stubConnection(PlatformType.EMBEDDED, OWNER_LOGIN);
        stubOwner();

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAnUnknownConnectionIsUnknown() {
        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.empty());

        ResourceOwner resourceOwner = connectionOwnershipResolver.resolveOwner(CONNECTION_ID);

        assertThat(resourceOwner).isEqualTo(ResourceOwner.unknown());
    }

    private void stubConnection(PlatformType type, String createdBy) {
        Connection connection = new Connection();

        connection.setCreatedBy(createdBy);
        connection.setId(CONNECTION_ID);
        connection.setType(type);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));
    }

    private void stubOwner() {
        User user = new User();

        user.setId(OWNER_USER_ID);

        when(userService.fetchUserByLogin(OWNER_LOGIN)).thenReturn(Optional.of(user));
    }
}
