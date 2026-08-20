/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;

/**
 * Visibility and named-user grants for automation projects (resource-visibility phase 2). Authorization is
 * owner-or-admin, annotated on the implementation so it protects every caller; the GraphQL controller only maps
 * arguments. Validation order is authorize → project-in-workspace → grantee-in-workspace, and every validation failure
 * collapses to {@code ProjectErrorType.INVALID_PROJECT} so a caller cannot enumerate user ids.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectSharingFacade {

    List<Long> getProjectGrants(long workspaceId, long projectId);

    void grantProjectAccess(long workspaceId, long projectId, long userId);

    void revokeProjectAccess(long workspaceId, long projectId, long userId);

    void setProjectVisibility(long workspaceId, long projectId, ResourceVisibility visibility);
}
