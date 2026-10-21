/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.AppTagDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The gated management surface for the fields of an app. Instances, their bindings and their keys are managed through
 * {@link AppInstanceFacade}. Every method carries its own {@code @PreAuthorize} on the implementation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppFacade {

    /**
     * Creates the app and its first instance in the given workspace and environment, owned by the app's owner, and
     * returns that instance with its plaintext secret key. The first instance takes the sign-in mode as
     * {@link AppInstanceFacade#createAppInstance} does: {@code null} means the mode {@link #isUserAuthAvailable}
     * decides. A category is resolved as {@link #updateApp} resolves it; {@code null} or an empty name means none.
     */
    AppSecretKeyDTO createApp(
        long workspaceId, Environment environment, String name, @Nullable String description,
        AppHostingTarget hostingTarget, ResourceVisibility visibility, @Nullable AppUserAuthMode userAuthMode,
        @Nullable AppCategoryDTO category, boolean portalDiscoverable);

    void deleteApp(long id);

    void deleteAppIcon(long id);

    AppDTO getApp(long id);

    /**
     * The app's icon with the content type detected from its bytes, or empty when the app has none.
     */
    Optional<AppIconDTO> getAppIcon(long id);

    /**
     * The categories the visible apps of a workspace use, sorted by name, for the app dialog to offer.
     */
    List<AppCategoryDTO> getAppCategories(long workspaceId);

    /**
     * The tags the visible apps of a workspace use, sorted by name, for the app list to filter by.
     */
    List<AppTagDTO> getAppTags(long workspaceId);

    List<AppDTO> getApps(long workspaceId);

    /**
     * Whether app end-user sign-in can be provided, that is whether the authorization server is on. Creating an app or
     * an instance without a sign-in mode gives {@link AppUserAuthMode#REQUIRED} when it is and
     * {@link AppUserAuthMode#NONE} when it is not.
     */
    boolean isUserAuthAvailable(long workspaceId);

    /**
     * Updates an app. Every argument except {@code id} is optional: {@code null} keeps the stored value. An empty
     * description clears it. A given name keeps the blank and duplicate-name checks. Visibility is set through
     * {@link AppSharingFacade}. A category with an id attaches that existing category, ignores the name and fails with
     * {@code APP_CATEGORY_INVALID} when there is none; without an id its name attaches the category of that name,
     * created when there is none, and an empty name clears the category. {@code portalDiscoverable} may be set in any
     * status; it shows the app in the App Portal only while the app is {@code ACTIVE}.
     */
    AppDTO updateApp(
        long id, @Nullable String name, @Nullable String description, @Nullable AppCategoryDTO category,
        @Nullable Boolean portalDiscoverable);

    /**
     * Stores a PNG, JPEG or WebP image of at most 256 KB as the app's icon and deletes the file of the icon it
     * replaces. The type is detected from the content; anything else fails with {@code APP_ICON_INVALID} before
     * anything is stored.
     */
    void updateAppIcon(long id, InputStream inputStream);

    /**
     * Replaces the tags of the app; an empty list clears them. A tag with an id attaches that existing tag and never
     * renames it; without an id its name attaches the tag of that name, created when there is none.
     */
    AppDTO updateAppTags(long id, List<AppTagDTO> tags);

    AppDTO updateAppStatus(long id, AppStatus status);

    /**
     * The connections of a workspace in an environment, for the app loop: no parameters, no credentials.
     */
    List<AppWorkspaceConnectionDTO> getConnections(
        long workspaceId, Environment environment, @Nullable String componentName);

    /**
     * The action and agent-tool names a component version offers, for choosing what to allow on a binding.
     */
    AppComponentOperationsDTO getComponentOperations(String componentName, int componentVersion);
}
