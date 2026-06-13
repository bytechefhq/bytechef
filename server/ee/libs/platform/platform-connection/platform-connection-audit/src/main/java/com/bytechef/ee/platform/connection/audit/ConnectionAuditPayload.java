/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Typed payload accepted by the preferred
 * {@link ConnectionAuditPublisher#publish(ConnectionAuditEvent, ConnectionAuditPayload)} overload. Uses
 * {@code Map<String, String>} for additional data so call sites cannot accidentally stash arbitrary {@code Object}
 * values that the audit pipeline would then stringify inconsistently.
 *
 * <p>
 * The payload is immutable by construction: {@code data} is defensively copied, {@code connectionId} is validated
 * non-negative, and {@link #toMap()} adapts the payload back to the generic {@code Map<String, Object>} shape the
 * existing audit pipeline expects.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectionAuditPayload(long connectionId, Map<String, String> data) {

    public ConnectionAuditPayload {
        if (connectionId < 0) {
            throw new IllegalArgumentException("connectionId must be non-negative; got " + connectionId);
        }

        Objects.requireNonNull(data, "data");

        // Reject a colliding "connectionId" key at the source. The payload's connectionId slot is
        // authoritative; silently yielding to a caller-supplied entry in toMap() would mean two call
        // sites disagreeing on the subject of the audit event with no visible error.
        if (data.containsKey("connectionId")) {
            throw new IllegalArgumentException(
                "data must not contain a 'connectionId' key; use the payload's connectionId slot instead");
        }

        data = Map.copyOf(data);
    }

    public static ConnectionAuditPayload of(long connectionId) {
        return new ConnectionAuditPayload(connectionId, Map.of());
    }

    public static ConnectionAuditPayload of(long connectionId, String key, String value) {
        return new ConnectionAuditPayload(connectionId, Map.of(key, value));
    }

    public Map<String, Object> toMap() {
        // Compact-constructor enforces that `data` does not already carry a "connectionId" key, so a
        // plain put cannot clobber a user-supplied value here. Using put (not putIfAbsent) makes the
        // invariant "the payload's connectionId slot is authoritative" hold by construction.
        Map<String, Object> result = new LinkedHashMap<>(data);

        result.put("connectionId", String.valueOf(connectionId));

        return result;
    }
}
