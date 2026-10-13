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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OAuth2DiscoveryClient {

    private static final String INITIALIZE_REQUEST = """
        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},
         "clientInfo":{"name":"ByteChef","version":"1.0"}}}
        """;
    private static final Pattern RESOURCE_METADATA_PATTERN = Pattern.compile("resource_metadata=\"([^\"]+)\"");
    private static final String PROTECTED_RESOURCE_WELL_KNOWN = "/.well-known/oauth-protected-resource";
    private static final String AUTHORIZATION_SERVER_WELL_KNOWN = "/.well-known/oauth-authorization-server";
    private static final String OPENID_WELL_KNOWN = "/.well-known/openid-configuration";

    private final OAuth2HttpClient oAuth2HttpClient;

    public OAuth2DiscoveryClient(OAuth2HttpClient oAuth2HttpClient) {
        this.oAuth2HttpClient = oAuth2HttpClient;
    }

    public DiscoveredAuthorizationServer discover(String protectedResourceUrl) {
        oAuth2HttpClient.validateUrl(protectedResourceUrl);

        URI resourceUri = URI.create(protectedResourceUrl);
        Map<String, Object> protectedResourceMetadata = fetchProtectedResourceMetadata(resourceUri);

        String resource = requireString(protectedResourceMetadata, "resource");

        if (!originOf(URI.create(resource)).equals(originOf(resourceUri))) {
            throw new ConfigurationException(
                "Protected resource metadata advertises a resource from another origin: " + resource,
                OAuth2ErrorType.INVALID_METADATA);
        }

        String issuer = stripTrailingSlash(
            firstString(protectedResourceMetadata, "authorization_servers").orElse(originOf(resourceUri)));
        Map<String, Object> authorizationServerMetadata = fetchAuthorizationServerMetadata(issuer);

        String advertisedIssuer = stripTrailingSlash(requireString(authorizationServerMetadata, "issuer"));

        if (!advertisedIssuer.equals(issuer)) {
            throw new ConfigurationException(
                "Authorization server metadata issuer %s does not match %s".formatted(advertisedIssuer, issuer),
                OAuth2ErrorType.INVALID_METADATA);
        }

        List<String> codeChallengeMethods = strings(authorizationServerMetadata, "code_challenge_methods_supported");

        if (!codeChallengeMethods.isEmpty() && !codeChallengeMethods.contains("S256")) {
            throw new ConfigurationException(
                "Authorization server does not support PKCE S256", OAuth2ErrorType.INVALID_METADATA);
        }

        String authorizationEndpoint = requireString(authorizationServerMetadata, "authorization_endpoint");
        String tokenEndpoint = requireString(authorizationServerMetadata, "token_endpoint");
        String registrationEndpoint = optionalString(authorizationServerMetadata, "registration_endpoint");

        oAuth2HttpClient.validateUrl(authorizationEndpoint);
        oAuth2HttpClient.validateUrl(tokenEndpoint);

        if (registrationEndpoint != null) {
            oAuth2HttpClient.validateUrl(registrationEndpoint);
        }

        List<String> scopes = strings(protectedResourceMetadata, "scopes_supported");

        if (scopes.isEmpty()) {
            scopes = strings(authorizationServerMetadata, "scopes_supported");
        }

        return new DiscoveredAuthorizationServer(
            issuer, authorizationEndpoint, tokenEndpoint, registrationEndpoint, resource, scopes,
            codeChallengeMethods, strings(authorizationServerMetadata, "token_endpoint_auth_methods_supported"));
    }

    private Map<String, Object> fetchProtectedResourceMetadata(URI resourceUri) {
        OAuth2HttpClient.Response probeResponse = oAuth2HttpClient.probe(resourceUri.toString(), INITIALIZE_REQUEST);

        List<String> candidateUrls = new ArrayList<>();

        if (probeResponse.statusCode() == 401 || probeResponse.statusCode() == 403) {
            probeResponse.headers()
                .firstValue("WWW-Authenticate")
                .map(RESOURCE_METADATA_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .ifPresent(candidateUrls::add);
        }

        String origin = originOf(resourceUri);
        String path = stripTrailingSlash(resourceUri.getRawPath() == null ? "" : resourceUri.getRawPath());

        if (!path.isEmpty()) {
            candidateUrls.add(origin + PROTECTED_RESOURCE_WELL_KNOWN + path);
        }

        candidateUrls.add(origin + PROTECTED_RESOURCE_WELL_KNOWN);

        return fetchFirstJson(candidateUrls)
            .orElseThrow(() -> new ConfigurationException(
                "This server doesn't advertise OAuth; try Bearer Token or no authentication",
                OAuth2ErrorType.METADATA_NOT_FOUND));
    }

    private Map<String, Object> fetchAuthorizationServerMetadata(String issuer) {
        URI issuerUri = URI.create(issuer);
        String origin = originOf(issuerUri);
        String path = stripTrailingSlash(issuerUri.getRawPath() == null ? "" : issuerUri.getRawPath());

        List<String> candidateUrls = path.isEmpty()
            ? List.of(origin + AUTHORIZATION_SERVER_WELL_KNOWN, origin + OPENID_WELL_KNOWN)
            : List.of(
                origin + AUTHORIZATION_SERVER_WELL_KNOWN + path, origin + OPENID_WELL_KNOWN + path,
                issuer + OPENID_WELL_KNOWN);

        return fetchFirstJson(candidateUrls)
            .orElseThrow(() -> new ConfigurationException(
                "Authorization server metadata not found for " + issuer, OAuth2ErrorType.METADATA_NOT_FOUND));
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> fetchFirstJson(List<String> urls) {
        for (String url : urls) {
            OAuth2HttpClient.Response response = oAuth2HttpClient.get(url);

            if (response.statusCode() == 200) {
                try {
                    return Optional.of(JsonUtils.read(response.body(), Map.class));
                } catch (RuntimeException runtimeException) {
                    throw new ConfigurationException(
                        "Metadata at " + url + " is not valid JSON", runtimeException,
                        OAuth2ErrorType.INVALID_METADATA);
                }
            }
        }

        return Optional.empty();
    }

    private static String requireString(Map<String, Object> metadata, String key) {
        String value = optionalString(metadata, key);

        if (value == null) {
            throw new ConfigurationException(
                "Metadata is missing required field " + key, OAuth2ErrorType.INVALID_METADATA);
        }

        return value;
    }

    private static String optionalString(Map<String, Object> metadata, String key) {
        return metadata.get(key) instanceof String string && !string.isBlank() ? string : null;
    }

    private static Optional<String> firstString(Map<String, Object> metadata, String key) {
        return strings(metadata, key).stream()
            .findFirst();
    }

    private static List<String> strings(Map<String, Object> metadata, String key) {
        if (metadata.get(key) instanceof List<?> list) {
            return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        }

        return List.of();
    }

    private static String originOf(URI uri) {
        String origin = uri.getScheme() + "://" + uri.getHost();

        return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
