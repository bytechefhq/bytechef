/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.hashicorp.vault;

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
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * HashiCorp Vault-backed {@link CredentialStore}. Reads/writes secrets in KV v2 at a path derived from the
 * operator-configured template (default {@code "bytechef/{ref}"}), shared across connections and properties.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class HashiCorpVaultCredentialStore implements CredentialStore {

    private static final Logger log = LoggerFactory.getLogger(HashiCorpVaultCredentialStore.class);

    private static final String DEFAULT_PATH_TEMPLATE = "bytechef/{ref}";

    private final Cache<String, Map<String, Object>> cache;
    private final String kvMount;
    private final String pathTemplate;
    private final boolean readOnly;
    private final VaultTemplate vaultTemplate;

    @SuppressFBWarnings("EI2")
    public HashiCorpVaultCredentialStore(
        ApplicationProperties applicationProperties, VaultTemplate vaultTemplate) {

        ApplicationProperties.CredentialStore credentialStore = applicationProperties.getCredentialStore();
        ApplicationProperties.CredentialStore.HashiCorpVault vaultConfig = credentialStore.getHashicorpVault();

        String configuredTemplate = credentialStore.getPathTemplate();

        this.pathTemplate = configuredTemplate != null ? configuredTemplate : DEFAULT_PATH_TEMPLATE;
        this.kvMount = vaultConfig.getKvMount();
        this.readOnly = vaultConfig.isReadOnly();
        this.vaultTemplate = vaultTemplate;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(credentialStore.getCache()
                .getTtl())
            .build();
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

        String path = resolvePath(ref);

        return cache.get(path, this::fetchSecret);
    }

    @Override
    public void storeSecret(CredentialSecret secret, Map<String, ?> payload) {
        if (readOnly) {
            throw new UnsupportedOperationException("HashiCorp Vault store is configured read-only");
        }

        String ref = secret.getCredentialRef();

        if (ref == null) {
            ref = UUID.randomUUID()
                .toString();

            secret.setCredentialRef(ref);
        }

        String path = resolvePath(ref);

        kvOps().put(path, payload);

        cache.invalidate(path);

        secret.setPayload(Map.of());
    }

    @Override
    public void deleteSecret(CredentialSecret secret) {
        if (readOnly) {
            throw new UnsupportedOperationException("HashiCorp Vault store is configured read-only");
        }

        String ref = secret.getCredentialRef();

        if (ref == null) {
            return;
        }

        String path = resolvePath(ref);

        kvOps().delete(path);

        cache.invalidate(path);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchSecret(String path) {
        try {
            VaultResponse response = kvOps().get(path);

            if (response == null || response.getData() == null) {
                log.warn("Secret not found in HashiCorp Vault: {}", path);

                return Map.of();
            }

            return (Map<String, Object>) response.getData();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to fetch secret from HashiCorp Vault: " + path, exception);
        }
    }

    private VaultKeyValueOperations kvOps() {
        return vaultTemplate.opsForKeyValue(kvMount, VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
    }

    private String resolvePath(String ref) {
        return CredentialPathResolver.resolve(pathTemplate, TenantContext.getCurrentTenantId(), null, ref);
    }
}
