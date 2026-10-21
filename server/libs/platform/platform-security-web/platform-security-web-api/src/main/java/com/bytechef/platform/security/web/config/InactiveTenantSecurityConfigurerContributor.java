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

import com.bytechef.platform.security.web.filter.InactiveTenantFilter;
import com.bytechef.tenant.service.TenantDeletionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.stereotype.Component;

/**
 * Registers {@link InactiveTenantFilter} right after {@link AuthorizationFilter}, so the API key and MCP JWT filters
 * have already bound the request to its tenant.
 *
 * @author Ivica Cardic
 */
@Component
public class InactiveTenantSecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;

    public InactiveTenantSecurityConfigurerContributor(
        ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider) {

        this.tenantDeletionServiceProvider = tenantDeletionServiceProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>>
        T getSecurityConfigurerAdapter() {

        return (T) new InactiveTenantConfigurer(tenantDeletionServiceProvider);
    }

    static class InactiveTenantConfigurer extends AbstractHttpConfigurer<InactiveTenantConfigurer, HttpSecurity> {

        private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;

        InactiveTenantConfigurer(ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider) {
            this.tenantDeletionServiceProvider = tenantDeletionServiceProvider;
        }

        @Override
        public void init(HttpSecurity http) {
            http.addFilterAfter(new InactiveTenantFilter(tenantDeletionServiceProvider), AuthorizationFilter.class);
        }
    }
}
