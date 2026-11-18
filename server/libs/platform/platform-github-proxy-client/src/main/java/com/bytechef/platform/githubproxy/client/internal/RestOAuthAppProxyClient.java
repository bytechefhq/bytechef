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
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

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
        Map<String, OAuthApp> oAuthApps = restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/oauth-apps")
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, OAuthApp>>() {});

        if (oAuthApps == null) {
            return Map.of();
        }

        return oAuthApps;
    }
}
