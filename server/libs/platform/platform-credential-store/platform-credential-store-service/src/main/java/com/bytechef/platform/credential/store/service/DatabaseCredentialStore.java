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

package com.bytechef.platform.credential.store.service;

import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Default {@link CredentialStore} backed by the entity's inline encrypted payload column. Always registered.
 *
 * @author Ivica Cardic
 */
@Component
public class DatabaseCredentialStore implements CredentialStore {

    @Override
    public CredentialStoreType getType() {
        return CredentialStoreType.DATABASE;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Map<String, ?> getSecret(CredentialSecret secret) {
        return secret.getPayload();
    }

    @Override
    public void storeSecret(CredentialSecret secret, Map<String, ?> payload) {
        secret.setPayload(payload);
    }

    @Override
    public void deleteSecret(CredentialSecret secret) {
        // No-op: the inline payload is cleared when the row is deleted.
    }
}
