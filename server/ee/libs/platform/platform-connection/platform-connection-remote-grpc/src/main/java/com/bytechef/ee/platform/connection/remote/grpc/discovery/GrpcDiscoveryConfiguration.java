/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.discovery;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.NameResolverRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link DiscoveryClientNameResolverProvider} with gRPC's global {@link NameResolverRegistry} when the
 * gRPC remote transport is active and a Spring Cloud {@link DiscoveryClient} is present, enabling
 * {@code discovery:///<serviceId>} channel targets to resolve through the same registry the REST transport uses. If no
 * discovery client is available (e.g. the monolith), registration is skipped and static/dns targets still work.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.remote.transport", havingValue = "grpc")
public class GrpcDiscoveryConfiguration implements InitializingBean {

    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;

    @SuppressFBWarnings("EI")
    public GrpcDiscoveryConfiguration(ObjectProvider<DiscoveryClient> discoveryClientProvider) {
        this.discoveryClientProvider = discoveryClientProvider;
    }

    @Override
    public void afterPropertiesSet() {
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();

        if (discoveryClient != null) {
            NameResolverRegistry.getDefaultRegistry()
                .register(new DiscoveryClientNameResolverProvider(discoveryClient));
        }
    }
}
