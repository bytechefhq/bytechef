
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ContextConnectionImpl implements Context.Connection, AuthorizationContextConnection {

    private final String authorizationName;
    private final String componentName;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final int connectionVersion;
    private final Map<String, ?> parameters;

    @SuppressFBWarnings("EI")
    public ContextConnectionImpl(Connection connection, ConnectionDefinitionService connectionDefinitionService) {
        this.authorizationName = connection.getAuthorizationName();
        this.componentName = connection.getComponentName();
        this.connectionVersion = connection.getConnectionVersion();
        this.parameters = connection.getParameters();
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @SuppressFBWarnings("EI")
    public ContextConnectionImpl(
        String componentName, int connectionVersion, Map<String, ?> parameters, String authorizationName,
        ConnectionDefinitionService connectionDefinitionService) {

        this.authorizationName = authorizationName;
        this.componentName = componentName;
        this.connectionVersion = connectionVersion;
        this.parameters = parameters;
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public Authorization.ApplyResponse applyAuthorization() {
        return connectionDefinitionService.executeAuthorizationApply(
            componentName, connectionVersion, parameters, authorizationName);
    }

    @Override
    public Optional<String> fetchBaseUri() {
        return connectionDefinitionService.executeBaseUri(componentName, connectionVersion, parameters);
    }

    @Override
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString() {
        return "ContextConnection{" +
            "authorizationName='" + authorizationName + '\'' +
            ", componentName='" + componentName + '\'' +
            ", connectionVersion=" + connectionVersion +
            ", parameters=" + parameters +
            '}';
    }
}
