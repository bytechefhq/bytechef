/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.discovery;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.NameResolverRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Resolves {@code discovery:///<service-id>} targets through Spring Cloud discovery. gRPC keeps one global registry, so
 * a single provider is registered once and reads the discovery client of the running application.
 *
 * @version ee
 */
public final class DiscoveryClientNameResolverProvider extends NameResolverProvider {

    public static final String SCHEME = "discovery";

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(30);

    private static final DiscoveryClientNameResolverProvider INSTANCE = new DiscoveryClientNameResolverProvider();

    static {
        NameResolverRegistry.getDefaultRegistry()
            .register(INSTANCE);
    }

    private final AtomicReference<@Nullable DiscoveryClient> discoveryClientReference = new AtomicReference<>();

    private DiscoveryClientNameResolverProvider() {
    }

    public static void setDiscoveryClient(@Nullable DiscoveryClient discoveryClient) {
        INSTANCE.discoveryClientReference.set(discoveryClient);
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    @Override
    public @Nullable NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        DiscoveryClient discoveryClient = discoveryClientReference.get();

        if (!SCHEME.equals(targetUri.getScheme()) || discoveryClient == null) {
            return null;
        }

        String path = targetUri.getPath();

        return new DiscoveryClientNameResolver(
            path.startsWith("/") ? path.substring(1) : path, discoveryClient, args.getScheduledExecutorService(),
            REFRESH_INTERVAL);
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }
}
