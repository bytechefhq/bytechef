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

package com.bytechef.security.web.oauth2;

import com.bytechef.platform.security.web.config.OAuth2LoginCustomizer;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Component;

/**
 * Wires Google and GitHub login into the API filter chain in both tenant modes. When enterprise SSO is enabled its
 * customizer configures {@code oauth2Login} instead and routes the social registrations through the same user services,
 * so only one of the two ever owns the configuration.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnExpression("${bytechef.security.social-login.enabled:false} and !${bytechef.security.sso.enabled:false}")
public class SocialOAuth2LoginCustomizer implements OAuth2LoginCustomizer {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final SocialOidcUserService socialOidcUserService;
    private final RememberMeServices rememberMeServices;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public SocialOAuth2LoginCustomizer(
        CustomOAuth2UserService customOAuth2UserService, SocialOidcUserService socialOidcUserService,
        RememberMeServices rememberMeServices, TenantService tenantService) {

        this.customOAuth2UserService = customOAuth2UserService;
        this.socialOidcUserService = socialOidcUserService;
        this.rememberMeServices = rememberMeServices;
        this.tenantService = tenantService;
    }

    @Override
    public void customize(HttpSecurity http) throws Exception {
        http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(endpoint -> endpoint
                .userService(customOAuth2UserService)
                .oidcUserService(socialOidcUserService))
            .successHandler(new OAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService))
            .failureHandler(new OAuth2AuthenticationFailureHandler()));
    }
}
