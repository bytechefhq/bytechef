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

package com.bytechef.platform.connection.web.graphql;

import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ConnectionCredentialStoreGraphQlControllerIntTest.TestStoresConfiguration.class,
    ConnectionCredentialStoreGraphQlController.class
})
@GraphQlTest(
    controllers = ConnectionCredentialStoreGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@Import(ConnectionCredentialStoreGraphQlControllerIntTest.TestStoresConfiguration.class)
class ConnectionCredentialStoreGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testConnectionCredentialStoresReturnsRegisteredStores() {
        graphQlTester.document("""
            query {
                connectionCredentialStores {
                    type
                    readOnly
                }
            }
            """)
            .execute()
            .path("connectionCredentialStores")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testDatabaseStoreReportsNotReadOnly() {
        graphQlTester.document("""
            query {
                connectionCredentialStores {
                    type
                    readOnly
                }
            }
            """)
            .execute()
            .path("connectionCredentialStores")
            .entityList(Map.class)
            .satisfies(stores -> {
                boolean found = stores.stream()
                    .anyMatch(
                        store -> "DATABASE".equals(store.get("type")) && Boolean.FALSE.equals(store.get("readOnly")));

                if (!found) {
                    throw new AssertionError(
                        "Expected a DATABASE store with readOnly=false but none was found in: " + stores);
                }
            });
    }

    @Test
    void testHashiCorpVaultStoreReportsReadOnly() {
        graphQlTester.document("""
            query {
                connectionCredentialStores {
                    type
                    readOnly
                }
            }
            """)
            .execute()
            .path("connectionCredentialStores")
            .entityList(Map.class)
            .satisfies(stores -> {
                boolean found = stores.stream()
                    .anyMatch(
                        store -> "HASHICORP_VAULT".equals(store.get("type"))
                            && Boolean.TRUE.equals(store.get("readOnly")));

                if (!found) {
                    throw new AssertionError(
                        "Expected a HASHICORP_VAULT store with readOnly=true but none was found in: " + stores);
                }
            });
    }

    @TestConfiguration
    static class TestStoresConfiguration {

        @Bean
        List<CredentialStore> credentialStores() {
            return List.of(
                new StubStore(CredentialStoreType.DATABASE, false),
                new StubStore(CredentialStoreType.HASHICORP_VAULT, true));
        }
    }

    private record StubStore(CredentialStoreType type, boolean readOnly)
        implements CredentialStore {

        @Override
        public CredentialStoreType getType() {
            return type;
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public Map<String, ?> getSecret(CredentialSecret secret) {
            return Map.of();
        }

        @Override
        public void storeSecret(CredentialSecret secret, Map<String, ?> payload) {
            // no-op for test
        }

        @Override
        public void deleteSecret(CredentialSecret secret) {
            // no-op for test
        }
    }
}
