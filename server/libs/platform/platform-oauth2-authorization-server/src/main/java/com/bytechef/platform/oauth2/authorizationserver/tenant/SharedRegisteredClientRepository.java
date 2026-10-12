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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Keeps registered OAuth2 clients in the shared default tenant regardless of the current tenant.
 *
 * @author Ivica Cardic
 */
public class SharedRegisteredClientRepository implements RegisteredClientRepository {

    private final RegisteredClientRepository delegate;

    @SuppressFBWarnings("EI2")
    public SharedRegisteredClientRepository(RegisteredClientRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        TenantScope.run(TenantContext.DEFAULT_TENANT_ID, () -> delegate.save(registeredClient));
    }

    @Override
    @Nullable
    public RegisteredClient findById(String id) {
        return TenantScope.call(TenantContext.DEFAULT_TENANT_ID, () -> delegate.findById(id));
    }

    @Override
    @Nullable
    public RegisteredClient findByClientId(String clientId) {
        return TenantScope.call(TenantContext.DEFAULT_TENANT_ID, () -> delegate.findByClientId(clientId));
    }
}
