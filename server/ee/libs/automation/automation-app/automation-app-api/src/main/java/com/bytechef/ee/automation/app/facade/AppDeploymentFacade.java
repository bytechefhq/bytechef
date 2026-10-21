/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.dto.AppDeploymentDTO;
import com.bytechef.ee.automation.app.dto.AppVersionDTO;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppDeploymentFacade {

    int DEFAULT_LIST_LIMIT = 50;

    int MAX_LIST_LIMIT = 200;

    /**
     * Asks the provider to stop a deployment that has not reached a terminal status and marks it {@code CANCELED}. A
     * terminal deployment is never changed.
     */
    AppDeploymentDTO cancel(long appInstanceId, long appDeploymentId);

    AppVersionDTO createVersion(
        long appId, String name, @Nullable String gitRef, @Nullable String gitCommitSha,
        @Nullable String description);

    /**
     * Writes a {@code QUEUED} deployment of the version and asks the instance's provider to build and release it. A
     * provider failure leaves the deployment {@code FAILED}; it is returned, not thrown.
     */
    AppDeploymentDTO deploy(long appInstanceId, long appVersionId);

    /**
     * The version of the app with the given name, which is trimmed first. It needs {@code APP_VIEW} on the app, so it
     * serves the app's own workspace; a workspace that hosts an instance of the app reads through
     * {@link #fetchVersionForInstance}.
     */
    Optional<AppVersionDTO> fetchVersion(long appId, String name);

    /**
     * The version, with the given trimmed name, of the app the instance belongs to. It needs {@code APP_VIEW} on the
     * instance, so a workspace that hosts an instance of an app owned by another workspace can resolve the versions it
     * deploys.
     */
    Optional<AppVersionDTO> fetchVersionForInstance(long appInstanceId, String name);

    Optional<AppDeploymentDTO> getCurrentDeployment(long appInstanceId);

    AppDeploymentDTO getDeployment(long appInstanceId, long appDeploymentId);

    List<AppDeploymentDTO> getDeployments(long appInstanceId, int limit);

    /**
     * The newest versions of the app. It needs {@code APP_VIEW} on the app; a workspace that hosts an instance of the
     * app reads through {@link #getVersionsForInstance}.
     */
    List<AppVersionDTO> getVersions(long appId, int limit);

    /**
     * The newest versions of the app the instance belongs to. It needs {@code APP_VIEW} on the instance, so a workspace
     * that hosts an instance of an app owned by another workspace sees the versions it may deploy.
     */
    List<AppVersionDTO> getVersionsForInstance(long appInstanceId, int limit);

    /**
     * Whether a deployment provider is installed for the hosting target, so an instance of it can be deployed once it
     * has a deployment connection.
     */
    boolean isDeploymentProviderAvailable(AppHostingTarget hostingTarget);

    /**
     * Records a {@code READY} deployment made outside ByteChef and makes it the instance's current one. It needs
     * {@code APP_DEPLOY} on the instance only, so a caller who holds it in the workspace that hosts the instance may
     * create a version name on the app this way, even when it may not create a version through {@link #createVersion}:
     * the version is found by name or created.
     */
    AppDeploymentDTO recordDeployment(
        long appInstanceId, String versionName, @Nullable String url, @Nullable String gitCommitSha);

    /**
     * Deploys again, as a new deployment, a version that was once {@code READY} on the instance.
     */
    AppDeploymentDTO rollback(long appInstanceId, long appVersionId);
}
