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

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * What the consent page shows about the client asking for access: its self-declared, unverified name and the hosts of
 * its registered redirect URIs, which is where an approval sends the authorization code.
 *
 * @author Ivica Cardic
 */
public record OAuth2ConsentClient(String clientId, @Nullable String clientName, List<String> redirectHosts) {

    public OAuth2ConsentClient {
        Objects.requireNonNull(clientId, "clientId");

        redirectHosts = List.copyOf(redirectHosts);
    }
}
