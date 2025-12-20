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

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2ParametersFacadeImpl implements OAuth2ParametersFacade {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final OAuth2Service oAuth2Service;

    public OAuth2ParametersFacadeImpl(
        ConnectionDefinitionService connectionDefinitionService, OAuth2Service oAuth2Service) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters,
        AuthorizationType authorizationType) {

        return connectionDefinitionService.getOAuth2AuthorizationParameters(
            componentName, connectionVersion, authorizationType,
            oAuth2Service.checkPredefinedParameters(componentName, connectionParameters));
    }
}
