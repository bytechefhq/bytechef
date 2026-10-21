/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppOperationType;
import com.bytechef.platform.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A binding with its connection's details. {@code componentVersion} is the latest version of the connection's
 * component, the one app executions run, or null when the component cannot be resolved (deleted, dropped in an upgrade,
 * or absent from the component index).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppBindingDTO(
    long id, long appId, String bindingKey, long connectionId, String connectionName, String componentName,
    int connectionVersion, @Nullable Integer componentVersion, List<String> actionNames, List<String> toolNames,
    boolean proxyEnabled, List<String> proxyAllowedMethods) {

    public static AppBindingDTO of(
        AppConnection appConnection, Connection connection, @Nullable Integer componentVersion) {

        return new AppBindingDTO(
            appConnection.getId(), appConnection.getAppId(), appConnection.getBindingKey(),
            appConnection.getConnectionId(), connection.getName(), connection.getComponentName(),
            connection.getConnectionVersion(), componentVersion,
            appConnection.getOperationNames(AppOperationType.ACTION),
            appConnection.getOperationNames(AppOperationType.TOOL), appConnection.isProxyEnabled(),
            appConnection.getProxyAllowedMethodList());
    }
}
