/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.config;

import com.bytechef.ee.security.sso.oauth2.SsoOAuth2AuthenticationFailureHandler;
import com.bytechef.ee.security.sso.oauth2.SsoOAuth2AuthenticationSuccessHandler;
import com.bytechef.ee.security.sso.saml2.DynamicRelyingPartyRegistrationRepository;
import com.bytechef.ee.security.sso.saml2.SsoSaml2AuthenticationFailureHandler;
import com.bytechef.ee.security.sso.saml2.SsoSaml2AuthenticationSuccessHandler;
import com.bytechef.ee.security.sso.web.configurer.SsoEnforcementHttpConfigurer;
import com.bytechef.ee.security.sso.web.filter.SsoEnforcementFilter;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.OAuth2LoginCustomizer;
import com.bytechef.platform.security.web.config.Saml2LoginCustomizer;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.oauth2.CustomOAuth2UserService;
import com.bytechef.security.web.oauth2.CustomOidcUserService;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * SSO security configuration that provides SAML2 and OIDC SSO beans. Active when EE version is enabled and
 * {@code bytechef.security.sso.enabled=true}. Works in both single-tenant and multi-tenant modes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
class SsoSecurityConfiguration {

    @Bean
    AuthorizeHttpRequestContributor saml2MetadataAuthorizeHttpRequestContributor() {
        return new AuthorizeHttpRequestContributor() {

            @Override
            public List<String> getApiPermitAllRequestMatcherPaths() {
                return List.of("/saml2/metadata/**");
            }
        };
    }

    @Bean
    OAuth2LoginCustomizer ssoOAuth2LoginCustomizer(
        CustomOAuth2UserService customOAuth2UserService, CustomOidcUserService customOidcUserService,
        RememberMeServices rememberMeServices, TenantService tenantService) {

        return http -> http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(endpoint -> endpoint
                .userService(customOAuth2UserService)
                .oidcUserService(customOidcUserService))
            .successHandler(new SsoOAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService))
            .failureHandler(new SsoOAuth2AuthenticationFailureHandler()));
    }

    @Bean
    Saml2LoginCustomizer ssoSaml2LoginCustomizer(
        DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository,
        IdentityProviderService identityProviderService, RememberMeServices rememberMeServices,
        TenantService tenantService, UserService userService) {

        return http -> http.saml2Login(saml2 -> saml2
            .relyingPartyRegistrationRepository(dynamicRelyingPartyRegistrationRepository)
            .successHandler(
                new SsoSaml2AuthenticationSuccessHandler(
                    identityProviderService, rememberMeServices, tenantService, userService))
            .failureHandler(new SsoSaml2AuthenticationFailureHandler()));
    }

    @Bean
    SecurityConfigurerContributor ssoEnforcementSecurityConfigurerContributor(
        IdentityProviderService identityProviderService) {

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>>
                T getSecurityConfigurerAdapter() {

                return (T) new SsoEnforcementHttpConfigurer(new SsoEnforcementFilter(identityProviderService));
            }
        };
    }
}
