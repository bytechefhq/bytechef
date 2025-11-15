/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.platform.connection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionContext {

    private static final AtomicLong ATOMIC_CONNECTION_ID = new AtomicLong(1);
    private static final Map<Long, Connection> CONNECTION_MAP = new ConcurrentHashMap<>();

    public static Map<String, ?> getConnectionParameters(long id) {
        return CONNECTION_MAP.get(id).parameters;
    }

    public static long putConnectionParameters(String name, Map<String, ?> parameters) {
        long connectionId = -1;

        for (Map.Entry<Long, Connection> entry : CONNECTION_MAP.entrySet()) {
            Connection connection = entry.getValue();

            if (connection.name.equals(name)) {
                connectionId = entry.getKey();

                break;
            }
        }

        if (connectionId == -1) {
            connectionId = ATOMIC_CONNECTION_ID.getAndIncrement();

            CONNECTION_MAP.putIfAbsent(connectionId, new Connection(name, parameters));
        }

        return connectionId;
    }

    private record Connection(String name, Map<String, ?> parameters) {
    }
}
