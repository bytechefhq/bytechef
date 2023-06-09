
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

package com.bytechef.helios.connection.web.rest;

import com.bytechef.helios.connection.web.rest.model.OAuth2PropertiesModel;
import com.bytechef.hermes.connection.config.OAuth2Properties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class ConnectionOAuth2PropertiesController implements ConnectionOauth2PropertiesApi {

    private final ConversionService conversionService;
    private final OAuth2Properties oAuth2Properties;

    @SuppressFBWarnings("EI")
    public ConnectionOAuth2PropertiesController(ConversionService conversionService,
        OAuth2Properties oAuth2Properties) {
        this.conversionService = conversionService;
        this.oAuth2Properties = oAuth2Properties;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<OAuth2PropertiesModel> getOAuth2Properties() {
        return ResponseEntity.ok(conversionService.convert(oAuth2Properties, OAuth2PropertiesModel.class));
    }
}
