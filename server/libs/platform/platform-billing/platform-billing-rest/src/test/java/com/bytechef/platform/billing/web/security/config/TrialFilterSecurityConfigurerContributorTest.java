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

package com.bytechef.platform.billing.web.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.service.TrialService;
import com.bytechef.platform.billing.web.filter.TrialFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @author Matija Petanjek
 */
class TrialFilterSecurityConfigurerContributorTest {

    private final TrialService trialService = mock(TrialService.class);

    private final TrialFilterSecurityConfigurerContributor contributor =
        new TrialFilterSecurityConfigurerContributor(trialService);

    @Test
    void testGetSecurityConfigurerAdapterRegistersTrialFilterAfterBasicAuthenticationFilter() throws Exception {
        HttpSecurity httpSecurity = mock(HttpSecurity.class);

        when(httpSecurity.addFilterAfter(any(Filter.class), eq(BasicAuthenticationFilter.class)))
            .thenReturn(httpSecurity);

        Object adapter = contributor.getSecurityConfigurerAdapter();

        assertThat(adapter).isInstanceOf(TrialFilterSecurityConfigurerContributor.TrialFilterConfigurer.class);

        ((TrialFilterSecurityConfigurerContributor.TrialFilterConfigurer) adapter).init(httpSecurity);

        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);

        verify(httpSecurity).addFilterAfter(filterCaptor.capture(), eq(BasicAuthenticationFilter.class));
        assertThat(filterCaptor.getValue()).isInstanceOf(TrialFilter.class);
    }
}
