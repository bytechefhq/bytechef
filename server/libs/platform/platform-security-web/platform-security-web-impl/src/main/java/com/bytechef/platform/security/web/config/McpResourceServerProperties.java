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
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The trusted JWT issuers accepted on the MCP endpoints. Mirrors {@code ApplicationProperties.Oauth2.ResourceServer}
 * (which exists so strict property binding does not fail); this EE-local binding is the functional reader used by the
 * MCP resource-server wiring, and is only consulted in EE. When no issuer is configured, the resource server is dormant
 * and the MCP endpoints keep their API-key-only behavior.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties("bytechef.oauth2.resource-server")
@SuppressFBWarnings({
    "EI", "EI2"
})
public class McpResourceServerProperties {

    private List<Issuer> issuers = new ArrayList<>();

    public List<Issuer> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<Issuer> issuers) {
        this.issuers = issuers;
    }

    /**
     * Finds the configured mapping for the given issuer identifier, or empty if the issuer is not configured.
     */
    public Optional<Issuer> findIssuer(String uri) {
        return issuers.stream()
            .filter(issuer -> uri.equals(issuer.getUri()))
            .findFirst();
    }

    /**
     * A trusted JWT issuer and how its tokens map to a ByteChef tenant and authorities.
     */
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

        /**
         * Whether this issuer is the ByteChef embedded authorization server. Its tokens carry the requested MCP
         * endpoint URL as their {@code aud}, so audience validation always enforces that the token's {@code aud}
         * contains the current endpoint URL.
         */
        public boolean isSelf() {
            return self;
        }

        /**
         * For an external issuer, the fixed audience value its tokens must carry. When set, audience validation
         * requires the token's {@code aud} to contain this value; when unset, audience validation is skipped for this
         * issuer (so an IdP that does not emit an audience still works).
         */
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
