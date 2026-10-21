/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppInstance;
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
        AppInstance appInstance, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods);

    Optional<AppConnection> fetchBinding(long appInstanceId, String bindingKey);

    /**
     * The connections {@link #bind} accepts for the instance: automation connections in the instance's workspace and
     * environment.
     */
    List<Connection> getBindableConnections(AppInstance appInstance);

    /**
     * Deletes every binding of the instance.
     */
    void deleteBindings(long appInstanceId);

    List<AppConnection> getBindings(long appInstanceId);

    List<AppConnection> getBindingsByConnectionId(long connectionId);

    /**
     * Whether {@link #bind} accepts the connection for the instance.
     */
    boolean isBindable(AppInstance appInstance, long connectionId);

    boolean isConnectionOfComponent(long connectionId, String componentName);

    /**
     * @return {@code true} when a binding was deleted, {@code false} when the instance had no binding under the key
     */
    boolean unbind(long appInstanceId, String bindingKey);
}
