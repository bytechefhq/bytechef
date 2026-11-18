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
    private final RemoteOAuth2Apps remoteOAuth2Apps;

    public OAuth2ServiceImpl(ApplicationProperties applicationProperties, RemoteOAuth2Apps remoteOAuth2Apps) {
        this.oauth2 = applicationProperties.getOauth2();
        this.remoteOAuth2Apps = remoteOAuth2Apps;
    }

    @Override
    public Map<String, ?> checkPredefinedParameters(String componentName, Map<String, ?> parameters) {
        Map<String, Object> newParameters = new HashMap<>(parameters);

        if (StringUtils.isBlank(MapUtils.getString(parameters, Authorization.CLIENT_ID))) {
            Map<String, OAuth2App> oAuth2AppMap = getEffectiveApps();

            if (oAuth2AppMap.containsKey(componentName)) {
                OAuth2App oAuth2App = oAuth2AppMap.get(componentName);

                newParameters.putAll(
                    Map.of(
                        Authorization.CLIENT_ID, oAuth2App.getClientId(),
                        Authorization.CLIENT_SECRET, oAuth2App.getClientSecret()));
            }
        }

        return newParameters;
    }

    @Override
    public String getRedirectUri() {
        return oauth2.getRedirectUri();
    }

    @Override
    public List<String> getPredefinedApps() {
        Map<String, OAuth2App> yamlApps = oauth2.getPredefinedApps();

        Set<String> names = new LinkedHashSet<>(yamlApps.keySet());

        Map<String, OAuth2App> remoteApps = remoteOAuth2Apps.getOAuth2Apps();

        names.addAll(
            remoteApps.keySet()
                .stream()
                .sorted()
                .toList());

        return List.copyOf(names);
    }

    private Map<String, OAuth2App> getEffectiveApps() {
        Map<String, OAuth2App> remoteApps = remoteOAuth2Apps.getOAuth2Apps();

        Map<String, OAuth2App> apps = new HashMap<>(remoteApps);

        apps.putAll(oauth2.getPredefinedApps());

        return apps;
    }
}
