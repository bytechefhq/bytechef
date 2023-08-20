
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

package com.bytechef.hermes.configuration.remote.web.rest.service;

import com.bytechef.hermes.configuration.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal/oauth2-service")
public class OAuth2ServiceController {

    private final OAuth2Service oAuth2Service;

    @SuppressFBWarnings("EI")
    public OAuth2ServiceController(OAuth2Service oAuth2Service) {
        this.oAuth2Service = oAuth2Service;
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/check-predefined-parameters/{componentName}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Map<String, ?>> checkPredefinedParameters(
        @PathVariable String componentName, @RequestBody Map<String, ?> connectionParameters) {

        return ResponseEntity
            .ok(oAuth2Service.checkPredefinedParameters(componentName, connectionParameters));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-redirect-uri",
        produces = {
            "application/json"
        })
    public ResponseEntity<String> getRedirectUri() {
        return ResponseEntity.ok(oAuth2Service.getRedirectUri());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-predefined-apps",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<String>> getPredefinedApps() {
        return ResponseEntity.ok(oAuth2Service.getPredefinedApps());
    }
}
