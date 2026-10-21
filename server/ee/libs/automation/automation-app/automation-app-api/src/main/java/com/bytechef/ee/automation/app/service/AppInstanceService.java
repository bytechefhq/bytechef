/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppInstanceService {

    /**
     * The created instance together with its plaintext secret key, which is shown once and never stored.
     */
    @SuppressFBWarnings("EI")
    record CreatedAppInstance(AppInstance appInstance, String secretKey) {

        @Override
        public String toString() {
            return "CreatedAppInstance[appInstance=" + appInstance + ", secretKey=[REDACTED]]";
        }
    }

    /**
     * The re-keyed instance together with its new plaintext secret key, which is shown once and never stored.
     */
    @SuppressFBWarnings("EI")
    record RotatedKey(AppInstance appInstance, String secretKey) {

        @Override
        public String toString() {
            return "RotatedKey[appInstance=" + appInstance + ", secretKey=[REDACTED]]";
        }
    }

    /**
     * Generates the uuid and the secret key and starts the instance as {@link AppInstanceStatus#ACTIVE} with the
     * sign-in mode the given instance carries. The owner and the workspace are required, and the name is unique per app
     * and environment.
     */
    CreatedAppInstance create(AppInstance appInstance);

    void delete(long id);

    Optional<AppInstance> fetchAppInstance(long id);

    /**
     * Hashes the key and looks the instance up by it, whatever the instance's status.
     */
    Optional<AppInstance> fetchAppInstanceBySecretKey(String secretKey);

    Optional<AppInstance> fetchAppInstanceByUuid(String uuid);

    /**
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_INSTANCE_NOT_FOUND} when there is no such
     *                                                       instance
     */
    AppInstance getAppInstance(long id);

    /**
     * Returns the app's instances ordered by id, so the first is the one created with the app.
     */
    List<AppInstance> getAppInstances(long appId);

    /**
     * Returns the instances of all the given apps in one read, ordered by id.
     */
    List<AppInstance> getAppInstancesByAppIds(Collection<Long> appIds);

    List<AppInstance> getAppInstancesByDeploymentConnectionId(long deploymentConnectionId);

    /**
     * Returns the instances that live in the workspace in the environment, whichever workspace owns their app, ordered
     * by id.
     */
    List<AppInstance> getWorkspaceAppInstances(long workspaceId, Environment environment);

    RotatedKey rotateSecretKey(long id);

    /**
     * Best-effort last-used mark, written at most once per minute.
     */
    void touchSecretKeyLastUsedDate(long id);

    /**
     * Updates name, hosting target and url. A {@code null} field keeps the stored value; an empty url clears it. A
     * given name must not be blank and must be unused by the app's other instances in the instance's environment.
     */
    AppInstance update(long id, @Nullable String name, @Nullable AppHostingTarget hostingTarget, @Nullable String url);

    /**
     * Changes the status. {@link AppInstanceStatus#TERMINATED} is final.
     *
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_INSTANCE_TERMINATED} when the instance is
     *                                                       terminated and {@code status} is another status
     */
    AppInstance updateStatus(long id, AppInstanceStatus status);

    AppInstance updateCurrentAppDeployment(long id, long appDeploymentId);

    AppInstance updateConfiguration(long id, Map<String, ?> configuration);

    AppInstance updateDeploymentSettings(
        long id, @Nullable Long deploymentConnectionId, Map<String, ?> deploymentSettings);

    AppInstance updateVisibility(long id, ResourceVisibility visibility);

    /**
     * Stores the sign-in mode and the OAuth2 client id of the instance's registered client, {@code null} when it has
     * none.
     */
    AppInstance updateUserAuth(long id, AppUserAuthMode userAuthMode, @Nullable String registeredClientId);
}
