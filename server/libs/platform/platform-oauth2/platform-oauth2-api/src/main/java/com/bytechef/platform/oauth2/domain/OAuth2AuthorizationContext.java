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

package com.bytechef.platform.oauth2.domain;

import com.bytechef.component.definition.Authorization;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public record OAuth2AuthorizationContext(
    String componentName, int connectionVersion, String codeVerifier, String clientId, String clientSecret,
    String tokenEndpointAuthMethod, String authorizationUrl, String tokenUrl, String issuer, String resource) {

    public static OAuth2AuthorizationContext ofPkce(String componentName, int connectionVersion, String codeVerifier) {
        return new OAuth2AuthorizationContext(
            componentName, connectionVersion, codeVerifier, null, null, null, null, null, null, null);
    }

    public static OAuth2AuthorizationContext ofDiscovery(
        String componentName, int connectionVersion, String codeVerifier, RegisteredClient registeredClient,
        DiscoveredAuthorizationServer authorizationServer) {

        return new OAuth2AuthorizationContext(
            componentName, connectionVersion, codeVerifier, registeredClient.clientId(),
            registeredClient.clientSecret(), registeredClient.tokenEndpointAuthMethod(),
            authorizationServer.authorizationEndpoint(), authorizationServer.tokenEndpoint(),
            authorizationServer.issuer(), authorizationServer.resource());
    }

    public boolean isDiscovery() {
        return clientId != null;
    }

    public Map<String, Object> toConnectionParameters() {
        Map<String, Object> connectionParameters = new HashMap<>();

        putIfPresent(connectionParameters, Authorization.CLIENT_ID, clientId);
        putIfPresent(connectionParameters, Authorization.CLIENT_SECRET, clientSecret);
        putIfPresent(connectionParameters, Authorization.TOKEN_ENDPOINT_AUTH_METHOD, tokenEndpointAuthMethod);
        putIfPresent(connectionParameters, Authorization.AUTHORIZATION_URL, authorizationUrl);
        putIfPresent(connectionParameters, Authorization.TOKEN_URL, tokenUrl);
        putIfPresent(connectionParameters, Authorization.RESOURCE, resource);

        return connectionParameters;
    }

    @Override
    public String toString() {
        return "OAuth2AuthorizationContext[componentName=" + componentName + ", connectionVersion="
            + connectionVersion + ", codeVerifier=" + redact(codeVerifier) + ", clientId=" + clientId
            + ", clientSecret=" + redact(clientSecret) + ", tokenEndpointAuthMethod=" + tokenEndpointAuthMethod
            + ", authorizationUrl=" + authorizationUrl + ", tokenUrl=" + tokenUrl + ", issuer=" + issuer
            + ", resource=" + resource + "]";
    }

    private static String redact(String value) {
        return value == null ? null : "***";
    }

    private static void putIfPresent(Map<String, Object> connectionParameters, String key, String value) {
        if (value != null) {
            connectionParameters.put(key, value);
        }
    }
}
