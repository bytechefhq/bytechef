/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Collection;
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
     * Generates the uuid and starts the app as {@link AppStatus#DRAFT}; the name is unique per workspace.
     */
    App create(App app);

    /**
     * Deletes the app; its instances and their bindings go with it.
     */
    void delete(long id);

    Optional<App> fetchApp(long id);

    /**
     * @throws com.bytechef.exception.ConfigurationException with {@code APP_NOT_FOUND} when there is no such app
     */
    App getApp(long id);

    List<App> getApps(long workspaceId);

    List<App> getAppsByIds(Collection<Long> ids);

    /**
     * Updates name and description. A {@code null} field keeps the stored value; an empty description clears it. A
     * given name must not be blank and must be unused in the app's workspace.
     */
    App update(long id, @Nullable String name, @Nullable String description);

    /**
     * Sets the category of the app; {@code null} clears it. The category must exist.
     */
    App updateCategory(long id, @Nullable Long categoryId);

    /**
     * Replaces the tags of the app with the given tag ids; an empty list clears them. The tags must exist.
     */
    App updateTags(long id, List<Long> tagIds);

    /**
     * Sets the stored icon file of the app; {@code null} clears it. The file itself is not touched.
     */
    App updateIcon(long id, @Nullable FileEntry icon);

    App updatePortalDiscoverable(long id, boolean portalDiscoverable);

    App updateStatus(long id, AppStatus status);

    App updateVisibility(long id, ResourceVisibility visibility);
}
