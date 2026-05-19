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

package com.bytechef.platform.credential.store;

import java.util.Map;

/**
 * Strategy for persisting and resolving the credential payload of a {@link CredentialSecret}. The DATABASE-backed store
 * is always registered; operators may additionally register one external store. Read-only implementations throw
 * {@link UnsupportedOperationException} from {@link #storeSecret} / {@link #deleteSecret}; callers gate on
 * {@link #isReadOnly()} first.
 *
 * @author Ivica Cardic
 */
public interface CredentialStore {

    CredentialStoreType getType();

    boolean isReadOnly();

    Map<String, ?> getSecret(CredentialSecret secret);

    /**
     * Persist the payload. Called BEFORE the row is saved, so the implementation may mutate the entity (set
     * {@code credentialRef}, clear the inline payload).
     */
    void storeSecret(CredentialSecret secret, Map<String, ?> payload);

    /** Remove the payload. Called BEFORE the row is deleted. */
    void deleteSecret(CredentialSecret secret);
}
