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
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
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

    private static final String LINEAR_URL = "https://mcp.linear.app/mcp";
    private static final String LINEAR_AUTHORIZATION_URL = "https://linear.app/oauth/authorize";
    private static final String LINEAR_TOKEN_URL = "https://api.linear.app/oauth/token";

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final RemoteMcpServerCatalogClient remoteMcpServerCatalogClient = mock(RemoteMcpServerCatalogClient.class);
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

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("googleDrive", Map.of());

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("remoteId");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("remoteSecret");
    }

    @Test
    void testPredefinedOAuth2AppWinsOverRemote() {
        predefinedOAuth2Apps.put("googleDrive", app("predefinedId", "predefinedSecret"));

        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("googleDrive", new OAuthApp("remoteId", "remoteSecret")));

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("googleDrive", Map.of());

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("predefinedId");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("predefinedSecret");

        verify(remoteOAuth2AppsClient, never()).getOAuth2Apps();
    }

    @Test
    void testProvidedClientIdIsKept() {
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("googleDrive", new OAuthApp("remoteId", "remoteSecret")));

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters(
            "googleDrive", Map.of(Authorization.CLIENT_ID, "mine", Authorization.CLIENT_SECRET, "mySecret"));

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("mine");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("mySecret");

        verify(remoteOAuth2AppsClient, never()).getOAuth2Apps();
    }

    @Test
    void testUnknownComponentLeavesParametersUnchanged() {
        when(remoteOAuth2AppsClient.getOAuth2Apps()).thenReturn(Map.of());

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

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

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        List<String> names = oAuth2Service.getPredefinedApps();

        assertThat(names).containsExactly("slack", "googleDrive", "asana", "zendesk");
    }

    @Test
    void testCatalogComponentAppFillsMcpClientCredentialsAndCatalogEndpoints() {
        when(remoteMcpServerCatalogClient.getMcpServerCatalog())
            .thenReturn(List.of(linearEntry(false, LINEAR_AUTHORIZATION_URL, LINEAR_TOKEN_URL, "linear")));
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("linear", new OAuthApp("linearId", "linearSecret")));

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters(
            "mcpClient",
            Map.of(
                "url", LINEAR_URL, Authorization.AUTHORIZATION_URL, "https://attacker.example.com/authorize",
                Authorization.TOKEN_URL, "https://attacker.example.com/token"));

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("linearId");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("linearSecret");
        assertThat(parameters.get(Authorization.AUTHORIZATION_URL)).isEqualTo(LINEAR_AUTHORIZATION_URL);
        assertThat(parameters.get(Authorization.TOKEN_URL)).isEqualTo(LINEAR_TOKEN_URL);
        assertThat(parameters.get("url")).isEqualTo(LINEAR_URL);
    }

    @Test
    void testLocalPredefinedAppOfCatalogComponentIsUsed() {
        predefinedOAuth2Apps.put("linear", app("localId", "localSecret"));

        when(remoteMcpServerCatalogClient.getMcpServerCatalog())
            .thenReturn(List.of(linearEntry(false, LINEAR_AUTHORIZATION_URL, LINEAR_TOKEN_URL, "linear")));
        when(remoteOAuth2AppsClient.getOAuth2Apps()).thenReturn(Map.of());

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("mcpClient", Map.of("url", LINEAR_URL));

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("localId");
        assertThat(parameters.get(Authorization.CLIENT_SECRET)).isEqualTo("localSecret");
        assertThat(parameters.get(Authorization.TOKEN_URL)).isEqualTo(LINEAR_TOKEN_URL);
    }

    @Test
    void testCatalogEntryWithDynamicClientRegistrationIsIgnored() {
        when(remoteMcpServerCatalogClient.getMcpServerCatalog())
            .thenReturn(List.of(linearEntry(true, LINEAR_AUTHORIZATION_URL, LINEAR_TOKEN_URL, "linear")));
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("linear", new OAuthApp("linearId", "linearSecret")));

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("mcpClient", Map.of("url", LINEAR_URL));

        assertThat(parameters).containsOnlyKeys("url");
    }

    @Test
    void testCatalogEntryWithoutEndpointsIsIgnored() {
        when(remoteMcpServerCatalogClient.getMcpServerCatalog())
            .thenReturn(List.of(linearEntry(false, LINEAR_AUTHORIZATION_URL, " ", "linear")));
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("linear", new OAuthApp("linearId", "linearSecret")));

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("mcpClient", Map.of("url", LINEAR_URL));

        assertThat(parameters).containsOnlyKeys("url");
    }

    @Test
    void testCatalogComponentWithoutAppIsIgnored() {
        when(remoteMcpServerCatalogClient.getMcpServerCatalog())
            .thenReturn(List.of(linearEntry(false, LINEAR_AUTHORIZATION_URL, LINEAR_TOKEN_URL, "linear")));
        when(remoteOAuth2AppsClient.getOAuth2Apps()).thenReturn(Map.of());

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters("mcpClient", Map.of("url", LINEAR_URL));

        assertThat(parameters).containsOnlyKeys("url");
    }

    @Test
    void testMcpClientUrlWithoutCatalogEntryIsIgnored() {
        when(remoteMcpServerCatalogClient.getMcpServerCatalog())
            .thenReturn(List.of(linearEntry(false, LINEAR_AUTHORIZATION_URL, LINEAR_TOKEN_URL, "linear")));
        when(remoteOAuth2AppsClient.getOAuth2Apps())
            .thenReturn(Map.of("linear", new OAuthApp("linearId", "linearSecret")));

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters(
            "mcpClient", Map.of("url", "https://mcp.example.com/mcp"));

        assertThat(parameters).containsOnlyKeys("url");
    }

    @Test
    void testProvidedMcpClientIdKeepsTheUserEndpoints() {
        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        Map<String, ?> parameters = oAuth2Service.checkPredefinedParameters(
            "mcpClient",
            Map.of(
                "url", LINEAR_URL, Authorization.CLIENT_ID, "mine", Authorization.TOKEN_URL,
                "https://auth.example.com/token"));

        assertThat(parameters.get(Authorization.CLIENT_ID)).isEqualTo("mine");
        assertThat(parameters.get(Authorization.TOKEN_URL)).isEqualTo("https://auth.example.com/token");

        verify(remoteMcpServerCatalogClient, never()).getMcpServerCatalog();
    }

    @Test
    void testOtherComponentsDoNotReadTheCatalog() {
        when(remoteOAuth2AppsClient.getOAuth2Apps()).thenReturn(Map.of());

        OAuth2ServiceImpl oAuth2Service = newOAuth2Service();

        oAuth2Service.checkPredefinedParameters("slack", Map.of("url", LINEAR_URL));

        verify(remoteMcpServerCatalogClient, never()).getMcpServerCatalog();
    }

    private OAuth2ServiceImpl newOAuth2Service() {
        return new OAuth2ServiceImpl(applicationProperties, remoteMcpServerCatalogClient, remoteOAuth2AppsClient);
    }

    private static McpServerCatalogEntry linearEntry(
        boolean dynamicClientRegistration, String authorizationUrl, String tokenUrl, String component) {

        return new McpServerCatalogEntry(
            "linear", "Linear", null, LINEAR_URL, null, "httpStreamable",
            new McpServerCatalogAuthentication(
                "oauth2", dynamicClientRegistration, null, authorizationUrl, tokenUrl, false, null, null),
            null, component);
    }

    private static OAuth2App app(String clientId, String clientSecret) {
        OAuth2App oAuth2App = new OAuth2App();

        oAuth2App.setClientId(clientId);
        oAuth2App.setClientSecret(clientSecret);

        return oAuth2App;
    }
}
