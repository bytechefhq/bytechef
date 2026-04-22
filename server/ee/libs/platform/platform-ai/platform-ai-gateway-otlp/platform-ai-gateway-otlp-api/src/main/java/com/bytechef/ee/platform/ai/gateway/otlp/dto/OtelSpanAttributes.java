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
 * Per-span attributes (the OTLP {@code Span.attributes} list, after unwrap). Distinct type from
 * {@link OtelResourceAttributes} so the two cannot be swapped at any {@link OtelGenAiSpan} construction site — both
 * maps are structurally {@code Map<String, Object>}, and a positional swap would silently corrupt every downstream
 * {@code gen_ai.*} lookup.
 *
 * <p>
 * The map is defensively copied via {@link Map#copyOf(Map)} so the wrapper is deeply immutable for keys / scalar
 * values.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record OtelSpanAttributes(Map<String, Object> values) {

    /**
     * Sentinel inserted into attribute arrays in place of OTLP {@code VALUE_NOT_SET} entries. Dropping unset slots
     * silently shifts positional indices for every consumer (e.g. {@code gen_ai.input.messages[3]} would resolve to the
     * wrong element after a single missing earlier entry); preserving a sentinel keeps positional access stable while
     * still letting consumers detect "no value here" by identity comparison ({@code value == UNSET_ATTRIBUTE_VALUE}).
     *
     * <p>
     * Implemented as an enum (single-constant) rather than {@code new Object()}: the enum guarantees JVM-wide identity
     * even across reflection-based serialization (Jackson, Kryo, Java native), so any future code path that round-trips
     * the attribute map cannot silently break the contract by deserializing the sentinel into a "real" value.
     *
     * <p>
     * Lives on {@link OtelSpanAttributes} (otlp-api) rather than on the mapper impl (otlp-service) so any module that
     * depends only on otlp-api — gateway-api, downstream consumers — can reach the sentinel for {@code ==} compare
     * without dragging in the mapper module's protobuf dependency.
     */
    public enum UnsetAttribute {
        INSTANCE;

        @Override
        public String toString() {
            return "<unset>";
        }
    }

    /**
     * Convenience constant. {@code UnsetAttribute.INSTANCE == UNSET_ATTRIBUTE_VALUE} for any consumer using either
     * form.
     */
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
