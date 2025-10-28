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

import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("connectionDefinitionFacade")
public class ConnectionDefinitionFacadeImpl implements ConnectionDefinitionFacade {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ContextFactory contextFactory;

    public ConnectionDefinitionFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, ContextFactory contextFactory) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.contextFactory = contextFactory;
    }

    @Override
    public AuthorizationCallbackResponse executeAuthorizationCallback(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters, String redirectUri) {

        return connectionDefinitionService.executeAuthorizationCallback(
            componentName, connectionVersion, authorizationType, connectionParameters,
            contextFactory.createContext(componentName, null), redirectUri);
    }

    @Override
    public Optional<String> executeBaseUri(String componentName, ComponentConnection componentConnection) {
        return connectionDefinitionService.executeBaseUri(
            componentName, componentConnection, contextFactory.createContext(componentName, componentConnection));
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, AuthorizationType authorizationType,
        Map<String, ?> connectionParameters) {

        return connectionDefinitionService.getOAuth2AuthorizationParameters(
            componentName, connectionVersion, authorizationType, connectionParameters,
            contextFactory.createContext(componentName, null));
    }
}
