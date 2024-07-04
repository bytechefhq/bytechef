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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.web.rest.model.GetOAuth2AuthorizationParametersRequestModel;
import com.bytechef.platform.configuration.web.rest.model.OAuth2AuthorizationParametersModel;
import com.bytechef.platform.configuration.web.rest.model.OAuth2PropertiesModel;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnEndpoint
public class OAuth2ApiController implements Oauth2Api {

    private final ConversionService conversionService;
    private final OAuth2ParametersFacade oAuth2ParametersFacade;
    private final OAuth2Service oAuth2Service;

    @SuppressFBWarnings("EI")
    public OAuth2ApiController(
        ConversionService conversionService, OAuth2ParametersFacade oAuth2ParametersFacade,
        OAuth2Service oAuth2Service) {

        this.conversionService = conversionService;
        this.oAuth2ParametersFacade = oAuth2ParametersFacade;
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    public ResponseEntity<OAuth2AuthorizationParametersModel> getOAuth2AuthorizationParameters(
        GetOAuth2AuthorizationParametersRequestModel parametersRequestModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
                    parametersRequestModel.getComponentName(), parametersRequestModel.getConnectionVersion(),
                    parametersRequestModel.getParameters(), parametersRequestModel.getAuthorizationName()),
                OAuth2AuthorizationParametersModel.class));
    }

    @Override
    public ResponseEntity<OAuth2PropertiesModel> getOAuth2Properties() {
        return ResponseEntity.ok(
            new OAuth2PropertiesModel()
                .predefinedApps(oAuth2Service.getPredefinedApps())
                .redirectUri(oAuth2Service.getRedirectUri()));
    }
}
