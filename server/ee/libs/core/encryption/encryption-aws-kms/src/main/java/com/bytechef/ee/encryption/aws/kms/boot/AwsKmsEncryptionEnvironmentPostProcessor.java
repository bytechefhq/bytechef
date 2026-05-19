/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Activates AWS KMS encryption when {@code bytechef.encryption.provider=aws-kms}. Sets
 * {@code spring.cloud.aws.enabled=true} so that Spring Cloud AWS core auto-configuration
 * ({@code CredentialsProviderAutoConfiguration}, {@code RegionProviderAutoConfiguration}) wires the
 * {@code AwsCredentialsProvider} and {@code AwsRegionProvider} beans consumed by
 * {@link com.bytechef.ee.encryption.aws.kms.config.AwsKmsEncryptionConfiguration}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsKmsEncryptionEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        String provider = environment.getProperty("bytechef.encryption.provider", String.class);

        if (Objects.equals(provider, "aws-kms")) {
            source.put("spring.cloud.aws.kms.enabled", true);
        }

        MapPropertySource mapPropertySource =
            new MapPropertySource("Custom AWS KMS Encryption Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
