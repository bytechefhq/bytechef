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

import static com.bytechef.component.definition.Authorization.TOKEN_ENDPOINT_AUTH_METHOD_BASIC;
import static com.bytechef.component.definition.Authorization.TOKEN_ENDPOINT_AUTH_METHOD_NONE;
import static com.bytechef.component.definition.Authorization.TOKEN_ENDPOINT_AUTH_METHOD_POST;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OAuth2DynamicClientRegistrar {

    private static final int MAX_ERROR_TEXT_LENGTH = 500;
    private static final List<String> PREFERRED_AUTH_METHODS = List.of(
        TOKEN_ENDPOINT_AUTH_METHOD_NONE, TOKEN_ENDPOINT_AUTH_METHOD_BASIC, TOKEN_ENDPOINT_AUTH_METHOD_POST);

    private final OAuth2HttpClient oAuth2HttpClient;

    public OAuth2DynamicClientRegistrar(OAuth2HttpClient oAuth2HttpClient) {
        this.oAuth2HttpClient = oAuth2HttpClient;
    }

    public RegisteredClient register(DiscoveredAuthorizationServer authorizationServer, String redirectUri) {
        String registrationEndpoint = authorizationServer.registrationEndpoint();

        if (registrationEndpoint == null) {
            throw new ConfigurationException(
                "This server doesn't allow automatic registration; use OAuth2 (manual)",
                OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED);
        }

        String requestedAuthMethod = selectAuthMethod(authorizationServer.tokenEndpointAuthMethods());

        Map<String, Object> registrationRequest = new LinkedHashMap<>();

        registrationRequest.put("client_name", "ByteChef");
        registrationRequest.put("redirect_uris", List.of(redirectUri));
        registrationRequest.put("grant_types", List.of("authorization_code", "refresh_token"));
        registrationRequest.put("response_types", List.of("code"));
        registrationRequest.put("token_endpoint_auth_method", requestedAuthMethod);

        OAuth2HttpClient.Response response = oAuth2HttpClient.post(
            registrationEndpoint, "application/json", JsonUtils.write(registrationRequest), Map.of());

        Map<String, Object> responseBody = readBody(response.body());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            Object errorText = responseBody.get("error_description");

            if (errorText == null) {
                errorText = responseBody.get("error");
            }

            if (errorText == null) {
                errorText = response.body();
            }

            throw new ConfigurationException(
                "Registration rejected: " + truncate(String.valueOf(errorText)),
                OAuth2ErrorType.REGISTRATION_REJECTED);
        }

        if (!(responseBody.get("client_id") instanceof String clientId) || clientId.isBlank()) {
            throw new ConfigurationException(
                "Registration response is missing client_id", OAuth2ErrorType.REGISTRATION_REJECTED);
        }

        String clientSecret = responseBody.get("client_secret") instanceof String secret && !secret.isBlank()
            ? secret : null;
        String authMethod =
            responseBody.get("token_endpoint_auth_method") instanceof String method && !method.isBlank()
                ? method : requestedAuthMethod;

        return new RegisteredClient(clientId, clientSecret, authMethod);
    }

    private static String selectAuthMethod(List<String> supportedAuthMethods) {
        if (supportedAuthMethods == null || supportedAuthMethods.isEmpty()) {
            return TOKEN_ENDPOINT_AUTH_METHOD_NONE;
        }

        return PREFERRED_AUTH_METHODS.stream()
            .filter(supportedAuthMethods::contains)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "Authorization server supports no usable token endpoint authentication method: "
                    + truncate(String.valueOf(supportedAuthMethods)),
                OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBody(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        try {
            Map<String, Object> result = JsonUtils.read(body, Map.class);

            return result == null ? Map.of() : result;
        } catch (RuntimeException runtimeException) {
            return Map.of();
        }
    }

    private static String truncate(String text) {
        return text.length() <= MAX_ERROR_TEXT_LENGTH ? text : text.substring(0, MAX_ERROR_TEXT_LENGTH);
    }
}
