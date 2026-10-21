/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppDeploymentService {

    /**
     * Cancels the deployment in one read: the row that is checked is the row that is saved, so its version guards the
     * decision. Answers empty, without writing, when the row carries a provider deployment id other than
     * {@code handledProviderDeploymentId}, because that provider deployment was not canceled at the provider.
     */
    Optional<AppDeployment> cancel(long id, @Nullable String handledProviderDeploymentId);

    /**
     * Locks the instance row and refuses with {@code APP_DEPLOYMENT_IN_PROGRESS} while the instance has a deployment
     * that is not terminal.
     */
    void checkNoDeploymentInProgress(long appInstanceId);

    /**
     * Refuses a deployment that is not terminal with {@code APP_DEPLOYMENT_IN_PROGRESS} while another deployment of the
     * same instance is not terminal either.
     */
    AppDeployment create(AppDeployment appDeployment);

    AppDeployment getAppDeployment(long id);

    List<AppDeployment> getAppDeployments(long appInstanceId, int limit);

    List<AppDeployment> getNonTerminalAppDeployments();

    boolean hasReadyAppDeployment(long appInstanceId, long appVersionId);

    AppDeployment updateStatus(
        long id, AppDeploymentStatus status, @Nullable String url, @Nullable String errorMessage);

    AppDeployment updateStatus(
        long id, @Nullable String providerDeploymentId, AppDeploymentStatus status, @Nullable String url,
        @Nullable String errorMessage);
}
