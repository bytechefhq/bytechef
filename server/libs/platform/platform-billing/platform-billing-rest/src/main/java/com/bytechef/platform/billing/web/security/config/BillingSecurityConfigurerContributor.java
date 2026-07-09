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

import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class BillingSecurityConfigurerContributor implements SecurityConfigurerContributor {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends org.springframework.security.config.annotation.web.HttpSecurityBuilder<B>>
        T getSecurityConfigurerAdapter() {
        return (T) new BillingWebhookCsrfConfigurer();
    }

    private static class BillingWebhookCsrfConfigurer
        extends AbstractHttpConfigurer<BillingWebhookCsrfConfigurer, HttpSecurity> {

        @Override
        public void init(HttpSecurity http) {
            CsrfConfigurer<?> csrf = http.getConfigurer(CsrfConfigurer.class);

            if (csrf != null) {
                csrf.ignoringRequestMatchers("/api/platform/billing/webhook");
            }
        }
    }
}
