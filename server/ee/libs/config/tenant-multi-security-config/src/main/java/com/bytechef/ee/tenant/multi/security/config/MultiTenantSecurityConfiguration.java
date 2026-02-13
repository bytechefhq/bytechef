/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.config;

import com.bytechef.ee.tenant.multi.security.MultiTenantUserDetailsService;
import com.bytechef.ee.tenant.multi.security.saml2.DynamicRelyingPartyRegistrationRepository;
import com.bytechef.ee.tenant.multi.security.saml2.MultiTenantSaml2AuthenticationFailureHandler;
import com.bytechef.ee.tenant.multi.security.saml2.MultiTenantSaml2AuthenticationSuccessHandler;
import com.bytechef.ee.tenant.multi.security.web.authentication.MultiTenantAuthenticationFailureHandler;
import com.bytechef.ee.tenant.multi.security.web.authentication.MultiTenantAuthenticationSuccessHandler;
import com.bytechef.ee.tenant.multi.security.web.authentication.MultiTenantOAuth2AuthenticationFailureHandler;
import com.bytechef.ee.tenant.multi.security.web.authentication.MultiTenantOAuth2AuthenticationSuccessHandler;
import com.bytechef.ee.tenant.multi.security.web.filter.MultiTenantInternalFilter;
import com.bytechef.ee.tenant.multi.security.web.filter.SsoEnforcementFilter;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.OAuth2LoginCustomizer;
import com.bytechef.platform.security.web.config.Saml2LoginCustomizer;
import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.oauth2.CustomOAuth2UserService;
import com.bytechef.security.web.oauth2.CustomOidcUserService;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
public class MultiTenantSecurityConfiguration {

    @Bean
    MultiTenantAuthenticationFailureHandler multiTenantAuthenticationFailureHandler() {
        return new MultiTenantAuthenticationFailureHandler();
    }

    @Bean
    MultiTenantAuthenticationSuccessHandler multiTenantAuthenticationSuccessHandler(
        TenantService tenantService,
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider) {

        return new MultiTenantAuthenticationSuccessHandler(tenantService, twoFactorAuthenticationCustomizerProvider);
    }

    @Bean
    MultiTenantInternalFilter multiTenantInternalFilter() {
        return new MultiTenantInternalFilter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
    SsoEnforcementFilter ssoEnforcementFilter(IdentityProviderService identityProviderService) {
        return new SsoEnforcementFilter(identityProviderService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
    OAuth2LoginCustomizer multiTenantOAuth2LoginCustomizer(
        CustomOAuth2UserService customOAuth2UserService, CustomOidcUserService customOidcUserService,
        RememberMeServices rememberMeServices, TenantService tenantService) {

        return http -> http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(endpoint -> endpoint
                .userService(customOAuth2UserService)
                .oidcUserService(customOidcUserService))
            .successHandler(new MultiTenantOAuth2AuthenticationSuccessHandler(rememberMeServices, tenantService))
            .failureHandler(new MultiTenantOAuth2AuthenticationFailureHandler()));
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
    AuthorizeHttpRequestContributor saml2MetadataAuthorizeHttpRequestContributor() {
        return new AuthorizeHttpRequestContributor() {

            @Override
            public List<String> getApiPermitAllRequestMatcherPaths() {
                return List.of("/saml2/metadata/**");
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
    Saml2LoginCustomizer multiTenantSaml2LoginCustomizer(
        DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository,
        RememberMeServices rememberMeServices, TenantService tenantService, UserService userService) {

        return http -> http.saml2Login(saml2 -> saml2
            .relyingPartyRegistrationRepository(dynamicRelyingPartyRegistrationRepository)
            .successHandler(
                new MultiTenantSaml2AuthenticationSuccessHandler(rememberMeServices, tenantService, userService))
            .failureHandler(new MultiTenantSaml2AuthenticationFailureHandler()));
    }

    @Bean("userDetailsService")
    MultiTenantUserDetailsService multiTenantUserDetailsService(
        AuthorityService authorityService, TenantService tenantService) {

        return new MultiTenantUserDetailsService(authorityService, tenantService);
    }
}
