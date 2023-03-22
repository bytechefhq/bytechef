
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.hermes.connection.config;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Arik Cohen
 */
@Component
@ConfigurationProperties(prefix = "bytechef.oauth2")
@SuppressFBWarnings("EI")
public class OAuth2Properties {

    private Map<String, OAuth2App> predefinedApps = new HashMap<>();
    private String redirectUri;

    public Connection checkPredefinedApp(Connection connection) {
        if (!StringUtils.hasText(connection.getParameter(Authorization.CLIENT_ID))) {
            if (predefinedApps.containsKey(connection.getComponentName())) {
                OAuth2Properties.OAuth2App oAuth2App = predefinedApps.get(connection.getComponentName());

                connection.putAllParameters(
                    Map.of(
                        Authorization.CLIENT_ID, oAuth2App.clientId(),
                        Authorization.CLIENT_SECRET, oAuth2App.clientSecret()));
            } else {
                throw new IllegalStateException(
                    "Component definition %s does not exist".formatted(connection.getComponentName()));
            }
        }

        return connection;
    }

    public Map<String, OAuth2App> getPredefinedApps() {
        return predefinedApps;
    }

    public void setPredefinedApps(Map<String, OAuth2App> predefinedApps) {
        this.predefinedApps = predefinedApps;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public record OAuth2App(String clientId, String clientSecret) {
    }
}
