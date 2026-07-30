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

import com.bytechef.platform.billing.service.TrialService;
import com.bytechef.platform.billing.web.filter.TrialFilter;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * Registers {@link TrialFilter} into the security filter chain, right after {@link BasicAuthenticationFilter} so it
 * runs only once a request is authenticated. Kept separate from {@link BillingSecurityConfigurerContributor} (which
 * stays unconditional) because {@link TrialFilter} itself must not run when billing is disabled.
 *
 * @author Matija Petanjek
 */
@Component
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class TrialFilterSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final TrialService trialService;

    public TrialFilterSecurityConfigurerContributor(TrialService trialService) {
        this.trialService = trialService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>>
        T getSecurityConfigurerAdapter() {
        return (T) new TrialFilterConfigurer(trialService);
    }

    static class TrialFilterConfigurer extends AbstractHttpConfigurer<TrialFilterConfigurer, HttpSecurity> {

        private final TrialService trialService;

        TrialFilterConfigurer(TrialService trialService) {
            this.trialService = trialService;
        }

        @Override
        public void init(HttpSecurity http) {
            http.addFilterAfter(new TrialFilter(trialService), BasicAuthenticationFilter.class);
        }
    }
}
