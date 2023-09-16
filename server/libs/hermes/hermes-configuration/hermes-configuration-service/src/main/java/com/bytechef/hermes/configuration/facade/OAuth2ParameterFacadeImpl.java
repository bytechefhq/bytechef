
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

package com.bytechef.hermes.configuration.facade;

import com.bytechef.hermes.configuration.service.OAuth2Service;
import com.bytechef.hermes.component.registry.domain.OAuth2AuthorizationParameters;
import com.bytechef.hermes.component.registry.service.RemoteConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2ParameterFacadeImpl implements OAuth2ParameterFacade {

    private final RemoteConnectionDefinitionService connectionDefinitionService;
    private final OAuth2Service oAuth2Service;

    @SuppressFBWarnings("EI")
    public OAuth2ParameterFacadeImpl(
        RemoteConnectionDefinitionService connectionDefinitionService, OAuth2Service oAuth2Service) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    public OAuth2AuthorizationParameters getOAuth2AuthorizationParameters(
        String componentName, int connectionVersion, Map<String, ?> connectionParameters, String authorizationName) {

        return connectionDefinitionService.getOAuth2AuthorizationParameters(
            componentName, connectionVersion,
            oAuth2Service.checkPredefinedParameters(componentName, connectionParameters),
            authorizationName);
    }
}
