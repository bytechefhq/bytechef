/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import com.bytechef.message.event.MessageEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Tells the other nodes of a cluster to drop a cache entry, or the whole cache when the key is {@code null}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CacheInvalidationEvent implements MessageEvent<CacheInvalidationMessageRoute> {

    private String cacheName;
    private Instant createDate;
    private @Nullable String key;
    private Map<String, Object> metadata = new HashMap<>();
    private String nodeId;
    private CacheInvalidationMessageRoute route;

    private CacheInvalidationEvent() {
    }

    public CacheInvalidationEvent(String nodeId, String cacheName, @Nullable String key) {
        this.cacheName = cacheName;
        this.createDate = Instant.now();
        this.key = key;
        this.nodeId = nodeId;
        this.route = CacheInvalidationMessageRoute.CACHE_INVALIDATION_EVENTS;
    }

    public String getCacheName() {
        return cacheName;
    }

    @Override
    public Instant getCreateDate() {
        return createDate;
    }

    public @Nullable String getKey() {
        return key;
    }

    @Override
    public Object getMetadata(String name) {
        return metadata.get(name);
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public CacheInvalidationMessageRoute getRoute() {
        return route;
    }

    @Override
    public void putMetadata(String name, Object value) {
        metadata.put(name, value);
    }

    @Override
    public String toString() {
        return "CacheInvalidationEvent{" +
            "cacheName='" + cacheName + '\'' +
            ", key='" + key + '\'' +
            ", nodeId='" + nodeId + '\'' +
            ", createDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
