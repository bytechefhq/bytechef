/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppConnectionRepository;
import com.bytechef.ee.automation.app.util.AppBindingKeys;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppConnectionServiceImpl implements AppConnectionService {

    private static final Set<String> PROXY_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");

    private final AppConnectionRepository appConnectionRepository;
    private final AppService appService;
    private final ConnectionService connectionService;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings("EI")
    public AppConnectionServiceImpl(
        AppConnectionRepository appConnectionRepository, AppService appService, ConnectionService connectionService,
        WorkspaceConnectionService workspaceConnectionService) {

        this.appConnectionRepository = appConnectionRepository;
        this.appService = appService;
        this.connectionService = connectionService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public AppConnection bind(
        long appId, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods) {

        if (!AppBindingKeys.isValidKey(bindingKey)) {
            throw new ConfigurationException(
                "Binding key must match " + AppBindingKeys.keyPattern(), AppErrorType.APP_BINDING_KEY_INVALID);
        }

        checkFlatKeyUnique(appId, bindingKey);

        App app = appService.getApp(appId);
        Connection connection = connectionService.getConnection(connectionId);

        if (!isBindable(app, connection, getWorkspaceConnectionIds(app))) {
            throw new ConfigurationException(
                "Connection %s cannot be bound to app %s: it must be an automation connection in the app's workspace and environment"
                    .formatted(connection.getId(), app.getId()),
                AppErrorType.APP_CONNECTION_NOT_BINDABLE);
        }

        List<String> methods = proxyAllowedMethods.stream()
            .map(String::toUpperCase)
            .toList();

        for (String method : methods) {
            Validate.isTrue(PROXY_METHODS.contains(method), "Unsupported proxy method: %s", method);
        }

        AppConnection appConnection = appConnectionRepository.findByAppIdAndBindingKey(appId, bindingKey)
            .orElseGet(AppConnection::new);

        appConnection.setAppId(appId);
        appConnection.setBindingKey(bindingKey);
        appConnection.setConnectionId(connectionId);
        appConnection.setOperations(actionNames, toolNames);
        appConnection.setProxyEnabled(proxyEnabled);
        appConnection.setProxyAllowedMethodList(proxyEnabled ? methods : List.of());

        return appConnectionRepository.save(appConnection);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppConnection> fetchBinding(long appId, String bindingKey) {
        return appConnectionRepository.findByAppIdAndBindingKey(appId, bindingKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getBindableConnections(long appId) {
        App app = appService.getApp(appId);

        Set<Long> workspaceConnectionIds = getWorkspaceConnectionIds(app);

        if (workspaceConnectionIds.isEmpty()) {
            return List.of();
        }

        return connectionService.getConnections(new ArrayList<>(workspaceConnectionIds))
            .stream()
            .filter(connection -> isBindable(app, connection, workspaceConnectionIds))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppConnection> getBindings(long appId) {
        return appConnectionRepository.findAllByAppIdOrderByBindingKey(appId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppConnection> getBindingsByConnectionId(long connectionId) {
        return appConnectionRepository.findAllByConnectionId(connectionId);
    }

    @Override
    public boolean unbind(long appId, String bindingKey) {
        Optional<AppConnection> binding = appConnectionRepository.findByAppIdAndBindingKey(appId, bindingKey);

        binding.ifPresent(appConnectionRepository::delete);

        return binding.isPresent();
    }

    /**
     * Binding keys that differ only in separators ({@code support-crm} and {@code support_crm}) normalize to the same
     * flat tool name, which has to stay unique per app. Rebinding a key itself is fine.
     */
    private void checkFlatKeyUnique(long appId, String bindingKey) {
        String flatKey = AppBindingKeys.toFlatKey(bindingKey);

        boolean collides = appConnectionRepository.findAllByAppIdOrderByBindingKey(appId)
            .stream()
            .map(AppConnection::getBindingKey)
            .anyMatch(
                existingKey -> !existingKey.equals(bindingKey) && AppBindingKeys.toFlatKey(existingKey)
                    .equals(flatKey));

        if (collides) {
            throw new ConfigurationException(
                "Binding key '%s' collides with an existing binding key of the app: both normalize to '%s'"
                    .formatted(bindingKey, flatKey),
                AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS);
        }
    }

    private Set<Long> getWorkspaceConnectionIds(App app) {
        return workspaceConnectionService.getWorkspaceConnections(Validate.notNull(app.getWorkspaceId(), "workspaceId"))
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * A connection is bindable when it is an automation connection in the app's environment and a member of the app's
     * workspace. Virtual AI-provider connections have no workspace_connection row, so the membership check excludes
     * them as well. This is the one definition of bindable: {@link #bind} enforces it and
     * {@link #getBindableConnections} lists by it.
     */
    private static boolean isBindable(App app, Connection connection, Set<Long> workspaceConnectionIds) {
        return workspaceConnectionIds.contains(connection.getId()) && connection.getType() == PlatformType.AUTOMATION
            && connection.getEnvironmentId() == app.getEnvironmentId();
    }
}
