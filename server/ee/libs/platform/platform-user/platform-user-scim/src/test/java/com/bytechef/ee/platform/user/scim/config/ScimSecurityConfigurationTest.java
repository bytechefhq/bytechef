/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.security.web.config.AuthorizeHttpRequestContributor;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.tenant.service.TenantService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @version ee
 */
class ScimSecurityConfigurationTest {

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final ScimSecurityConfiguration scimSecurityConfiguration = new ScimSecurityConfiguration();
    private final TenantService tenantService = mock(TenantService.class);

    @Test
    void testAuthorizeHttpRequestContributorPermitsScimApiPaths() {
        AuthorizeHttpRequestContributor authorizeHttpRequestContributor =
            scimSecurityConfiguration.scimAuthorizeHttpRequestContributor();

        assertThat(authorizeHttpRequestContributor.getApiPermitAllRequestMatcherPaths())
            .containsExactly("/api/scim/v2/**");
        assertThat(authorizeHttpRequestContributor.getPermitAllRequestMatcherPaths()).isEmpty();
        assertThat(authorizeHttpRequestContributor.getFrameablePermitAllRequestMatcherPaths()).isEmpty();
    }

    @Test
    void testSecurityConfigurerContributorAddsBearerTokenFilterBeforeBasicAuthentication() {
        SecurityConfigurerContributor securityConfigurerContributor =
            scimSecurityConfiguration.scimSecurityConfigurerContributor(identityProviderService, tenantService);

        ScimHttpConfigurer scimHttpConfigurer = securityConfigurerContributor.getSecurityConfigurerAdapter();

        HttpSecurity httpSecurity = mock(HttpSecurity.class);

        scimHttpConfigurer.configure(httpSecurity);

        ArgumentCaptor<Filter> filterArgumentCaptor = ArgumentCaptor.forClass(Filter.class);

        verify(httpSecurity).addFilterBefore(filterArgumentCaptor.capture(), eq(BasicAuthenticationFilter.class));

        assertThat(filterArgumentCaptor.getValue()).isInstanceOf(ScimBearerTokenAuthenticationFilter.class);

        verifyNoInteractions(identityProviderService, tenantService);
    }

    @Test
    void testSecurityConfigurerContributorReusesSingleFilterInstance() {
        SecurityConfigurerContributor securityConfigurerContributor =
            scimSecurityConfiguration.scimSecurityConfigurerContributor(identityProviderService, tenantService);

        ScimHttpConfigurer firstScimHttpConfigurer = securityConfigurerContributor.getSecurityConfigurerAdapter();
        ScimHttpConfigurer secondScimHttpConfigurer = securityConfigurerContributor.getSecurityConfigurerAdapter();

        HttpSecurity firstHttpSecurity = mock(HttpSecurity.class);
        HttpSecurity secondHttpSecurity = mock(HttpSecurity.class);

        firstScimHttpConfigurer.configure(firstHttpSecurity);
        secondScimHttpConfigurer.configure(secondHttpSecurity);

        ArgumentCaptor<Filter> firstFilterArgumentCaptor = ArgumentCaptor.forClass(Filter.class);
        ArgumentCaptor<Filter> secondFilterArgumentCaptor = ArgumentCaptor.forClass(Filter.class);

        verify(firstHttpSecurity).addFilterBefore(
            firstFilterArgumentCaptor.capture(), eq(BasicAuthenticationFilter.class));
        verify(secondHttpSecurity).addFilterBefore(
            secondFilterArgumentCaptor.capture(), eq(BasicAuthenticationFilter.class));

        assertThat(firstScimHttpConfigurer).isNotSameAs(secondScimHttpConfigurer);
        assertThat(firstFilterArgumentCaptor.getValue()).isSameAs(secondFilterArgumentCaptor.getValue());
    }
}
