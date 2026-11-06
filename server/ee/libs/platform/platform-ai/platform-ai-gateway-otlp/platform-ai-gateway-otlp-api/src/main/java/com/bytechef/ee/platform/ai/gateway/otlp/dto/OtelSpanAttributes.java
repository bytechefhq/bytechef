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
public record OtelSpanAttributes(Map<String, Object> values) {

    public enum UnsetAttribute {
        INSTANCE;

        @Override
        public String toString() {
            return "<unset>";
        }
    }

    public static final Object UNSET_ATTRIBUTE_VALUE = UnsetAttribute.INSTANCE;

    public OtelSpanAttributes {
        Validate.notNull(values, "values must not be null");

        values = Map.copyOf(values);
    }

    public static OtelSpanAttributes empty() {
        return new OtelSpanAttributes(Map.of());
    }

    public static OtelSpanAttributes of(Map<String, Object> values) {
        return new OtelSpanAttributes(values);
    }

    public Object get(String key) {
        return values.get(key);
    }
}
