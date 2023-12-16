/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.configuration.facade;

import com.bytechef.hermes.component.registry.domain.ComponentConnection;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.oauth2.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2ParameterFacadeImpl implements OAuth2ParameterFacade {

    private final ConnectionDefinitionFacade connectionDefinitionFacade;
    private final OAuth2Service oAuth2Service;

    @SuppressFBWarnings("EI")
    public OAuth2ParameterFacadeImpl(
        ConnectionDefinitionFacade connectionDefinitionFacade, OAuth2Service oAuth2Service) {

        this.connectionDefinitionFacade = connectionDefinitionFacade;
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName) {

        return connectionDefinitionFacade.getOAuth2AuthorizationParameters(
            componentName,
            new ComponentConnection(
                connectionVersion, oAuth2Service.checkPredefinedParameters(componentName, connectionParameters),
                authorizationName));
    }
}
