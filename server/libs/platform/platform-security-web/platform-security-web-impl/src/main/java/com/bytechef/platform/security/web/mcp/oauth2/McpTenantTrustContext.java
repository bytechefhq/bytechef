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

package com.bytechef.platform.security.web.mcp.oauth2;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class McpTenantTrustContext {

    private static final ThreadLocal<Map<String, McpTenantIssuer>> ISSUERS_BY_URI = new ThreadLocal<>();

    private McpTenantTrustContext() {
    }

    public static void set(List<McpTenantIssuer> tenantIssuers) {
        ISSUERS_BY_URI.set(
            tenantIssuers.stream()
                .collect(Collectors.toMap(McpTenantIssuer::issuerUri, Function.identity(), (first, second) -> first)));
    }

    @Nullable
    public static McpTenantIssuer getIssuer(String issuerUri) {
        Map<String, McpTenantIssuer> issuersByUri = ISSUERS_BY_URI.get();

        return issuersByUri == null ? null : issuersByUri.get(issuerUri);
    }

    public static Set<String> getIssuerUris() {
        Map<String, McpTenantIssuer> issuersByUri = ISSUERS_BY_URI.get();

        return issuersByUri == null ? Set.of() : issuersByUri.keySet();
    }

    public static void clear() {
        ISSUERS_BY_URI.remove();
    }
}
