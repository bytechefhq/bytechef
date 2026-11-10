/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionService {

    default void create(long connectedUserId, long connectionId) {
        create(connectedUserId, connectionId, null);
    }

    void create(long connectedUserId, long connectionId, @Nullable String provisionedBy);

    void deleteByConnectionId(long connectionId);

    boolean existsByConnectionId(long connectionId);

    List<Long> getConnectionIds(long connectedUserId);

    Set<Long> getConnectionIdsIn(Set<Long> connectionIds);

    Set<Long> getSharedConnectionIds();

    void updateShared(long connectionId, boolean shared);
}
