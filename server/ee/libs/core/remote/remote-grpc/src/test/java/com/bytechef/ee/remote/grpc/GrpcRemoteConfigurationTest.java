/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import io.grpc.NameResolver;
import io.grpc.NameResolverRegistry;
import io.grpc.SynchronizationContext;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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

    @Test
    void testBlankTokenFailsWhenABeanCallsAnotherMicroservice() {
        applicationContextRunner
            .withUserConfiguration(RemoteCallerConfiguration.class)
            .withPropertyValues("bytechef.remote.grpc.plaintext=true", "bytechef.remote.grpc.service-token=")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .hasStackTraceContaining("bytechef.remote.grpc.service-token must be set to call another"));
    }

    @Test
    void testBlankTokenWithUnusedChannelsStarts() {
        applicationContextRunner
            .withBean(ManagedGrpcChannels.class)
            .withPropertyValues("bytechef.remote.grpc.plaintext=true", "bytechef.remote.grpc.service-token=")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void testTokenWithRemoteCallerStarts() {
        applicationContextRunner
            .withUserConfiguration(RemoteCallerConfiguration.class)
            .withPropertyValues("bytechef.remote.grpc.plaintext=true", "bytechef.remote.grpc.service-token=secret")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void testClosedContextNoLongerResolvesThroughItsDiscoveryClient() {
        applicationContextRunner
            .withBean(DiscoveryClient.class, () -> mock(DiscoveryClient.class))
            .withPropertyValues("bytechef.remote.grpc.plaintext=true", "bytechef.remote.grpc.service-token=secret")
            .run(context -> assertThat(newDiscoveryNameResolver()).isNotNull());

        assertThat(newDiscoveryNameResolver()).isNull();
    }

    private static NameResolver newDiscoveryNameResolver() {
        NameResolver.Factory factory = NameResolverRegistry.getDefaultRegistry()
            .asFactory();

        return factory.newNameResolver(
            URI.create("discovery:///configuration-app"),
            NameResolver.Args.newBuilder()
                .setDefaultPort(9090)
                .setProxyDetector(socketAddress -> null)
                .setSynchronizationContext(new SynchronizationContext((thread, throwable) -> {}))
                .setServiceConfigParser(mock(NameResolver.ServiceConfigParser.class))
                .setScheduledExecutorService(mock(ScheduledExecutorService.class))
                .build());
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class PropertiesConfiguration {
    }

    @Configuration
    static class RemoteCallerConfiguration {

        @Bean
        ManagedGrpcChannels managedGrpcChannels(ApplicationProperties applicationProperties) {
            return new ManagedGrpcChannels(applicationProperties);
        }

        @Bean
        RemoteCaller remoteCaller(GrpcChannels grpcChannels) {
            return new RemoteCaller(grpcChannels);
        }
    }

    record RemoteCaller(GrpcChannels grpcChannels) {
    }

    @Configuration
    static class EchoServiceConfiguration {

        @Bean
        EchoService echoService() {
            return new EchoService();
        }
    }
}
