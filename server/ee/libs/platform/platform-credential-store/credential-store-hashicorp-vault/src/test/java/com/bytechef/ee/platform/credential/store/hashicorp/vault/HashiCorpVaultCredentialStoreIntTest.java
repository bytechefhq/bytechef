/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.hashicorp.vault;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = HashiCorpVaultCredentialStoreIntTestConfiguration.class)
@Testcontainers
class HashiCorpVaultCredentialStoreIntTest {

    private static final String VAULT_TOKEN = "test-root-token";

    @Container
    static VaultContainer<?> vault = new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.15"))
        .withVaultToken(VAULT_TOKEN);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("bytechef.credential-store.cache.ttl", () -> "5m");
        registry.add("bytechef.credential-store.external.provider", () -> "hashicorp-vault");
        registry.add("bytechef.credential-store.hashicorp-vault.uri", vault::getHttpHostAddress);
        registry.add("bytechef.credential-store.hashicorp-vault.authentication", () -> "token");
        registry.add("bytechef.credential-store.hashicorp-vault.kv-mount", () -> "secret");
        registry.add("bytechef.credential-store.hashicorp-vault.token", () -> VAULT_TOKEN);
        registry.add("vault.uri", vault::getHttpHostAddress);
        registry.add("vault.authentication", () -> "token");
        registry.add("vault.token", () -> VAULT_TOKEN);
    }

    @Autowired
    private HashiCorpVaultCredentialStore store;

    @Test
    void testGetTypeReportsHashicorpVault() {
        assertThat(store.getType()).isEqualTo(CredentialStoreType.HASHICORP_VAULT);
    }

    @Test
    void testIsReadOnlyDefaultsToFalse() {
        assertThat(store.isReadOnly()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStoreThenGetParametersRoundTrip() {
        Connection connection = new Connection();

        store.storeSecret(connection, Map.of("apiKey", "secret-value", "extraField", "42"));

        assertThat(connection.getCredentialRef()).isNotBlank();
        assertThat(connection.getParameters()).isEmpty();

        Map<String, Object> retrieved = (Map<String, Object>) store.getSecret(connection);

        assertThat(retrieved).containsEntry("apiKey", "secret-value");
        assertThat(retrieved).containsEntry("extraField", "42");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateExistingSecret() {
        Connection connection = new Connection();

        store.storeSecret(connection, Map.of("apiKey", "v1"));
        store.storeSecret(connection, Map.of("apiKey", "v2"));

        Map<String, Object> retrieved = (Map<String, Object>) store.getSecret(connection);

        assertThat(retrieved).containsEntry("apiKey", "v2");
    }

    @Test
    void testDeleteParametersRemovesSecret() {
        Connection connection = new Connection();

        store.storeSecret(connection, Map.of("apiKey", "to-be-deleted"));

        String ref = connection.getCredentialRef();

        store.deleteSecret(connection);

        Connection probe = new Connection();

        probe.setCredentialRef(ref);

        assertThat(store.getSecret(probe)).isEmpty();
    }

    @Test
    void testGetParametersWithNoCredentialRefReturnsEmpty() {
        Connection connection = new Connection();

        assertThat(store.getSecret(connection)).isEmpty();
    }
}
