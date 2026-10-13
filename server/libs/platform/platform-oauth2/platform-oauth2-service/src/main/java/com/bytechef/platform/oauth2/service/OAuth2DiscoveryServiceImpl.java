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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class OAuth2DiscoveryServiceImpl implements OAuth2DiscoveryService {

    private final OAuth2DiscoveryClient oAuth2DiscoveryClient;
    private final OAuth2DynamicClientRegistrar oAuth2DynamicClientRegistrar;
    private final String redirectUri;

    public OAuth2DiscoveryServiceImpl(
        ApplicationProperties applicationProperties, OAuth2DiscoveryClient oAuth2DiscoveryClient,
        OAuth2DynamicClientRegistrar oAuth2DynamicClientRegistrar) {

        this.oAuth2DiscoveryClient = oAuth2DiscoveryClient;
        this.oAuth2DynamicClientRegistrar = oAuth2DynamicClientRegistrar;
        this.redirectUri = applicationProperties.getOauth2()
            .getRedirectUri();
    }

    @Override
    public DiscoveredAuthorizationServer discover(String protectedResourceUrl) {
        return oAuth2DiscoveryClient.discover(protectedResourceUrl);
    }

    @Override
    public RegisteredClient register(DiscoveredAuthorizationServer authorizationServer) {
        return oAuth2DynamicClientRegistrar.register(authorizationServer, redirectUri);
    }
}
