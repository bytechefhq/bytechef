/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import com.bytechef.commons.util.JsonUtils;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Writes cache values as plain JSON without type information; values are read back as strings, numbers, booleans, lists
 * and maps.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class JsonCacheValueRedisSerializer implements RedisSerializer<Object> {

    private static final byte[] EMPTY = new byte[0];

    @Override
    public byte[] serialize(@Nullable Object value) {
        if (value == null) {
            return EMPTY;
        }

        return JsonUtils.writeValueAsBytes(value);
    }

    @Override
    public @Nullable Object deserialize(byte @Nullable [] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        return JsonUtils.read(new String(bytes, StandardCharsets.UTF_8));
    }
}
