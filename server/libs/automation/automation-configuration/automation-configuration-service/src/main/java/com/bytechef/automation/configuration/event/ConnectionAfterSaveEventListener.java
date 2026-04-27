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

package com.bytechef.automation.configuration.event;

import com.bytechef.component.definition.Authorization;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.data.relational.core.conversion.AggregateChange;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 * @author Igor Beslic
 */
@Component
public class ConnectionAfterSaveEventListener extends AbstractRelationalEventListener<Connection> {

    private final ConnectionLifecycleFacade connectionLifecycleFacade;

    @SuppressFBWarnings("EI")
    public ConnectionAfterSaveEventListener(ConnectionLifecycleFacade connectionLifecycleFacade) {
        this.connectionLifecycleFacade = connectionLifecycleFacade;
    }

    /**
     * Checks weather connection requires refresh token flow. If method requires refresh token it will schedule token
     * refresh for newly inserted only if credentialsStatus is @Connection.CredentialStatus.VALID. For updated
     * connections if credentialStatus attribute was changed it schedules token refresh where new value credentialStatus
     * is #Connection.CredentialStatus.VALID and deletes existing scheduled refresh token routine if
     * #Connection.CredentialStatus.INVALID.
     *
     * @param afterSaveEvent will never be {@literal null}.
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    @Override
    protected void onAfterSave(AfterSaveEvent<Connection> afterSaveEvent) {
        Connection connection = afterSaveEvent.getEntity();

        Authorization.AuthorizationType authorizationType = connection.getAuthorizationType();

        if (Objects.isNull(authorizationType) || !authorizationType.requiresTokenRefresh()) {
            return;
        }

        AggregateChange<Connection> aggregateChange = afterSaveEvent.getAggregateChange();
        String tenantId = TenantContext.getCurrentTenantId();

        Connection.CredentialStatus credentialStatus = connection.getCredentialStatus();

        aggregateChange.forEachAction(dbAction -> {
            switch (dbAction) {
                case DbAction.Insert<?> a when credentialStatus == Connection.CredentialStatus.VALID ->
                    connectionLifecycleFacade.scheduleConnectionRefresh(
                        connection.getId(), connection.getParameters(), tenantId);
                case DbAction.InsertRoot<?> a when credentialStatus == Connection.CredentialStatus.VALID ->
                    connectionLifecycleFacade.scheduleConnectionRefresh(
                        connection.getId(), connection.getParameters(), tenantId);
                case DbAction.UpdateRoot<?> a -> {
                    if (connection.isCredentialsStatusUpdated()) {
                        return;
                    }

                    switch (credentialStatus) {
                        case Connection.CredentialStatus.VALID ->
                            connectionLifecycleFacade.scheduleConnectionRefresh(
                                connection.getId(), connection.getParameters(), tenantId);
                        case Connection.CredentialStatus.INVALID ->
                            connectionLifecycleFacade.deleteScheduledConnectionRefresh(connection.getId(), tenantId);
                        default -> {
                        }
                    }
                }
                default -> {
                }
            }
        });
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<Connection> beforeDeleteEvent) {
        Identifier connectionId = beforeDeleteEvent.getId();
        String tenantId = TenantContext.getCurrentTenantId();

        connectionLifecycleFacade.deleteScheduledConnectionRefresh((Long) connectionId.getValue(), tenantId);
    }
}
