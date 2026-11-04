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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver.ConnectionAccess;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTestConfigurationFacadeConnectionAccessTest {

    private static final long CONNECTION_ID = 7L;
    private static final long DEVELOPMENT_ENVIRONMENT_ID = 1L;
    private static final long PRODUCTION_ENVIRONMENT_ID = 0L;
    private static final Workflow WORKFLOW = new Workflow(
        "workflow1",
        """
            {
                "tasks": [
                    {
                        "name": "sendChannelMessage_1",
                        "type": "slack/v1/sendChannelMessage",
                        "parameters": {}
                    }
                ]
            }
            """,
        Format.JSON);

    private final ComponentConnectionFacade componentConnectionFacade = mock(ComponentConnectionFacade.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    @BeforeEach
    void beforeEach() {
        Connection connection = new Connection();

        connection.setComponentName("slack");

        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);
        when(componentConnectionFacade.getComponentConnection("workflow1", "sendChannelMessage_1", "slack"))
            .thenReturn(new ComponentConnection("slack", 1, "sendChannelMessage_1", "slack", true));
        when(componentConnectionFacade.getClusterElementComponentConnection(
            "workflow1", "aiAgent_1", "MODEL", "model_1", "model_1"))
                .thenReturn(new ComponentConnection("slack", 1, "aiAgent_1", "model_1", true));
        when(workflowService.getWorkflow("workflow1")).thenReturn(WORKFLOW);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionIsDeniedWhenNoResolverClaimsTheWorkflow() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.ABSTAIN);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, 0L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNothingSaved();
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionIsDeniedWithoutAnyResolver() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade();

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, 0L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNothingSaved();
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionIsDeniedWhenTheClaimingResolverDenies() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.ABSTAIN,
            (workflowId, connectionId, environmentId) -> ConnectionAccess.DENIED);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, 0L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNothingSaved();
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionIsSavedWhenTheClaimingResolverGrants() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.ABSTAIN,
            (workflowId, connectionId, environmentId) -> ConnectionAccess.GRANTED);

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, 0L);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, false, 0L);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionLetsATenantAdminPass() {
        authenticate(AuthorityConstants.ADMIN);

        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.DENIED);

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, 0L);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, false, 0L);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionIsDeniedForANonAdminUser() {
        authenticate(AuthorityConstants.USER);

        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.DENIED);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, 0L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNothingSaved();
    }

    @Test
    void testSaveClusterElementTestConfigurationConnectionIsDeniedWhenTheClaimingResolverDenies() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.DENIED);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            "workflow1", "aiAgent_1", "MODEL", "model_1", "model_1", CONNECTION_ID, 0L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNothingSaved();
    }

    @Test
    void testSaveClusterElementTestConfigurationConnectionIsSavedWhenTheClaimingResolverGrants() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.GRANTED);

        workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            "workflow1", "aiAgent_1", "MODEL", "model_1", "model_1", CONNECTION_ID, 0L);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "aiAgent_1", "model_1", CONNECTION_ID, false, 0L);
    }

    @Test
    void testSaveWorkflowTestConfigurationIsDeniedWhenAConnectionIsDenied() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            (workflowId, connectionId, environmentId) -> ConnectionAccess.DENIED);

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(CONNECTION_ID, "slack", "sendChannelMessage_1")));
        workflowTestConfiguration.setEnvironmentId(0L);
        workflowTestConfiguration.setInputs(Map.of());
        workflowTestConfiguration.setWorkflowId("workflow1");

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            workflowTestConfiguration))
                .isInstanceOf(AccessDeniedException.class);

        verify(workflowTestConfigurationService, never()).saveWorkflowTestConfiguration(workflowTestConfiguration);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionPassesTheTargetEnvironmentToTheResolver() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            createDevelopmentEnvironmentOnlyResolver());

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, DEVELOPMENT_ENVIRONMENT_ID);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, PRODUCTION_ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, false, DEVELOPMENT_ENVIRONMENT_ID);
        verify(workflowTestConfigurationService, never()).saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", CONNECTION_ID, false, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testSaveClusterElementTestConfigurationConnectionPassesTheTargetEnvironmentToTheResolver() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            createDevelopmentEnvironmentOnlyResolver());

        workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            "workflow1", "aiAgent_1", "MODEL", "model_1", "model_1", CONNECTION_ID, DEVELOPMENT_ENVIRONMENT_ID);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            "workflow1", "aiAgent_1", "MODEL", "model_1", "model_1", CONNECTION_ID, PRODUCTION_ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "aiAgent_1", "model_1", CONNECTION_ID, false, DEVELOPMENT_ENVIRONMENT_ID);
        verify(workflowTestConfigurationService, never()).saveWorkflowTestConfigurationConnection(
            "workflow1", "aiAgent_1", "model_1", CONNECTION_ID, false, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testSaveWorkflowTestConfigurationPassesItsEnvironmentToTheResolver() {
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade = createWorkflowTestConfigurationFacade(
            createDevelopmentEnvironmentOnlyResolver());

        WorkflowTestConfiguration developmentWorkflowTestConfiguration = createWorkflowTestConfiguration(
            DEVELOPMENT_ENVIRONMENT_ID);
        WorkflowTestConfiguration productionWorkflowTestConfiguration = createWorkflowTestConfiguration(
            PRODUCTION_ENVIRONMENT_ID);

        workflowTestConfigurationFacade.saveWorkflowTestConfiguration(developmentWorkflowTestConfiguration);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            productionWorkflowTestConfiguration))
                .isInstanceOf(AccessDeniedException.class);

        verify(workflowTestConfigurationService).saveWorkflowTestConfiguration(
            same(developmentWorkflowTestConfiguration));
        verify(workflowTestConfigurationService, never()).saveWorkflowTestConfiguration(
            same(productionWorkflowTestConfiguration));
    }

    private static WorkflowConnectionAccessResolver createDevelopmentEnvironmentOnlyResolver() {
        return (workflowId, connectionId, environmentId) -> environmentId == DEVELOPMENT_ENVIRONMENT_ID
            ? ConnectionAccess.GRANTED : ConnectionAccess.DENIED;
    }

    private static WorkflowTestConfiguration createWorkflowTestConfiguration(long environmentId) {
        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(CONNECTION_ID, "slack", "sendChannelMessage_1")));
        workflowTestConfiguration.setEnvironmentId(environmentId);
        workflowTestConfiguration.setInputs(Map.of());
        workflowTestConfiguration.setWorkflowId("workflow1");

        return workflowTestConfiguration;
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "user@localhost.com", null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private WorkflowTestConfigurationFacade createWorkflowTestConfigurationFacade(
        WorkflowConnectionAccessResolver... workflowConnectionAccessResolvers) {

        Map<String, Object> beans = new LinkedHashMap<>();

        for (int i = 0; i < workflowConnectionAccessResolvers.length; i++) {
            beans.put("workflowConnectionAccessResolver" + i, workflowConnectionAccessResolvers[i]);
        }

        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory(beans);

        return new WorkflowTestConfigurationFacadeImpl(
            connectionService, componentConnectionFacade,
            staticListableBeanFactory.getBeanProvider(WorkflowConnectionAccessResolver.class), workflowService,
            workflowTestConfigurationService);
    }

    private void verifyNothingSaved() {
        verify(workflowTestConfigurationService, never()).saveWorkflowTestConfigurationConnection(
            anyString(), anyString(), anyString(), anyLong(), anyBoolean(), anyLong());
    }
}
