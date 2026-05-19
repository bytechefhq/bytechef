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

package com.bytechef.platform.connection.service;

import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only {@link CredentialStore} that imitates an external secret store entirely in memory. Used by
 * {@code ConnectionServiceIntTest} to verify multi-store dispatch without standing up a vault. Maps the entity's
 * {@code credentialRef} to a parameters payload; generates a fresh UUID on first store.
 *
 * <p>
 * Registered as type {@link CredentialStoreType#HASHICORP_VAULT} purely so it has a non-DATABASE value — the test
 * doesn't care which non-default discriminator the store reports.
 *
 * @author Ivica Cardic
 */
public class TestExternalConnectionCredentialStore implements CredentialStore {

    private final Map<String, Map<String, Object>> secrets = new ConcurrentHashMap<>();
    private boolean readOnly;

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public CredentialStoreType getType() {
        return CredentialStoreType.HASHICORP_VAULT;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public Map<String, ?> getSecret(CredentialSecret secret) {
        String ref = secret.getCredentialRef();

        if (ref == null) {
            return Map.of();
        }

        return secrets.getOrDefault(ref, Map.of());
    }

    @Override
    public void storeSecret(CredentialSecret secret, Map<String, ?> payload) {
        String ref = secret.getCredentialRef();

        if (ref == null) {
            ref = UUID.randomUUID()
                .toString();

            secret.setCredentialRef(ref);
        }

        secrets.put(ref, new HashMap<>(payload));

        secret.setPayload(Map.of());
    }

    @Override
    public void deleteSecret(CredentialSecret secret) {
        String ref = secret.getCredentialRef();

        if (ref != null) {
            secrets.remove(ref);
        }
    }
}
