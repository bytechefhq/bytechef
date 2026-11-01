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

package com.bytechef.config.actuate.info;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Oauth2;
import com.bytechef.config.ApplicationProperties.Oauth2.ResourceServer.Issuer;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class Oauth2AuthorizationServerContributor implements InfoContributor {

    private final boolean enabled;

    public Oauth2AuthorizationServerContributor(ApplicationProperties applicationProperties) {
        Oauth2 oauth2 = applicationProperties.getOauth2();

        Oauth2.AuthorizationServer authorizationServer = oauth2.getAuthorizationServer();
        Oauth2.ResourceServer resourceServer = oauth2.getResourceServer();

        List<Issuer> issuers = resourceServer.getIssuers();

        this.enabled = authorizationServer.isEnabled() && issuers.stream()
            .anyMatch(Issuer::isSelf);
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("oauth2", Map.of("authorizationServer", Map.of("enabled", String.valueOf(enabled))));
    }
}
