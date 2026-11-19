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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Oauth2;
import com.bytechef.config.ApplicationProperties.Oauth2.OAuth2App;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2ServiceTest {

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final RemoteOAuth2AppsClient remoteOAuth2AppsClient = mock(RemoteOAuth2AppsClient.class);
    private Map<String, OAuth2App> predefinedOAuth2Apps;

    @BeforeEach
    void beforeEach() {
        predefinedOAuth2Apps = new LinkedHashMap<>();

        Oauth2 oauth2 = applicationProperties.getOauth2();

        oauth2.setPredefinedApps(predefinedOAuth2Apps);
    }

    @Test
    void testRemoteAppFillsBlankClientId() {
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("googleDrive", new OAuthApp("remoteId", "remoteSecret")));

        OAuth2ServiceImpl oAuth2Service = new OAuth2ServiceImpl(applicationProperties, remoteOAuth2AppsClient);

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("googleDrive", Map.of());

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("remoteId");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("remoteSecret");
    }

    @Test
    void testPredefinedOAuth2AppWinsOverRemote() {
        predefinedOAuth2Apps.put("googleDrive", app("predefinedId", "predefinedSecret"));

        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("googleDrive", new OAuthApp("remoteId", "remoteSecret")));

        OAuth2ServiceImpl oAuth2Service = new OAuth2ServiceImpl(applicationProperties, remoteOAuth2AppsClient);

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("googleDrive", Map.of());

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("predefinedId");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("predefinedSecret");

        verify(remoteOAuth2AppsClient, never()).getOAuth2Apps();
    }

    @Test
    void testProvidedClientIdIsKept() {
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("googleDrive", new OAuthApp("remoteId", "remoteSecret")));

        OAuth2ServiceImpl oAuth2Service = new OAuth2ServiceImpl(applicationProperties, remoteOAuth2AppsClient);

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters(
            "googleDrive", Map.of(Authorization.CLIENT_ID, "mine", Authorization.CLIENT_SECRET, "mySecret"));

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("mine");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("mySecret");

        verify(remoteOAuth2AppsClient, never()).getOAuth2Apps();
    }

    @Test
    void testUnknownComponentLeavesParametersUnchanged() {
        when(remoteOAuth2AppsClient.getOAuth2Apps()).thenReturn(Map.of());

        OAuth2ServiceImpl oAuth2Service = new OAuth2ServiceImpl(applicationProperties, remoteOAuth2AppsClient);

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("slack", Map.of("a", "b"));

        assertThat(parameters).containsOnlyKeys("a");
    }

    @Test
    void testGetPredefinedAppsListsNamesFromBothSources() {
        predefinedOAuth2Apps.put("slack", app("predefinedId", "predefinedSecret"));
        predefinedOAuth2Apps.put("googleDrive", app("predefinedId", "predefinedSecret"));

        when(remoteOAuth2AppsClient.getOAuth2Apps()).thenReturn(
            Map.of(
                "googleDrive", new OAuthApp("remoteId", "remoteSecret"), "zendesk", new OAuthApp("id", "secret"),
                "asana", new OAuthApp("id", "secret")));

        OAuth2ServiceImpl oAuth2Service = new OAuth2ServiceImpl(applicationProperties, remoteOAuth2AppsClient);

        List<String> names = oAuth2Service.getPredefinedOAuth2Apps();

        assertThat(names).containsExactly("slack", "googleDrive", "asana", "zendesk");
    }

    private static OAuth2App app(String clientId, String clientSecret) {
        OAuth2App oAuth2App = new OAuth2App();

        oAuth2App.setClientId(clientId);
        oAuth2App.setClientSecret(clientSecret);

        return oAuth2App;
    }
}
