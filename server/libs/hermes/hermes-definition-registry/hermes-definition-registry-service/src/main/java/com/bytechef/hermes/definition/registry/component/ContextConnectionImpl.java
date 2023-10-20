
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

package com.bytechef.hermes.definition.registry.component;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ContextConnectionImpl implements Context.Connection {

    private final String authorizationName;
    private final String componentName;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final int connectionVersion;
    private final Map<String, ?> parameters;

    @SuppressFBWarnings("EI")
    public ContextConnectionImpl(
        String authorizationName, String componentName, ConnectionDefinitionService connectionDefinitionService,
        int connectionVersion, Map<String, ?> parameters) {

        this.authorizationName = authorizationName;
        this.componentName = componentName;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionVersion = connectionVersion;
        this.parameters = parameters;
    }

    @Override
    public void applyAuthorization(AuthorizationContext authorizationContext) {
        connectionDefinitionService.executeAuthorizationApply(
            componentName, connectionVersion, parameters, authorizationName, authorizationContext);
    }

    @Override
    public Optional<String> fetchBaseUri() {
        return connectionDefinitionService.fetchBaseUri(
            componentName, connectionVersion, parameters);
    }

    @Override
    public String getBaseUri() {
        return fetchBaseUri().orElseThrow();
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
            ", connectionDefinitionService=" + connectionDefinitionService +
            ", connectionVersion=" + connectionVersion +
            ", parameters=" + parameters +
            '}';
    }
}
