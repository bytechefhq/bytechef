/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppService {

    /**
     * The created app together with its plaintext secret key, which is shown once and never stored.
     */
    @SuppressFBWarnings("EI")
    record CreatedApp(App app, String secretKey) {

        @Override
        public String toString() {
            return "CreatedApp[app=" + app + ", secretKey=[REDACTED]]";
        }
    }

    /**
     * The re-keyed app together with its new plaintext secret key, which is shown once and never stored.
     */
    @SuppressFBWarnings("EI")
    record RotatedKey(App app, String secretKey) {

        @Override
        public String toString() {
            return "RotatedKey[app=" + app + ", secretKey=[REDACTED]]";
        }
    }

    /**
     * Generates the uuid and the secret key; the name is unique per workspace and environment.
     */
    CreatedApp create(App app);

    void delete(long id);

    Optional<App> fetchApp(long id);

    /**
     * Hashes the key and looks the app up by it, whatever the app's status.
     */
    Optional<App> fetchAppBySecretKey(String secretKey);

    /**
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_NOT_FOUND} when there is no such app
     */
    App getApp(long id);

    List<App> getApps(long workspaceId, Environment environment);

    RotatedKey rotateSecretKey(long id);

    /**
     * Best-effort last-used mark, written at most once per minute.
     */
    void touchSecretKeyLastUsedDate(long id);

    /**
     * Updates name, description, hosting target, url, last deployed date and visibility. A {@code null} field keeps the
     * stored value; an empty description or url clears it. A given name must not be blank and must be unused in the
     * app's workspace and environment.
     */
    App update(
        long id, @Nullable String name, @Nullable String description, @Nullable AppHostingTarget hostingTarget,
        @Nullable String url, @Nullable Instant lastDeployedDate, @Nullable ResourceVisibility visibility);

    App updateStatus(long id, AppStatus status);
}
