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
 * A registered OAuth2 client as shown in the admin listing.
 *
 * @author Ivica Cardic
 */
public record RegisteredClientInfo(
    String id, String clientId, String clientName, @Nullable Instant clientIdIssuedAt, List<String> scopes,
    List<String> authorizationGrantTypes, List<String> redirectUris, int grantCount, boolean deletable) {

    public RegisteredClientInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(clientId, "clientId");

        if (grantCount < 0) {
            throw new IllegalArgumentException("grantCount must not be negative");
        }

        scopes = List.copyOf(scopes);
        authorizationGrantTypes = List.copyOf(authorizationGrantTypes);
        redirectUris = List.copyOf(redirectUris);
    }
}
