/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.security;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;

/**
 * Single source of truth for the "is this user a member of this workspace?" check used by every AI Hub REST and GraphQL
 * endpoint. Centralising the check means a future regression that loosens the membership rule changes one method
 * instead of every controller.
 *
 * <p>
 * Implemented as a static helper rather than a Spring bean so callers do not need to thread a new dependency through
 * their constructors and tests; each controller still injects {@link WorkspaceFacade} for the underlying lookup.
 * </p>
 *
 * <p>
 * In CE {@link WorkspaceFacade#getUserWorkspaces} returns every workspace, so this gate is permissive there; in EE it
 * returns only accessible workspaces, which closes the cross-workspace privilege-escalation vector.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkspaceAccessGuard {

    private WorkspaceAccessGuard() {
    }

    /**
     * Throws {@link ForbiddenException} when {@code userId} is not a member of {@code workspaceId}. The error message
     * is intentionally non-specific so the response shape does not let a caller distinguish "workspace does not exist"
     * from "workspace exists but you cannot reach it" — the workspace id is omitted on purpose so a probe cannot use
     * the response to confirm whether a given id refers to a real workspace.
     */
    public static void verifyUserCanAccessWorkspace(
        WorkspaceFacade workspaceFacade, long userId, long workspaceId) {

        if (!isMember(workspaceFacade, userId, workspaceId)) {
            throw new ForbiddenException("Workspace is not accessible to the current user");
        }
    }

    /**
     * Boolean variant of the membership check, for callers that need to throw a different exception type (e.g. the SSE
     * chat controller throws {@code ResponseStatusException} for HTTP fail-fast). Prefer
     * {@link #verifyUserCanAccessWorkspace(WorkspaceFacade, long, long)} unless you genuinely need a non-default
     * exception or non-throwing flow.
     */
    public static boolean isMember(WorkspaceFacade workspaceFacade, long userId, long workspaceId) {
        return workspaceFacade.getUserWorkspaces(userId)
            .stream()
            .map(Workspace::getId)
            .anyMatch(id -> id != null && id == workspaceId);
    }
}
