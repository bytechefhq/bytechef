/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.google.protobuf.ByteString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Carries free-form values in proto {@code bytes} fields as their Jackson JSON, so numbers keep their Java types. An
 * empty value means {@code null}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class JsonBytes {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public JsonBytes(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ByteString write(@Nullable Object value) {
        if (value == null) {
            return ByteString.EMPTY;
        }

        return ByteString.copyFrom(objectMapper.writeValueAsBytes(value));
    }

    public @Nullable Object read(ByteString bytes) {
        if (bytes.isEmpty()) {
            return null;
        }

        return objectMapper.readValue(bytes.toByteArray(), Object.class);
    }

    public <T> @Nullable T read(ByteString bytes, TypeReference<T> typeReference) {
        if (bytes.isEmpty()) {
            return null;
        }

        return objectMapper.readValue(bytes.toByteArray(), typeReference);
    }

    public @Nullable Map<String, Object> readMap(ByteString bytes) {
        return read(bytes, MAP_TYPE_REFERENCE);
    }
}
