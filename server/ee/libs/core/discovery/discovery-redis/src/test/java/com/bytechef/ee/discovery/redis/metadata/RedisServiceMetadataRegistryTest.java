/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.discovery.redis.registry.RedisRegistration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class RedisServiceMetadataRegistryTest {

    @Test
    void testRegisterMetadataAddsToExistingMetadata() {
        RedisRegistration redisRegistration = new RedisRegistration();

        redisRegistration.setMetadata(Map.of("components", "slack"));

        RedisServiceMetadataRegistry redisServiceMetadataRegistry =
            new RedisServiceMetadataRegistry(redisRegistration);

        redisServiceMetadataRegistry.registerMetadata(Map.of("grpc-port", "9090"));

        assertThat(redisRegistration.getMetadata()).containsExactlyInAnyOrderEntriesOf(
            Map.of("components", "slack", "grpc-port", "9090"));
    }

    @Test
    void testRegisterMetadataReplacesAValueRegisteredBefore() {
        RedisRegistration redisRegistration = new RedisRegistration();

        RedisServiceMetadataRegistry redisServiceMetadataRegistry =
            new RedisServiceMetadataRegistry(redisRegistration);

        redisServiceMetadataRegistry.registerMetadata(Map.of("grpc-port", "9090"));
        redisServiceMetadataRegistry.registerMetadata(Map.of("grpc-port", "9091"));

        assertThat(redisRegistration.getMetadata()).containsExactlyEntriesOf(Map.of("grpc-port", "9091"));
    }
}
