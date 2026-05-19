/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.hashicorp.vault;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.credential.store.hashicorp.vault.config.HashiCorpVaultCredentialStoreConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.vault.config.EnvironmentVaultConfiguration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ImportAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    EnvironmentVaultConfiguration.class, HashiCorpVaultCredentialStoreConfiguration.class
})
class HashiCorpVaultCredentialStoreIntTestConfiguration {
}
