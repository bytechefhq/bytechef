/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the platform-side {@link WorkspaceMembershipAssigner} seam so the tenant invite can place a freshly
 * provisioned user into workspaces. Workspace membership is an automation concept and the platform user modules cannot
 * see it, so the call inverts through the interface declared there.
 *
 * <p>
 * Delegates to {@link WorkspaceUserService#addWorkspaceUser} rather than writing rows directly, so the already-a-member
 * guard and the scope-cache eviction apply to invited users exactly as they do to users added by hand.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class WorkspaceMembershipAssignerImpl implements WorkspaceMembershipAssigner {

    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceMembershipAssignerImpl(WorkspaceUserService workspaceUserService) {
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    public void assign(long userId, List<WorkspaceAssignment> assignments) {
        for (WorkspaceAssignment assignment : assignments) {
            workspaceUserService.addWorkspaceUser(userId, assignment.workspaceId(), parseRole(assignment.roleName()));
        }
    }

    /**
     * Resolves a role name to its enum. The name arrives as a {@code String} because the seam cannot reference
     * {@link WorkspaceRole}; an unknown one is a caller error rather than something to default away, since silently
     * substituting a role would hand out an access level nobody asked for.
     */
    private static WorkspaceRole parseRole(String roleName) {
        try {
            return WorkspaceRole.valueOf(roleName);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                "Unknown workspace role '" + roleName + "'", WorkspaceUserErrorType.INVALID_WORKSPACE_ROLE);
        }
    }
}
