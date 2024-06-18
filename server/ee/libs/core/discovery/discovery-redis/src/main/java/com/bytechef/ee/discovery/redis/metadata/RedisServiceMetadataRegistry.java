/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.metadata;

import com.bytechef.ee.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.ee.discovery.redis.registry.RedisRegistration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Map<String, String> curMetadataMap = redisRegistration.getMetadata();

        Set<Map.Entry<String, String>> curMetadataEntry = curMetadataMap.entrySet();
        Set<Map.Entry<String, String>> metadataEntry = metadata.entrySet();

        redisRegistration.setMetadata(
            Stream.concat(curMetadataEntry.stream(), metadataEntry.stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
