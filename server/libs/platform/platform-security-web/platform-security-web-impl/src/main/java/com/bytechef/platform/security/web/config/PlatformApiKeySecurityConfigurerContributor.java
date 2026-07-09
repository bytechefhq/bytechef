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

package com.bytechef.platform.security.web.config;

import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.PlatformApiKeySecurityConfigurer;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
public class PlatformApiKeySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public PlatformApiKeySecurityConfigurerContributor(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        return (T) new PlatformApiKeySecurityConfigurer(apiKeyService, authorityService, userService);
    }
}
