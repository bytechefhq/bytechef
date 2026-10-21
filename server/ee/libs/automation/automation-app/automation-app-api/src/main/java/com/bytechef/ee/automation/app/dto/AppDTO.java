/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The management view of an app and its instances. Instances in other workspaces than the app's carry no key hint.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppDTO(
    long id, String uuid, String name, @Nullable String description, long workspaceId, long ownerUserId,
    @Nullable Long categoryId, List<Long> tagIds, boolean hasIcon, AppStatus status, ResourceVisibility visibility,
    boolean portalDiscoverable, List<AppInstanceDTO> instances, List<ForeignAppInstanceDTO> foreignInstances,
    String createdBy, Instant createdDate,
    String lastModifiedBy, Instant lastModifiedDate, int version) {

    public static AppDTO of(
        App app, List<AppInstanceDTO> instances, List<ForeignAppInstanceDTO> foreignInstances) {

        return new AppDTO(
            app.getId(), app.getUuid(), app.getName(), app.getDescription(), app.getWorkspaceId(),
            app.getOwnerUserId(), app.getCategoryId(), app.getTagIds(), app.getIcon() != null, app.getStatus(),
            app.getVisibility(),
            app.isPortalDiscoverable(), List.copyOf(instances), List.copyOf(foreignInstances), app.getCreatedBy(),
            app.getCreatedDate(),
            app.getLastModifiedBy(), app.getLastModifiedDate(), app.getVersion());
    }
}
