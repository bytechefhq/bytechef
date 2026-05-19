/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.hashicorp.vault.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Translates {@code bytechef.credential-store.hashicorp-vault.*} properties into {@code vault.*} properties so Spring
 * Vault's {@link org.springframework.vault.config.EnvironmentVaultConfiguration} wires up the {@code VaultTemplate}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class HashiCorpVaultCredentialStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty(
            "bytechef.credential-store.external.provider", String.class);

        if (!Objects.equals(provider, "hashicorp-vault")) {
            return;
        }

        Map<String, Object> source = new HashMap<>();

        copyIfPresent(environment, source,
            "bytechef.credential-store.hashicorp-vault.uri", "vault.uri");
        copyIfPresent(environment, source,
            "bytechef.credential-store.hashicorp-vault.authentication", "vault.authentication");
        copyIfPresent(environment, source,
            "bytechef.credential-store.hashicorp-vault.token", "vault.token");
        copyIfPresent(environment, source,
            "bytechef.credential-store.hashicorp-vault.approle.role-id",
            "vault.app-role.role-id");
        copyIfPresent(environment, source,
            "bytechef.credential-store.hashicorp-vault.approle.secret-id",
            "vault.app-role.secret-id");

        MapPropertySource mapPropertySource = new MapPropertySource(
            "Custom HashiCorp Vault Credential Store Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }

    private static void copyIfPresent(
        ConfigurableEnvironment environment, Map<String, Object> target, String sourceKey, String targetKey) {

        String value = environment.getProperty(sourceKey);

        if (value != null) {
            target.put(targetKey, value);
        }
    }
}
