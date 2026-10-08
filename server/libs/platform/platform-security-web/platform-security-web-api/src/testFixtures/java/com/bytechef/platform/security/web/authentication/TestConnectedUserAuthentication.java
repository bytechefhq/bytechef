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

package com.bytechef.platform.security.web.authentication;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * @author Ivica Cardic
 */
public final class TestConnectedUserAuthentication extends AbstractAuthenticationToken
    implements ConnectedUserAuthentication {

    private final boolean apiKeyAuthenticated;
    private final long connectedUserId;
    private final long environmentId;
    private final String externalUserId;

    public TestConnectedUserAuthentication(
        String externalUserId, long connectedUserId, long environmentId, boolean authenticated) {

        this(externalUserId, connectedUserId, environmentId, authenticated, false);
    }

    private TestConnectedUserAuthentication(
        String externalUserId, long connectedUserId, long environmentId, boolean authenticated,
        boolean apiKeyAuthenticated) {

        super(List.of());

        this.apiKeyAuthenticated = apiKeyAuthenticated;
        this.connectedUserId = connectedUserId;
        this.environmentId = environmentId;
        this.externalUserId = externalUserId;

        setAuthenticated(authenticated);
    }

    public static TestConnectedUserAuthentication apiKey(String externalUserId) {
        return new TestConnectedUserAuthentication(externalUserId, 1L, 2L, true, true);
    }

    public static TestConnectedUserAuthentication of(String externalUserId) {
        return new TestConnectedUserAuthentication(externalUserId, 1L, 2L, true);
    }

    @Override
    public boolean apiKeyAuthenticated() {
        return apiKeyAuthenticated;
    }

    @Override
    public long connectedUserId() {
        return connectedUserId;
    }

    @Override
    public long environmentId() {
        return environmentId;
    }

    @Override
    public String externalUserId() {
        return externalUserId;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return externalUserId;
    }
}
