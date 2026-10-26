/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 */
class GrpcRemoteConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class, GrpcRemoteConfiguration.class);

    @Test
    void testBlankTokenFailsWithBindableService() {
        applicationContextRunner
            .withUserConfiguration(EchoServiceConfiguration.class)
            .withPropertyValues("bytechef.remote.grpc.plaintext=true", "bytechef.remote.grpc.service-token=")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .hasStackTraceContaining("bytechef.remote.grpc.service-token"));
    }

    @Test
    void testPlaintextFalseWithoutSslBundleFails() {
        applicationContextRunner
            .withUserConfiguration(EchoServiceConfiguration.class)
            .withPropertyValues("bytechef.remote.grpc.plaintext=false", "bytechef.remote.grpc.service-token=secret")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .hasStackTraceContaining("spring.grpc.server.ssl.bundle"));
    }

    @Test
    void testSslDisabledWithBundleFails() {
        applicationContextRunner
            .withUserConfiguration(EchoServiceConfiguration.class)
            .withPropertyValues(
                "bytechef.remote.grpc.plaintext=false", "bytechef.remote.grpc.service-token=secret",
                "spring.grpc.server.ssl.bundle=grpc", "spring.grpc.server.ssl.enabled=false")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .hasStackTraceContaining("spring.grpc.server.ssl.enabled"));
    }

    @Test
    void testPlaintextTrueWithTokenStarts() {
        applicationContextRunner
            .withUserConfiguration(EchoServiceConfiguration.class)
            .withPropertyValues("bytechef.remote.grpc.plaintext=true", "bytechef.remote.grpc.service-token=secret")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void testNoBindableServiceNeedsNoToken() {
        applicationContextRunner
            .withPropertyValues("bytechef.remote.grpc.plaintext=false", "bytechef.remote.grpc.service-token=")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class PropertiesConfiguration {
    }

    @Configuration
    static class EchoServiceConfiguration {

        @Bean
        EchoService echoService() {
            return new EchoService();
        }
    }
}
