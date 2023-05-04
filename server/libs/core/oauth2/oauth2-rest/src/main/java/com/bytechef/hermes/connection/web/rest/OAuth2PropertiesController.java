
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

package com.bytechef.hermes.connection.web.rest;

import com.bytechef.oauth2.config.OAuth2Properties;
import com.bytechef.hermes.connection.web.rest.model.OAuth2PropertiesModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class OAuth2PropertiesController implements Oauth2PropertiesApi {

    private final ConversionService conversionService;
    private final OAuth2Properties oAuth2Properties;

    @SuppressFBWarnings("EI")
    public OAuth2PropertiesController(ConversionService conversionService, OAuth2Properties oAuth2Properties) {
        this.conversionService = conversionService;
        this.oAuth2Properties = oAuth2Properties;
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<OAuth2PropertiesModel>> getOAuth2Properties(ServerWebExchange exchange) {
        return Mono.just(conversionService.convert(oAuth2Properties, OAuth2PropertiesModel.class))
            .map(ResponseEntity::ok);
    }
}
