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

package com.bytechef.platform.credential.store.exception;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.credential.store.CredentialStoreType;

/**
 * Thrown when a write is attempted against a {@link CredentialStoreType} configured read-only.
 *
 * @author Ivica Cardic
 */
public class ReadOnlyCredentialStoreException extends ConfigurationException {

    public ReadOnlyCredentialStoreException(CredentialStoreType storeType) {
        super(
            "Credential store %s is configured read-only — writes are not permitted".formatted(storeType),
            CredentialStoreErrorType.READ_ONLY_CREDENTIAL_STORE);
    }
}
