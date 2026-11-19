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

package com.bytechef.platform.githubproxy.client.internal;

import com.bytechef.platform.githubproxy.client.OAuthAppProxyClient;
import com.bytechef.platform.githubproxy.client.model.OAuthApp;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
public final class RestOAuthAppProxyClient implements OAuthAppProxyClient {

    private final RestClient restClient;

    public RestOAuthAppProxyClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    @Override
    public Map<String, OAuthApp> getOAuthApps() {
        ResponseEntity<Map<String, OAuthApp>> responseEntity = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/oauth-apps")
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::is3xxRedirection, (request, response) -> {
                throw new RestClientResponseException(
                    "The OAuth apps proxy answered with a redirect", response.getStatusCode(), "", null, null, null);
            })
            .toEntity(new ParameterizedTypeReference<Map<String, OAuthApp>>() {});

        Map<String, OAuthApp> oAuthApps = responseEntity.getBody();

        if (oAuthApps == null) {
            throw new RestClientResponseException(
                "The OAuth apps proxy answered without a body", responseEntity.getStatusCode(), "", null, null, null);
        }

        return oAuthApps;
    }
}
