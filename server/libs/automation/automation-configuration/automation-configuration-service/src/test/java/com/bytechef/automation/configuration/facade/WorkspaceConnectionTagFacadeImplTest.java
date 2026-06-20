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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Verifies that {@link WorkspaceConnectionFacadeImpl#getConnectionTags(long)} scopes the tag lookup to the workspace's
 * own connections, not the global connection table.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceConnectionTagFacadeImplTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private ConnectionLifecycleFacade connectionLifecycleFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ResourceVisibilityResolver resourceVisibilityResolver;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private TagService tagService;

    @Mock
    private UserService userService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    @Mock
    private WorkspaceFacade workspaceFacade;

    private WorkspaceConnectionFacadeImpl workspaceConnectionFacade;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        workspaceConnectionFacade = new WorkspaceConnectionFacadeImpl(
            applicationEventPublisher, connectionFacade, connectionLifecycleFacade, connectionService,
            resourceVisibilityResolver, emptyProvider, projectDeploymentWorkflowService, projectService, tagService,
            userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);
    }

    @Test
    void testGetConnectionTagsScopesToWorkspace() {
        WorkspaceConnection workspaceConnection = new WorkspaceConnection(20L, 5L);

        Connection connection = new Connection();

        connection.setTagIds(List.of(10L, 11L));

        when(workspaceConnectionService.getWorkspaceConnections(5L)).thenReturn(List.of(workspaceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(connection));
        when(tagService.getTags(List.of(10L, 11L))).thenReturn(List.of(new Tag("a"), new Tag("b")));

        List<Tag> tags = workspaceConnectionFacade.getConnectionTags(5L);

        assertThat(tags).hasSize(2);
    }
}
