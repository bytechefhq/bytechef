/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.aws.secretsmanager;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.credential.store.CredentialPathResolver;
import com.bytechef.platform.credential.store.CredentialSecret;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

/**
 * AWS Secrets Manager-backed {@link CredentialStore}. Writes the credential payload as a JSON-serialized map under a
 * path derived from the operator-configured template (default {@code "bytechef/{ref}"}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsSecretsManagerCredentialStore implements CredentialStore {

    private static final Logger log = LoggerFactory.getLogger(AwsSecretsManagerCredentialStore.class);

    private static final String DEFAULT_PATH_TEMPLATE = "bytechef/{ref}";

    private final Cache<String, Map<String, Object>> cache;
    private final ObjectMapper objectMapper;
    private final String pathTemplate;
    private final boolean readOnly;
    private final SecretsManagerClient secretsManagerClient;

    @SuppressFBWarnings("EI2")
    public AwsSecretsManagerCredentialStore(
        ApplicationProperties applicationProperties, ObjectMapper objectMapper,
        SecretsManagerClient secretsManagerClient) {

        ApplicationProperties.CredentialStore credentialStore = applicationProperties.getCredentialStore();

        String configuredTemplate = credentialStore.getPathTemplate();

        this.pathTemplate = configuredTemplate != null ? configuredTemplate : DEFAULT_PATH_TEMPLATE;
        this.readOnly = credentialStore.getAwsSecretsManager()
            .isReadOnly();
        this.objectMapper = objectMapper;
        this.secretsManagerClient = secretsManagerClient;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(credentialStore.getCache()
                .getTtl())
            .build();
    }

    @Override
    public CredentialStoreType getType() {
        return CredentialStoreType.AWS_SECRETS_MANAGER;
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

        String secretName = resolvePath(ref);

        return cache.get(secretName, this::fetchSecret);
    }

    @Override
    public void storeSecret(CredentialSecret secret, Map<String, ?> payload) {
        if (readOnly) {
            throw new UnsupportedOperationException("AWS Secrets Manager store is configured read-only");
        }

        String ref = secret.getCredentialRef();
        boolean isNewSecret = ref == null;

        if (isNewSecret) {
            ref = UUID.randomUUID()
                .toString();

            secret.setCredentialRef(ref);
        }

        String secretName = resolvePath(ref);
        String secretJson = serialize(payload);

        if (isNewSecret) {
            secretsManagerClient.createSecret(
                CreateSecretRequest.builder()
                    .name(secretName)
                    .secretString(secretJson)
                    .build());
        } else {
            secretsManagerClient.putSecretValue(
                PutSecretValueRequest.builder()
                    .secretId(secretName)
                    .secretString(secretJson)
                    .build());
        }

        cache.invalidate(secretName);

        secret.setPayload(Map.of());
    }

    @Override
    public void deleteSecret(CredentialSecret secret) {
        if (readOnly) {
            throw new UnsupportedOperationException("AWS Secrets Manager store is configured read-only");
        }

        String ref = secret.getCredentialRef();

        if (ref == null) {
            return;
        }

        String secretName = resolvePath(ref);

        secretsManagerClient.deleteSecret(
            DeleteSecretRequest.builder()
                .secretId(secretName)
                .forceDeleteWithoutRecovery(true)
                .build());

        cache.invalidate(secretName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchSecret(String secretName) {
        try {
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(
                GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build());

            return objectMapper.readValue(response.secretString(), Map.class);
        } catch (ResourceNotFoundException e) {
            log.warn("Secret not found in AWS Secrets Manager: {}", secretName);

            return Map.of();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to fetch secret from AWS Secrets Manager: " + secretName, e);
        }
    }

    private String serialize(Map<String, ?> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize parameters to JSON", e);
        }
    }

    private String resolvePath(String ref) {
        return CredentialPathResolver.resolve(pathTemplate, TenantContext.getCurrentTenantId(), null, ref);
    }
}
