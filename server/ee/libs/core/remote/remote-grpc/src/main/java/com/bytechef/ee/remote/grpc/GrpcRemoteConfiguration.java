/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.remote.grpc.discovery.DiscoveryClientNameResolverProvider;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenServerInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextServerInterceptor;
import io.grpc.BindableService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * Secures every gRPC service a microservice serves with the internal service token and its tenant, refuses to serve
 * without a token or, unless the trusted-network switch is on, without TLS, and resolves discovery targets.
 *
 * @version ee
 */
@Configuration
public class GrpcRemoteConfiguration {

    @Bean
    SmartInitializingSingleton grpcDiscoveryNameResolverRegistration(
        ObjectProvider<DiscoveryClient> discoveryClientProvider) {

        return () -> DiscoveryClientNameResolverProvider.setDiscoveryClient(discoveryClientProvider.getIfAvailable());
    }

    @Bean
    SmartInitializingSingleton grpcServerSecurityCheck(
        ApplicationProperties applicationProperties, Environment environment, ListableBeanFactory beanFactory) {

        return () -> {
            boolean servesBytechefService = beanFactory.getBeansOfType(BindableService.class)
                .values()
                .stream()
                .anyMatch(bindableService -> StringUtils.startsWith(
                    bindableService.getClass()
                        .getName(),
                    "com.bytechef."));

            if (!servesBytechefService) {
                return;
            }

            ApplicationProperties.Remote remote = applicationProperties.getRemote();

            ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

            if (StringUtils.isBlank(grpc.getServiceToken())) {
                throw new IllegalStateException(
                    "bytechef.remote.grpc.service-token must be set to serve gRPC to other microservices");
            }

            if (!grpc.isPlaintext() && StringUtils.isBlank(environment.getProperty("spring.grpc.server.ssl.bundle"))) {
                throw new IllegalStateException(
                    "spring.grpc.server.ssl.bundle must be set to serve gRPC with TLS, or set " +
                        "bytechef.remote.grpc.plaintext=true on a trusted network");
            }
        };
    }

    @Bean
    @GlobalServerInterceptor
    ServiceTokenServerInterceptor serviceTokenServerInterceptor(ApplicationProperties applicationProperties) {
        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        return new ServiceTokenServerInterceptor(grpc.getServiceToken());
    }

    @Bean
    @GlobalServerInterceptor
    TenantContextServerInterceptor tenantContextServerInterceptor() {
        return new TenantContextServerInterceptor();
    }
}
