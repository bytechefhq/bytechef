/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionFacade {

    long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO);

    /**
     * Deletes a connection owned by the connected user. Throws {@link java.util.NoSuchElementException} when
     * {@code connectionId} is not owned by {@code connectedUserId} (never surfaces as a permission error, so a
     * connection id cannot be enumerated by a connected user probing ids that belong to someone else).
     */
    void deleteConnectedUserConnection(long connectedUserId, long connectionId);

    /**
     * @param componentName the component to filter by, or {@code null} to return connections across every component
     */
    List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds);

    /**
     * Replaces the credentials of a connection owned by the connected user, keeping its id. Throws
     * {@link java.util.NoSuchElementException} when {@code connectionId} is not owned by {@code connectedUserId}.
     */
    void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters);
}
