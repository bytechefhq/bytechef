/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record OtelResourceAttributes(Map<String, Object> values) {

    public OtelResourceAttributes {
        Validate.notNull(values, "values must not be null");

        values = Map.copyOf(values);
    }

    public static OtelResourceAttributes empty() {
        return new OtelResourceAttributes(Map.of());
    }

    public static OtelResourceAttributes of(Map<String, Object> values) {
        return new OtelResourceAttributes(values);
    }

    public Object get(String key) {
        return values.get(key);
    }
}
