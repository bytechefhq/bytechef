
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.facade;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionFacadeImpl implements ConnectionDefinitionFacade {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionDefinitionFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
    }

    @Override
    public void applyAuthorization(long connectionId, Authorization.AuthorizationContext authorizationContext) {
        Connection connection = connectionService.getConnection(connectionId);

        connectionDefinitionService.applyAuthorization(connection, authorizationContext);
    }

    @Override
    public Optional<String> fetchBaseUri(long connectionId) {
        Connection connection = connectionService.getConnection(connectionId);

        return connectionDefinitionService.fetchBaseUri(connection);
    }
}
