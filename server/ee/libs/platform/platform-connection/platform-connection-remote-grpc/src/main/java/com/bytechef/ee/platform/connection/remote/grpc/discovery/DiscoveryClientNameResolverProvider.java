/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.discovery;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import java.net.URI;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * gRPC {@link NameResolverProvider} for the {@code discovery} scheme. A target of {@code discovery:///<serviceId>}
 * yields a {@link DiscoveryClientNameResolver} that looks the service up in the Spring Cloud {@link DiscoveryClient}.
 * Registered programmatically (carrying the Spring-managed {@code DiscoveryClient}) rather than via ServiceLoader, so
 * it can reuse the application's discovery bean.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DiscoveryClientNameResolverProvider extends NameResolverProvider {

    static final String SCHEME = "discovery";

    private final DiscoveryClient discoveryClient;

    public DiscoveryClientNameResolverProvider(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public @Nullable NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!SCHEME.equals(targetUri.getScheme())) {
            return null;
        }

        return new DiscoveryClientNameResolver(extractServiceId(targetUri), discoveryClient);
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    private static String extractServiceId(URI targetUri) {
        String path = targetUri.getPath();

        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }

        String authority = targetUri.getAuthority();

        return authority != null ? authority : "";
    }
}
