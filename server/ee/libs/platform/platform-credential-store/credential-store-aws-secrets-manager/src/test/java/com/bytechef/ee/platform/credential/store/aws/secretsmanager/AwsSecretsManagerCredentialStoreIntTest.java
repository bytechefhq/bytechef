/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.aws.secretsmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AwsSecretsManagerCredentialStoreIntTestConfiguration.class)
@Testcontainers
class AwsSecretsManagerCredentialStoreIntTest {

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SECRETSMANAGER);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("bytechef.credential-store.external.provider", () -> "aws-secrets-manager");
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add(
            "spring.cloud.aws.secretsmanager.endpoint",
            () -> localStack.getEndpointOverride(SECRETSMANAGER)
                .toString());
    }

    @Autowired
    private AwsSecretsManagerCredentialStore store;

    @Test
    void testGetTypeReportsAwsSecretsManager() {
        assertThat(store.getType()).isEqualTo(CredentialStoreType.AWS_SECRETS_MANAGER);
    }

    @Test
    void testIsReadOnlyDefaultsToFalse() {
        assertThat(store.isReadOnly()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStoreThenGetParametersRoundTrip() {
        Connection connection = new Connection();

        store.storeSecret(connection, Map.of("apiKey", "secret-value", "extraField", 42));

        assertThat(connection.getCredentialRef()).isNotBlank();
        assertThat(connection.getParameters()).isEmpty();

        Map<String, Object> retrieved = (Map<String, Object>) store.getSecret(connection);

        assertThat(retrieved).containsEntry("apiKey", "secret-value");
        assertThat(retrieved).containsEntry("extraField", 42);
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
