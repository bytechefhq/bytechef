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

package com.bytechef.platform.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.authentication.PlatformApiKeyAuthenticationProvider;
import com.bytechef.platform.security.web.authentication.PlatformApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.Objects;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
public class PlatformApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    protected static final String PATH_PATTERN = "^/api/platform/v[0-9]+/.+";

    public PlatformApiKeySecurityConfigurer(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        super(
            PATH_PATTERN, new PlatformApiKeyAuthenticationConverter(),
            new PlatformApiKeyAuthenticationProvider(apiKeyService, authorityService, userService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher(PATH_PATTERN));
        // For CORS requests
        csrf.ignoringRequestMatchers(request -> Objects.equals(request.getMethod(), "OPTIONS"));
    }

    private static class PlatformApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

        @Override
        protected Authentication doConvert(int environment, String authToken, String tenantId) {
            return new PlatformApiKeyAuthenticationToken(environment, authToken, tenantId);
        }
    }
}
