/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.security.web.matcher;

import com.bytechef.platform.security.web.matcher.AuthenticatedRequestMatcherContributor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class EmbeddedAuthenticatedRequestMatcherContributor implements AuthenticatedRequestMatcherContributor {

    @Override
    public void requestMatchers(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz,
        MvcRequestMatcher.Builder mvc) {

        authz.requestMatchers(mvc.pattern("/api/embedded/v1/**"))
            .authenticated()
            .requestMatchers(mvc.pattern("/api/embedded/by-connected-user-token/v1/**"))
            .authenticated();
    }
}
