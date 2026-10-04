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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties("bytechef.oauth2.resource-server")
@SuppressFBWarnings({
    "EI", "EI2"
})
public class McpResourceServerProperties implements InitializingBean {

    private List<Issuer> issuers = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        for (int index = 0; index < issuers.size(); index++) {
            Issuer issuer = issuers.get(index);

            if (StringUtils.isBlank(issuer.getUri())) {
                throw new IllegalStateException(
                    "bytechef.oauth2.resource-server.issuers[" + index + "].uri must not be blank");
            }

            if (issuer.isSelf() && StringUtils.isNotBlank(issuer.getAudience())) {
                throw new IllegalStateException(
                    "bytechef.oauth2.resource-server.issuers[" + index + "] must not set audience when self is true");
            }
        }
    }

    public List<Issuer> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<Issuer> issuers) {
        this.issuers = issuers;
    }

    public Optional<Issuer> findIssuer(String uri) {
        return issuers.stream()
            .filter(issuer -> uri.equals(issuer.getUri()))
            .findFirst();
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public static class Issuer {

        private @Nullable String uri;
        private @Nullable String tenantClaim;
        private @Nullable String authoritiesClaim;
        private List<String> authorities = new ArrayList<>();
        private boolean self;
        private @Nullable String audience;

        @Nullable
        public String getUri() {
            return uri;
        }

        @Nullable
        public String getTenantClaim() {
            return tenantClaim;
        }

        @Nullable
        public String getAuthoritiesClaim() {
            return authoritiesClaim;
        }

        public List<String> getAuthorities() {
            return authorities;
        }

        public boolean isSelf() {
            return self;
        }

        @Nullable
        public String getAudience() {
            return audience;
        }

        public void setUri(@Nullable String uri) {
            this.uri = uri;
        }

        public void setTenantClaim(@Nullable String tenantClaim) {
            this.tenantClaim = tenantClaim;
        }

        public void setAuthoritiesClaim(@Nullable String authoritiesClaim) {
            this.authoritiesClaim = authoritiesClaim;
        }

        public void setAuthorities(List<String> authorities) {
            this.authorities = authorities;
        }

        public void setSelf(boolean self) {
            this.self = self;
        }

        public void setAudience(@Nullable String audience) {
            this.audience = audience;
        }
    }
}
