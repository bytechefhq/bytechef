/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeySecurityConfigurerTest {

    @Test
    void testConfigureRegistersOnlyTheApiKeyAuthenticationFilter() {
        HttpSecurity httpSecurity = mock(HttpSecurity.class);

        when(httpSecurity.getSharedObject(AuthenticationManager.class)).thenReturn(mock(AuthenticationManager.class));

        EmbeddedApiKeySecurityConfigurer embeddedApiKeySecurityConfigurer = new EmbeddedApiKeySecurityConfigurer(
            mock(ApiKeyService.class), mock(ConnectedUserService.class), mock(JwtTokenService.class),
            mock(SigningKeyService.class));

        embeddedApiKeySecurityConfigurer.configure(httpSecurity);

        ArgumentCaptor<Filter> filterArgumentCaptor = ArgumentCaptor.forClass(Filter.class);

        verify(httpSecurity).getSharedObject(AuthenticationManager.class);
        verify(httpSecurity).addFilterBefore(filterArgumentCaptor.capture(), eq(BasicAuthenticationFilter.class));
        verifyNoMoreInteractions(httpSecurity);

        assertThat(filterArgumentCaptor.getValue()).isInstanceOf(ApiKeyAuthenticationFilter.class);
    }
}
