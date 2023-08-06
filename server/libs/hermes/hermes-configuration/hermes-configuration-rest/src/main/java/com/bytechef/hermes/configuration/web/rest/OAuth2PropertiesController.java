
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

package com.bytechef.hermes.configuration.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnEnabled;
import com.bytechef.hermes.configuration.service.OAuth2Service;
import com.bytechef.hermes.configuration.web.rest.model.OAuth2PropertiesModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
@ConditionalOnEnabled("coordinator")
public class OAuth2PropertiesController implements Oauth2PropertiesApi {

    private final OAuth2Service oAuth2Service;

    @SuppressFBWarnings("EI")
    public OAuth2PropertiesController(OAuth2Service oAuth2Service) {
        this.oAuth2Service = oAuth2Service;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<OAuth2PropertiesModel> getOAuth2Properties() {
        return ResponseEntity.ok(
            new OAuth2PropertiesModel()
                .predefinedApps(oAuth2Service.getPredefinedApps())
                .redirectUri(oAuth2Service.getRedirectUri()));
    }
}
