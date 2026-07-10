/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import java.util.List;

/**
 * Admin-only facade for the workspace-management REST surface. Hosts the coarse {@code ROLE_ADMIN} guard so it applies
 * to every caller of the facade rather than only the REST controller. The finer-grained per-workspace permission checks
 * (tenant-admin for create/delete, workspace VIEWER/ADMIN for read/update) remain on
 * {@link com.bytechef.ee.automation.configuration.service.WorkspaceService}
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AdminWorkspaceFacade {

    Workspace createWorkspace(Workspace workspace);

    void deleteWorkspace(long id);

    Workspace getWorkspace(long id);

    List<Workspace> getWorkspaces();

    Workspace updateWorkspace(Workspace workspace);
}
