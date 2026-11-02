/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.boot;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Keeps Spring Boot's bearer-token gRPC server security off in every app that serves gRPC to the other apps. Those
 * calls carry the caller's principal in gRPC metadata, not a bearer token, so a {@code JwtDecoder} bean (the
 * authorization server's, for example) must not turn every inbound call into a 401.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GrpcServerSecurityEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String GRPC_SERVER_OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION =
        "org.springframework.boot.grpc.server.autoconfigure.security.GrpcServerOAuth2ResourceServerAutoConfiguration";

    private static final String AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String exclude = environment.getProperty(AUTOCONFIGURE_EXCLUDE, "");

        String mergedExclude = Stream.concat(
            Arrays.stream(exclude.split(",")),
            Stream.of(GRPC_SERVER_OAUTH2_RESOURCE_SERVER_AUTO_CONFIGURATION))
            .map(String::trim)
            .filter(className -> !className.isEmpty())
            .distinct()
            .collect(Collectors.joining(","));

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(
            new MapPropertySource("gRPC Server Security Config", Map.of(AUTOCONFIGURE_EXCLUDE, mergedExclude)));
    }
}
