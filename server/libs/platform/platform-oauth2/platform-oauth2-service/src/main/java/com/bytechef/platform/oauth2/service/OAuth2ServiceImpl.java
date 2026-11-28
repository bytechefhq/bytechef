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
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
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

    private static final String MCP_CLIENT_COMPONENT_NAME = "mcpClient";
    private static final String MCP_CLIENT_URL = "url";
    private static final String OAUTH2_AUTHENTICATION_TYPE = "oauth2";

    private final Oauth2 oauth2;
    private final RemoteMcpServerCatalogClient remoteMcpServerCatalogClient;
    private final RemoteOAuth2AppsClient remoteOAuth2AppsClient;

    public OAuth2ServiceImpl(
        ApplicationProperties applicationProperties, RemoteMcpServerCatalogClient remoteMcpServerCatalogClient,
        RemoteOAuth2AppsClient remoteOAuth2AppsClient) {

        this.oauth2 = applicationProperties.getOauth2();
        this.remoteMcpServerCatalogClient = remoteMcpServerCatalogClient;
        this.remoteOAuth2AppsClient = remoteOAuth2AppsClient;
    }

    @Override
    public Map<String, ?> checkPredefinedParameters(String componentName, Map<String, ?> parameters) {
        Map<String, Object> newParameters = new HashMap<>(parameters);

        if (StringUtils.isNotBlank(MapUtils.getString(parameters, Authorization.CLIENT_ID))) {
            return newParameters;
        }

        PredefinedApp predefinedApp = findPredefinedApp(componentName);

        if (predefinedApp != null) {
            newParameters.put(Authorization.CLIENT_ID, predefinedApp.clientId());
            newParameters.put(Authorization.CLIENT_SECRET, predefinedApp.clientSecret());
        } else if (MCP_CLIENT_COMPONENT_NAME.equals(componentName)) {
            applyMcpServerCatalogApp(newParameters);
        }

        return newParameters;
    }

    @Override
    public String getRedirectUri() {
        return oauth2.getRedirectUri();
    }

    @Override
    public List<String> getPredefinedApps() {
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

    private void applyMcpServerCatalogApp(Map<String, Object> parameters) {
        String url = MapUtils.getString(parameters, MCP_CLIENT_URL);

        if (StringUtils.isBlank(url)) {
            return;
        }

        String trimmedUrl = url.trim();

        for (McpServerCatalogEntry mcpServerCatalogEntry : remoteMcpServerCatalogClient.getMcpServerCatalog()) {
            McpServerCatalogAuthentication authentication = mcpServerCatalogEntry.authentication();

            if (trimmedUrl.equals(mcpServerCatalogEntry.url()) && hasPredefinedAppEndpoints(authentication) &&
                StringUtils.isNotBlank(mcpServerCatalogEntry.component())) {

                PredefinedApp predefinedApp = findPredefinedApp(mcpServerCatalogEntry.component());

                if (predefinedApp != null) {
                    parameters.put(Authorization.CLIENT_ID, predefinedApp.clientId());
                    parameters.put(Authorization.CLIENT_SECRET, predefinedApp.clientSecret());
                    parameters.put(Authorization.AUTHORIZATION_URL, authentication.authorizationUrl());
                    parameters.put(Authorization.TOKEN_URL, authentication.tokenUrl());
                }

                return;
            }
        }
    }

    private PredefinedApp findPredefinedApp(String componentName) {
        Map<String, OAuth2App> predefinedOAuth2Apps = oauth2.getPredefinedApps();

        OAuth2App oAuth2App = predefinedOAuth2Apps.get(componentName);

        if (oAuth2App != null) {
            return new PredefinedApp(oAuth2App.getClientId(), oAuth2App.getClientSecret());
        }

        Map<String, OAuthApp> remoteOAuth2Apps = remoteOAuth2AppsClient.getOAuth2Apps();

        OAuthApp oAuthApp = remoteOAuth2Apps.get(componentName);

        if (oAuthApp != null) {
            return new PredefinedApp(oAuthApp.clientId(), oAuthApp.clientSecret());
        }

        return null;
    }

    private static boolean hasPredefinedAppEndpoints(McpServerCatalogAuthentication authentication) {
        return authentication != null && OAUTH2_AUTHENTICATION_TYPE.equals(authentication.type()) &&
            !Boolean.TRUE.equals(authentication.dynamicClientRegistration()) &&
            StringUtils.isNotBlank(authentication.authorizationUrl()) &&
            StringUtils.isNotBlank(authentication.tokenUrl());
    }

    private record PredefinedApp(String clientId, String clientSecret) {
    }
}
