/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The gated management surface for apps. Every method carries its own {@code @PreAuthorize} on the implementation,
 * except {@link #requireAppInEnvironment}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppFacade {

    AppSecretKeyDTO createApp(
        long workspaceId, Environment environment, String name, @Nullable String description,
        AppHostingTarget hostingTarget, ResourceVisibility visibility);

    void deleteApp(long id);

    AppDTO getApp(long id);

    List<AppDTO> getApps(long workspaceId, long environmentId);

    /**
     * Returns when the app exists in the given environment and throws {@code APP_NOT_FOUND} otherwise. It is
     * deliberately unguarded because its only answers are "returns" and {@code APP_NOT_FOUND}, so it reveals nothing
     * beyond what a missing id reveals; callers authorize after it.
     */
    void requireAppInEnvironment(long id, Environment environment);

    /**
     * Updates an app. Every argument except {@code id} is optional: {@code null} keeps the stored value. An empty
     * description or url clears it. A given name keeps the blank and duplicate-name checks, and a given visibility is
     * checked against the visibilities apps support.
     */
    AppDTO updateApp(
        long id, @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility);

    AppDTO updateAppStatus(long id, AppStatus status);

    AppSecretKeyDTO rotateSecretKey(long id);

    List<AppBindingDTO> getBindings(long appId);

    /**
     * The connections {@link #bindConnection} would accept for the app, optionally of one component. {@code usable}
     * marks those with valid credentials the caller may use.
     */
    List<AppBindableConnectionDTO> getBindableConnections(long appId, @Nullable String componentName);

    /**
     * The connections of a workspace in an environment, for the app loop: no parameters, no credentials.
     */
    List<AppWorkspaceConnectionDTO> getConnections(
        long workspaceId, Environment environment, @Nullable String componentName);

    AppBindingDTO bindConnection(
        long appId, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods);

    void unbindConnection(long appId, String bindingKey);

    /**
     * The action and agent-tool names a component version offers, for choosing what to allow on a binding.
     */
    AppComponentOperationsDTO getComponentOperations(String componentName, int componentVersion);
}
