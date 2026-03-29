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

package com.bytechef.platform.component.facade;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Nikolina Spehar
 */
@Service("connectionDefinitionFacade")
public class ConnectionDefinitionFacadeImpl implements ConnectionDefinitionFacade {

    private final ConnectionService connectionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final TokenRefreshHandler tokenRefreshHandler;

    @SuppressWarnings("E1")
    public ConnectionDefinitionFacadeImpl(
        ConnectionService connectionService, ConnectionDefinitionService connectionDefinitionService,
        TokenRefreshHandler tokenRefreshHandler) {

        this.connectionService = connectionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.tokenRefreshHandler = tokenRefreshHandler;
    }

    @Override
    public ComponentConnection executeConnectionRefresh(@Nullable Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        Context context = connectionDefinitionService.createConnectionRefreshContext(
            componentConnection.getComponentName(), componentConnection);

        return tokenRefreshHandler.refreshCredentials(componentConnection, context);
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }
}
