/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A raw HTTP request an app asks ByteChef to send through one of its bound connections.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ProxyRequest(
    String method, String path, Map<String, List<String>> query, Map<String, List<String>> headers,
    @Nullable String body, BodyEncoding bodyEncoding, @Nullable String contentType) {

    public ProxyRequest {
        query = Collections.unmodifiableMap(deepCopy(query));
        headers = Collections.unmodifiableMap(deepCopy(headers));
    }

    static Map<String, List<String>> deepCopy(@Nullable Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();

        if (source != null) {
            for (Map.Entry<String, List<String>> entry : source.entrySet()) {
                copy.put(entry.getKey(), copyValues(entry.getValue()));
            }
        }

        return copy;
    }

    private static List<String> copyValues(@Nullable List<@Nullable String> values) {
        if (values == null) {
            return List.of();
        }

        List<String> copy = new ArrayList<>(values.size());

        for (String value : values) {
            copy.add(value == null ? "" : value);
        }

        return Collections.unmodifiableList(copy);
    }
}
