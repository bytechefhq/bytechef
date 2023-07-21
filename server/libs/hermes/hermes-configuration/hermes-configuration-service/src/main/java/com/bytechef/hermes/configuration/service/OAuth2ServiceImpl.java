
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

package com.bytechef.hermes.configuration.service;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.configuration.config.OAuth2Properties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private final OAuth2Properties oAuth2Properties;

    @SuppressFBWarnings("EI")
    public OAuth2ServiceImpl(OAuth2Properties oAuth2Properties) {
        this.oAuth2Properties = oAuth2Properties;
    }

    @Override
    public Map<String, ?> checkPredefinedApp(String componentName, Map<String, ?> connectionParameters) {
        Map<String, Object> newConnectionParameters = new HashMap<>(connectionParameters);

        if (!StringUtils.hasText((String) connectionParameters.get(Authorization.CLIENT_ID))) {
            Map<String, OAuth2Properties.OAuth2App> oAuth2AppMap = oAuth2Properties.getPredefinedApps();

            if (oAuth2AppMap.containsKey(componentName)) {
                OAuth2Properties.OAuth2App oAuth2App = oAuth2AppMap.get(componentName);

                newConnectionParameters.putAll(
                    Map.of(
                        Authorization.CLIENT_ID, oAuth2App.clientId(),
                        Authorization.CLIENT_SECRET, oAuth2App.clientSecret()));
            } else {
                throw new IllegalStateException(
                    "Component definition %s does not exist".formatted(componentName));
            }
        }

        return newConnectionParameters;
    }

    @Override
    public String getRedirectUri() {
        return oAuth2Properties.getRedirectUri();
    }

    @Override
    public List<String> getPredefinedApps() {
        return oAuth2Properties.getPredefinedApps()
            .keySet()
            .stream()
            .toList();
    }
}
