/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.platform.connection.domain.Connection;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppConnectionService {

    /**
     * Creates the binding, or replaces the existing one for the same binding key in place.
     */
    AppConnection bind(
        long appId, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods);

    Optional<AppConnection> fetchBinding(long appId, String bindingKey);

    /**
     * The connections {@link #bind} accepts for the app: automation connections in the app's workspace and environment.
     */
    List<Connection> getBindableConnections(long appId);

    List<AppConnection> getBindings(long appId);

    List<AppConnection> getBindingsByConnectionId(long connectionId);

    /**
     * @return {@code true} when a binding was deleted, {@code false} when the app had no binding under the key
     */
    boolean unbind(long appId, String bindingKey);
}
