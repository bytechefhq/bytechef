
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

import com.bytechef.oauth2.config.OAuth2Properties;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.GetOAuth2AuthorizationParametersRequestModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OAuth2AuthorizationParametersModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class ConnectionDefinitionController implements ConnectionDefinitionsApi {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConversionService conversionService;
    private final OAuth2Properties oAuth2Properties;

    @SuppressFBWarnings("EI")
    public ConnectionDefinitionController(
        ConnectionDefinitionService connectionDefinitionService, ConversionService conversionService,
        OAuth2Properties oAuth2Properties) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.conversionService = conversionService;
        this.oAuth2Properties = oAuth2Properties;
    }

    @Override
    public Mono<ResponseEntity<OAuth2AuthorizationParametersModel>> getOAuth2AuthorizationParameters(
        Mono<GetOAuth2AuthorizationParametersRequestModel> parametersRequestModelMono, ServerWebExchange exchange) {

        return parametersRequestModelMono
            .map(parametersRequestModel -> connectionDefinitionService.getOAuth2Parameters(
                parametersRequestModel.getComponentName(), parametersRequestModel.getConnectionVersion(),
                oAuth2Properties.checkPredefinedApp(
                    parametersRequestModel.getComponentName(), parametersRequestModel.getParameters()),
                parametersRequestModel.getAuthorizationName()))
            .map(oAuth2AuthorizationParametersDTO -> conversionService.convert(
                oAuth2AuthorizationParametersDTO, OAuth2AuthorizationParametersModel.class))
            .map(ResponseEntity::ok);
    }
}
