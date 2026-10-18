/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.config;

import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.SsoUserProvisioner;
import com.bytechef.ee.security.sso.oauth2.CustomOidcUserService;
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
import com.bytechef.platform.security.web.config.SpaWebFilterContributor;
import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.oauth2.CustomOAuth2UserService;
import com.bytechef.security.web.oauth2.SsoEnforcementChecker;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
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
        IdentityProviderLocator identityProviderLocator, RememberMeServices rememberMeServices,
        TenantService tenantService,
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider) {

        return http -> http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(endpoint -> endpoint
                .userService(customOAuth2UserService)
                .oidcUserService(customOidcUserService))
            .successHandler(
                new SsoOAuth2AuthenticationSuccessHandler(
                    identityProviderLocator, rememberMeServices, tenantService,
                    twoFactorAuthenticationCustomizerProvider.getIfAvailable()))
            .failureHandler(new SsoOAuth2AuthenticationFailureHandler()));
    }

    @Bean
    Saml2LoginCustomizer ssoSaml2LoginCustomizer(
        DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository,
        IdentityProviderLocator identityProviderLocator, RememberMeServices rememberMeServices,
        SsoUserProvisioner ssoUserProvisioner, TenantService tenantService,
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider) {

        return http -> http.saml2Login(saml2 -> saml2
            .relyingPartyRegistrationRepository(dynamicRelyingPartyRegistrationRepository)
            .successHandler(
                new SsoSaml2AuthenticationSuccessHandler(
                    identityProviderLocator, rememberMeServices, ssoUserProvisioner, tenantService,
                    twoFactorAuthenticationCustomizerProvider.getIfAvailable()))
            .failureHandler(new SsoSaml2AuthenticationFailureHandler()));
    }

    @Bean
    SsoEnforcementChecker ssoEnforcementChecker(IdentityProviderLocator identityProviderLocator) {
        return email -> {
            int atIndex = email.lastIndexOf('@');

            if (atIndex < 0) {
                return false;
            }

            return identityProviderLocator.findByDomain(email.substring(atIndex + 1))
                .map(locatedIdentityProvider -> locatedIdentityProvider.identityProvider()
                    .isEnforced())
                .orElse(false);
        };
    }

    @Bean
    SpaWebFilterContributor saml2SpaWebFilterContributor() {
        return () -> List.of("/saml2");
    }

    @Bean
    SecurityConfigurerContributor ssoEnforcementSecurityConfigurerContributor(
        IdentityProviderLocator identityProviderLocator) {

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>>
                T getSecurityConfigurerAdapter() {

                return (T) new SsoEnforcementHttpConfigurer(new SsoEnforcementFilter(identityProviderLocator));
            }
        };
    }
}
