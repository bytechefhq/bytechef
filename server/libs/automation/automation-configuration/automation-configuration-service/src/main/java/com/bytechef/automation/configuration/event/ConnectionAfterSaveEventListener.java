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
 */
@Component
public class ConnectionAfterSaveEventListener extends AbstractRelationalEventListener<Connection> {

    private final ConnectionLifecycleFacade connectionLifecycleFacade;

    @SuppressFBWarnings("EI")
    public ConnectionAfterSaveEventListener(ConnectionLifecycleFacade connectionLifecycleFacade) {
        this.connectionLifecycleFacade = connectionLifecycleFacade;
    }

    @Override
    protected void onAfterSave(AfterSaveEvent<Connection> afterSaveEvent) {
        Connection connection = afterSaveEvent.getEntity();

        Authorization.AuthorizationType authorizationType = connection.getAuthorizationType();

        if (Objects.isNull(authorizationType) || !authorizationType.hasRefreshToken()) {
            return;
        }

        AggregateChange<Connection> aggregateChange = afterSaveEvent.getAggregateChange();
        String tenantId = TenantContext.getCurrentTenantId();

        aggregateChange.forEachAction(dbAction -> {
            if ((dbAction instanceof DbAction.InsertRoot<?>) || (dbAction instanceof DbAction.Insert<?>)) {
                connectionLifecycleFacade.scheduleConnectionRefresh(
                    connection.getId(), connection.getParameters(), authorizationType, tenantId);
            }
            switch (dbAction) {
                case DbAction.Insert<?> a ->
                    connectionLifecycleFacade.scheduleConnectionRefresh(
                        connection.getId(), connection.getParameters(), authorizationType, tenantId);
                case DbAction.InsertRoot<?> a ->
                    connectionLifecycleFacade.scheduleConnectionRefresh(
                        connection.getId(), connection.getParameters(), authorizationType, tenantId);
                case DbAction.UpdateRoot<?> a -> {
                    if (connection.isCredentialsStatusUpdated()
                        && (connection.getCredentialStatus() != Connection.CredentialStatus.INVALID)) {
                        connectionLifecycleFacade.scheduleConnectionRefresh(
                            connection.getId(), connection.getParameters(), authorizationType, tenantId);
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
