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
import org.jspecify.annotations.Nullable;

/**
 * Entity-facing seam implemented by every credential-bearing entity ({@code Connection}, {@code Property}). It is
 * exactly the surface a {@link CredentialStore} touches — the inline payload accessors plus the external-store
 * discriminator and reference.
 *
 * @author Ivica Cardic
 */
public interface CredentialSecret {

    @Nullable
    String getCredentialRef();

    void setCredentialRef(@Nullable String credentialRef);

    /** The inline (in-entity) decrypted payload. Read by the DATABASE-backed store. */
    Map<String, ?> getPayload();

    /** Set or clear the inline payload. External stores call this with {@code Map.of()} to clear. */
    void setPayload(Map<String, ?> payload);

    CredentialStoreType getCredentialStoreType();

    void setCredentialStoreType(CredentialStoreType credentialStoreType);
}
