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

package com.bytechef.automation.connection.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.event.ConnectionCredentialsInvalidatedEvent;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.conversion.RootAggregateChange;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;

/**
 * @author Ivica Cardic
 */
class ConnectionAfterSaveEventListenerTest {

    private static final long CONNECTION_ID = 42L;

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final ConnectionLifecycleFacade connectionLifecycleFacade = mock(ConnectionLifecycleFacade.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ConnectionAfterSaveEventListener connectionAfterSaveEventListener =
        new ConnectionAfterSaveEventListener(
            applicationEventPublisher, connectionLifecycleFacade, projectDeploymentWorkflowService);

    @Test
    void testInvalidStatusPublishesEventBeforeDisablingWorkflows() {
        Connection connection = createConnection(Connection.CredentialStatus.INVALID);

        connection.setCredentialsStatusUpdated();

        connectionAfterSaveEventListener.onApplicationEvent(createUpdateEvent(connection));

        InOrder inOrder = inOrder(applicationEventPublisher, projectDeploymentWorkflowService);

        inOrder.verify(applicationEventPublisher)
            .publishEvent(new ConnectionCredentialsInvalidatedEvent(CONNECTION_ID));
        inOrder.verify(projectDeploymentWorkflowService)
            .disableProjectDeploymentWorkflowByConnectionId(CONNECTION_ID);
    }

    @Test
    void testUnchangedInvalidStatusPublishesNoEvent() {
        Connection connection = createConnection(Connection.CredentialStatus.INVALID);

        connectionAfterSaveEventListener.onApplicationEvent(createUpdateEvent(connection));

        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void testValidStatusPublishesNoEvent() {
        Connection connection = createConnection(Connection.CredentialStatus.VALID);

        connection.setCredentialsStatusUpdated();

        connectionAfterSaveEventListener.onApplicationEvent(createUpdateEvent(connection));

        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    private static Connection createConnection(Connection.CredentialStatus credentialStatus) {
        Connection connection = new Connection();

        connection.setAuthorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        connection.setCredentialStatus(credentialStatus);
        connection.setId(CONNECTION_ID);

        return connection;
    }

    private static AfterSaveEvent<Connection> createUpdateEvent(Connection connection) {
        RootAggregateChange<Connection> aggregateChange = MutableAggregateChange.forSave(connection);

        aggregateChange.setRootAction(new DbAction.UpdateRoot<>(connection, null));

        return new AfterSaveEvent<>(connection, aggregateChange);
    }
}
