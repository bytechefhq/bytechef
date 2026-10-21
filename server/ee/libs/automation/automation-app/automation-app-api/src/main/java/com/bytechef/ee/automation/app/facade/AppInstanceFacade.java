/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The gated management surface for app instances: their fields, status, secret key and connection bindings. Every
 * method carries its own {@code @PreAuthorize} on the implementation, except {@link #requireAppInstanceInEnvironment}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppInstanceFacade {

    /**
     * Creates an instance of the app in the given workspace and environment, owned by the caller, and returns it with
     * its plaintext secret key. A {@code null} sign-in mode means {@link AppUserAuthMode#REQUIRED} when the
     * authorization server is on and {@link AppUserAuthMode#NONE} when it is not; any mode but
     * {@link AppUserAuthMode#NONE} needs the authorization server.
     */
    AppSecretKeyDTO createAppInstance(
        long appId, long workspaceId, Environment environment, String name, AppHostingTarget hostingTarget,
        ResourceVisibility visibility, @Nullable AppUserAuthMode userAuthMode);

    void deleteAppInstance(long id);

    AppInstanceDTO getAppInstance(long id);

    /**
     * The instances the workspace hosts in the environment of apps owned by other workspaces, limited to those the
     * caller may see.
     */
    List<AppInstanceDTO> getHostedAppInstances(long workspaceId, Environment environment);

    /**
     * Throws {@code APP_INSTANCE_NOT_FOUND} when the instance does not exist, lives in another environment or is one
     * the caller may not view ({@code APP_VIEW} on the instance). It carries no {@code @PreAuthorize} so that these
     * three cases answer alike instead of one of them answering access denied; callers pass the id only to guarded
     * calls, which still refuse a caller who may view the instance but not manage it.
     */
    void requireAppInstanceInEnvironment(long id, Environment environment);

    /**
     * Updates an instance. Every argument except {@code id} is optional: {@code null} keeps the stored value. An empty
     * url clears it. Visibility is set through {@code AppSharingFacade}.
     */
    AppInstanceDTO updateAppInstance(
        long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url);

    /**
     * Updates an instance's fields and, when {@code configuration} is not {@code null}, replaces its configuration, in
     * one transaction: a refused field leaves the configuration as it was. The configuration must be a JSON object of
     * at most 64 KB when serialized, and is checked before anything is written.
     */
    AppInstanceDTO updateAppInstance(
        long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url,
        @Nullable Object configuration);

    /**
     * Replaces the instance's configuration. It must be a JSON object of at most 64 KB when serialized.
     */
    AppInstanceDTO updateAppInstanceConfiguration(long id, Object configuration);

    /**
     * Changes the status. {@link AppInstanceStatus#TERMINATED} is final: it removes the instance's bindings and keeps
     * its row.
     */
    AppInstanceDTO updateAppInstanceStatus(long id, AppInstanceStatus status);

    /**
     * Sets the connection and the settings the instance's deployment provider uses. The connection must be one
     * {@link #bindConnection} would accept, and the provider must accept the settings. A {@code null} connection with
     * {@code null} or empty settings clears both; a {@code null} connection with settings is refused.
     */
    AppInstanceDTO updateAppInstanceDeploymentSettings(
        long id, @Nullable Long deploymentConnectionId, @Nullable Map<String, ?> deploymentSettings);

    /**
     * Sets the sign-in mode. A mode that signs users in registers the instance's OAuth2 client once the instance has a
     * url; {@link AppUserAuthMode#NONE} deletes the client and revokes its tokens.
     */
    AppInstanceDTO updateAppInstanceUserAuth(long id, AppUserAuthMode userAuthMode);

    AppSecretKeyDTO rotateSecretKey(long id);

    List<AppBindingDTO> getBindings(long id);

    /**
     * The connections {@link #bindConnection} would accept for the instance, optionally of one component.
     * {@code usable} marks those with valid credentials the caller may use.
     */
    List<AppBindableConnectionDTO> getBindableConnections(long id, @Nullable String componentName);

    AppBindingDTO bindConnection(
        long id, String bindingKey, long connectionId, List<String> actionNames, List<String> toolNames,
        boolean proxyEnabled, List<String> proxyAllowedMethods);

    void unbindConnection(long id, String bindingKey);
}
