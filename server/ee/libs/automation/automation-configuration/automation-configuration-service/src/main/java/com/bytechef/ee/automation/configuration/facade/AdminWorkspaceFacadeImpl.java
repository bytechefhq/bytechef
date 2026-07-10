/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AdminWorkspaceFacadeImpl implements AdminWorkspaceFacade {

    private final WorkspaceService workspaceService;

    @SuppressFBWarnings("EI")
    public AdminWorkspaceFacadeImpl(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Workspace createWorkspace(Workspace workspace) {
        return workspaceService.create(workspace);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteWorkspace(long id) {
        workspaceService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Workspace getWorkspace(long id) {
        return workspaceService.getWorkspace(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<Workspace> getWorkspaces() {
        return workspaceService.getWorkspaces();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Workspace updateWorkspace(Workspace workspace) {
        return workspaceService.update(workspace);
    }
}
