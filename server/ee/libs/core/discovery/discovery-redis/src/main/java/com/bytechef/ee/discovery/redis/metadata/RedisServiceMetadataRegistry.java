/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.metadata;

import com.bytechef.ee.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.ee.discovery.redis.registry.RedisRegistration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisServiceMetadataRegistry implements ServiceMetadataRegistry {

    private final RedisRegistration redisRegistration;

    @SuppressFBWarnings("EI2")
    public RedisServiceMetadataRegistry(RedisRegistration redisRegistration) {
        this.redisRegistration = redisRegistration;
    }

    @Override
    public void registerMetadata(Map<String, String> metadata) {
        Map<String, String> mergedMetadata = new HashMap<>(
            Objects.requireNonNull(redisRegistration.getMetadata(), "metadata"));

        mergedMetadata.putAll(metadata);

        redisRegistration.setMetadata(mergedMetadata);
    }
}
