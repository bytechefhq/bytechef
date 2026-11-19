/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.oauth2.service;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Oauth2;
import com.bytechef.config.ApplicationProperties.Oauth2.OAuth2App;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    private final Oauth2 oauth2;
    private final RemoteOAuth2AppsClient remoteOAuth2AppsClient;

    public OAuth2ServiceImpl(ApplicationProperties applicationProperties,
        RemoteOAuth2AppsClient remoteOAuth2AppsClient) {
        this.oauth2 = applicationProperties.getOauth2();
        this.remoteOAuth2AppsClient = remoteOAuth2AppsClient;
    }

    @Override
    public Map<String, ?> checkPredefinedParameters(String componentName, Map<String, ?> parameters) {
        Map<String, Object> newParameters = new HashMap<>(parameters);

        if (StringUtils.isBlank(MapUtils.getString(parameters, Authorization.CLIENT_ID))) {
            Map<String, OAuth2App> predefinedOAuth2Apps = oauth2.getPredefinedApps();

            OAuth2App oAuth2App = predefinedOAuth2Apps.get(componentName);

            if (oAuth2App != null) {
                newParameters.putAll(
                    Map.of(
                        Authorization.CLIENT_ID, oAuth2App.getClientId(),
                        Authorization.CLIENT_SECRET, oAuth2App.getClientSecret()));
            } else {
                Map<String, OAuthApp> remoteOAuth2Apps = remoteOAuth2AppsClient.getOAuth2Apps();

                OAuthApp oAuthApp = remoteOAuth2Apps.get(componentName);

                if (oAuthApp != null) {
                    newParameters.putAll(
                        Map.of(
                            Authorization.CLIENT_ID, oAuthApp.clientId(),
                            Authorization.CLIENT_SECRET, oAuthApp.clientSecret()));
                }
            }
        }

        return newParameters;
    }

    @Override
    public String getRedirectUri() {
        return oauth2.getRedirectUri();
    }

    @Override
    public List<String> getPredefinedOAuth2Apps() {
        Map<String, OAuth2App> predefinedOAuth2Apps = oauth2.getPredefinedApps();

        Set<String> names = new LinkedHashSet<>(predefinedOAuth2Apps.keySet());

        Map<String, OAuthApp> remoteOAuth2Apps = remoteOAuth2AppsClient.getOAuth2Apps();

        names.addAll(
            remoteOAuth2Apps.keySet()
                .stream()
                .sorted()
                .toList());

        return List.copyOf(names);
    }
}
