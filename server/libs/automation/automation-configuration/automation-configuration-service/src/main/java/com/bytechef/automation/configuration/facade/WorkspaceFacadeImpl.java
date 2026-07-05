/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceFacadeImpl implements WorkspaceFacade {

    private final PermissionService permissionService;
    private final WorkspaceService workspaceService;

    @SuppressFBWarnings("EI")
    public WorkspaceFacadeImpl(PermissionService permissionService, WorkspaceService workspaceService) {
        this.permissionService = permissionService;
        this.workspaceService = workspaceService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin() or isCurrentUser(#id)")
    public List<Workspace> getUserWorkspaces(long id) {
        List<Workspace> workspaces = workspaceService.getWorkspaces();

        if (permissionService.isTenantAdmin()) {
            return workspaces;
        }

        return workspaces.stream()
            .filter(workspace -> permissionService.getMyWorkspaceRole(workspace.getId()) != null)
            .toList();
    }
}
