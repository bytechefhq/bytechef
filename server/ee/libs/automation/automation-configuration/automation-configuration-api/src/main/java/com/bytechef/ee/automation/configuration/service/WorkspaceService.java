/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("NM")
public interface WorkspaceService extends com.bytechef.automation.configuration.service.WorkspaceService {

    /**
     * Creates a new workspace. Authorization: tenant admin.
     *
     * @param workspace the workspace to create, with a {@code null} id
     * @return the created workspace
     */
    Workspace create(Workspace workspace);

    /**
     * Deletes the workspace and evicts the scope cache entry of every member it had. Authorization: tenant admin.
     * Refuses to delete the default workspace, which every tenant must keep.
     *
     * @param id the workspace to delete
     */
    void delete(long id);

    /**
     * Returns the workspace a project belongs to. Trusted-caller method (no {@code @PreAuthorize}): callers reach it
     * having already authorized against the project, and gating it here would demand a workspace scope the
     * project-level check does not imply.
     *
     * @param projectId the project whose workspace is wanted
     * @return the owning workspace
     */
    Workspace getProjectWorkspace(long projectId);

    /**
     * Returns every workspace in the tenant, unfiltered. Trusted-caller method (no {@code @PreAuthorize}) — the facades
     * above it narrow the result: {@code WorkspaceFacade} to the caller's own memberships, {@code AdminWorkspaceFacade}
     * to tenant admins. Never expose it directly.
     *
     * @return all workspaces in the tenant
     */
    List<Workspace> getWorkspaces();

    /**
     * Returns the workspace. Authorization: {@code WORKSPACE_VIEW} on it.
     *
     * @param id the workspace to read
     * @return the workspace
     */
    Workspace getWorkspace(long id);

    /**
     * Returns the workspace's display name. Trusted-caller method (no {@code @PreAuthorize}) so that a custom role
     * carrying only {@code WORKSPACE_MEMBER_MANAGE} can name the workspace in the "you were added" mail without also
     * holding {@code WORKSPACE_VIEW}. External entry points (REST, GraphQL) must enforce their own authorization.
     *
     * @param id the workspace id
     * @return the workspace's display name
     */
    String getWorkspaceName(long id);

    /**
     * Updates the workspace. Authorization: {@code WORKSPACE_MANAGE} on it.
     *
     * @param workspace the workspace to update, with a non-{@code null} id
     * @return the updated workspace
     */
    Workspace update(Workspace workspace);

    /**
     * Whether a workspace with this id exists. Trusted-caller method (no {@code @PreAuthorize}) like
     * {@link #getWorkspaceName(long)}: it hands back no part of the entity. Lets a write naming a workspace reject an
     * unknown id as caller error instead of failing on the foreign key as a 500.
     *
     * @param id the workspace id to check
     * @return {@code true} if a workspace with this id exists
     */
    boolean workspaceExists(long id);

}
