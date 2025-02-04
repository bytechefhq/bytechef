/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.platform.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectionContext {

    private static final AtomicLong ATOMIC_CONNECTION_ID = new AtomicLong(1);

    private static final ThreadLocal<Map<Long, Connection>> CURRENT_CONNECTION_MAP = ThreadLocal.withInitial(
        HashMap::new);

    public static Map<String, ?> getConnectionParameters(long id) {
        Map<Long, Connection> parameterMap = CURRENT_CONNECTION_MAP.get();

        return parameterMap.get(id).parameters;
    }

    public static long putConnectionParameters(@NonNull String name, @NonNull Map<String, ?> parameters) {
        Map<Long, Connection> parameterMap = CURRENT_CONNECTION_MAP.get();

        long connectionId = -1;

        for (Map.Entry<Long, Connection> entry : parameterMap.entrySet()) {
            Connection connection = entry.getValue();

            if (connection.name.equals(name)) {
                connectionId = entry.getKey();

                break;
            }
        }

        if (connectionId == -1) {
            connectionId = ATOMIC_CONNECTION_ID.getAndIncrement();

            parameterMap.putIfAbsent(connectionId, new Connection(name, parameters));

            CURRENT_CONNECTION_MAP.set(parameterMap);
        }

        return connectionId;
    }

    private record Connection(String name, Map<String, ?> parameters) {
    }
}
