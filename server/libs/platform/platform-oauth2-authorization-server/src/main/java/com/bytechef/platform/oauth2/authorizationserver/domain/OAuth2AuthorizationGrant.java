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

package com.bytechef.platform.oauth2.authorizationserver.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The active authorizations one user granted to a registered OAuth2 client, aggregated per principal. The MCP servers
 * are display names, or the raw resource URL when it names no known MCP server. The authorization times are absent when
 * none of the tokens records when it was issued.
 *
 * @author Ivica Cardic
 */
public record OAuth2AuthorizationGrant(
    String principalName, List<String> scopes, List<String> mcpServers, @Nullable Instant authorizedAt,
    @Nullable Instant lastIssuedAt, @Nullable Instant expiresAt) {

    public OAuth2AuthorizationGrant {
        Objects.requireNonNull(principalName, "principalName");

        if ((authorizedAt == null) != (lastIssuedAt == null)) {
            throw new IllegalArgumentException("authorizedAt and lastIssuedAt must both be present or both be absent");
        }

        if (authorizedAt != null && lastIssuedAt.isBefore(authorizedAt)) {
            throw new IllegalArgumentException("lastIssuedAt must not precede authorizedAt");
        }

        scopes = List.copyOf(scopes);
        mcpServers = List.copyOf(mcpServers);
    }
}
