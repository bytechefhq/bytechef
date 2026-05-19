/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.credential.store.aws.secretsmanager.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Translates {@code bytechef.credential-store.external.provider=aws-secrets-manager} into
 * {@code spring.cloud.aws.secretsmanager.enabled=true} so that Spring Cloud AWS auto-configures a
 * {@code SecretsManagerClient}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsSecretsManagerCredentialStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        String provider = environment.getProperty(
            "bytechef.credential-store.external.provider", String.class);

        if (Objects.equals(provider, "aws-secrets-manager")) {
            source.put("spring.cloud.aws.secretsmanager.enabled", true);
        }

        MapPropertySource mapPropertySource = new MapPropertySource(
            "Custom Spring Cloud AWS Secrets Manager Credential Store Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
