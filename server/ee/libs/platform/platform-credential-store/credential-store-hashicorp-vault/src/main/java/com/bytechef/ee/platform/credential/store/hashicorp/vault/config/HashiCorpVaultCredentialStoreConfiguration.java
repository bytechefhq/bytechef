/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.hashicorp.vault.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.credential.store.hashicorp.vault.HashiCorpVaultCredentialStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultTemplate;

/**
 * Wires {@link HashiCorpVaultCredentialStore} when the operator selects HashiCorp Vault. Spring Vault's
 * {@code EnvironmentVaultConfiguration} creates the {@code VaultTemplate} from the {@code vault.*} properties produced
 * by
 * {@link com.bytechef.ee.platform.credential.store.hashicorp.vault.boot.HashiCorpVaultCredentialStoreEnvironmentPostProcessor}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef.credential-store.external", name = "provider", havingValue = "hashicorp-vault")
public class HashiCorpVaultCredentialStoreConfiguration {

    @Bean
    HashiCorpVaultCredentialStore hashiCorpVaultCredentialStore(
        ApplicationProperties applicationProperties, VaultTemplate vaultTemplate) {

        return new HashiCorpVaultCredentialStore(applicationProperties, vaultTemplate);
    }
}
