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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseCredentialStoreTest {

    private final DatabaseCredentialStore databaseCredentialStore = new DatabaseCredentialStore();

    @Test
    void testGetTypeIsDatabase() {
        assertThat(databaseCredentialStore.getType()).isEqualTo(CredentialStoreType.DATABASE);
    }

    @Test
    void testIsReadOnlyFalse() {
        assertThat(databaseCredentialStore.isReadOnly()).isFalse();
    }

    @Test
    void testGetSecretReadsInlinePayload() {
        CredentialSecret secret = mock(CredentialSecret.class);
        Map<String, Object> payload = Map.of("apiKey", "abc");

        doReturn(payload).when(secret)
            .getPayload();

        assertThat(databaseCredentialStore.getSecret(secret)).isSameAs(payload);
    }

    @Test
    void testStoreSecretWritesInlinePayload() {
        CredentialSecret secret = mock(CredentialSecret.class);
        Map<String, Object> payload = Map.of("apiKey", "abc");

        databaseCredentialStore.storeSecret(secret, payload);

        verify(secret).setPayload(payload);
    }

    @Test
    void testDeleteSecretIsNoOp() {
        CredentialSecret secret = mock(CredentialSecret.class);

        databaseCredentialStore.deleteSecret(secret);

        verifyNoInteractions(secret);
    }
}
