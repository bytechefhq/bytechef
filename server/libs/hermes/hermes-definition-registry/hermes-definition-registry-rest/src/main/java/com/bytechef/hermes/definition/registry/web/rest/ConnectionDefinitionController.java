
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

package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.hermes.definition.registry.web.rest.model.GetOAuth2AuthorizationParametersRequestModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OAuth2AuthorizationParametersModel;
import com.bytechef.hermes.definition.registry.web.rest.oauth2.OAuth2ParameterAccessor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class ConnectionDefinitionController implements ConnectionDefinitionsApi {

    private final OAuth2ParameterAccessor oAuth2ParameterAccessor;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectionDefinitionController(
        OAuth2ParameterAccessor oAuth2ParameterAccessor, ConversionService conversionService) {

        this.oAuth2ParameterAccessor = oAuth2ParameterAccessor;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<OAuth2AuthorizationParametersModel> getOAuth2AuthorizationParameters(
        GetOAuth2AuthorizationParametersRequestModel parametersRequestModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                oAuth2ParameterAccessor.getOAuth2Parameters(
                    parametersRequestModel.getComponentName(), parametersRequestModel.getConnectionVersion(),
                    parametersRequestModel.getParameters(), parametersRequestModel.getAuthorizationName()),
                OAuth2AuthorizationParametersModel.class));
    }
}
