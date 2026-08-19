/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public interface WorkspaceUserRepository extends ListCrudRepository<WorkspaceUser, Long> {

    long countByCustomRoleId(long customRoleId);

    long countByWorkspaceIdAndWorkspaceRole(long workspaceId, int workspaceRole);

    /**
     * Counts the members who hold {@code workspaceRole} in one environment.
     *
     * <p>
     * A row with a null environment applies everywhere, so it counts toward every environment; a row naming an
     * environment counts only toward that one. Counting explicit rows alone would report zero admins for an environment
     * an implicit admin already covers, and counting the implicit row alone would miss per-environment admins entirely.
     *
     * @param environment the {@code Environment} ordinal
     */
    @Query("""
        SELECT COUNT(*) FROM workspace_user
        WHERE workspace_id = :workspaceId
          AND workspace_role = :workspaceRole
          AND (environment = :environment OR environment IS NULL)
        """)
    long countAdminsForEnvironment(long workspaceId, int workspaceRole, int environment);

    void deleteByUserIdAndWorkspaceId(long userId, long workspaceId);

    List<WorkspaceUser> findAllByUserId(long userId);

    List<WorkspaceUser> findAllByWorkspaceId(long workspaceId);

    /**
     * Returns the member's row, and is ambiguous once that member holds per-environment rows: it will match whichever
     * of them the database returns first. Callers that want the implicit row must use
     * {@link #findByUserIdAndWorkspaceIdAndEnvironmentIsNull(long, long)}, and callers that want a specific environment
     * {@link #findByUserIdAndWorkspaceIdAndEnvironment(long, long, int)}. Kept for the many existing callers that
     * predate the environment dimension.
     */
    Optional<WorkspaceUser> findByUserIdAndWorkspaceId(long userId, long workspaceId);

    /**
     * Returns the member's implicit row — the one that applies to every environment — or empty when the member is in
     * explicit mode. The derived {@code IS NULL} predicate matches {@code uk_workspace_user_implicit}'s own predicate,
     * so the partial index serves this lookup.
     */
    Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironmentIsNull(long userId, long workspaceId);

    /**
     * Returns the member's row for one environment, or empty when no row names it. Empty means denied — there is no
     * fallback to the implicit row, because a member in explicit mode has none.
     *
     * @param environment the {@code Environment} ordinal
     */
    Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironment(long userId, long workspaceId, int environment);

    /**
     * Returns every row the member holds in the workspace: exactly one implicit row, or one row per environment they
     * were granted.
     */
    List<WorkspaceUser> findAllByUserIdAndWorkspaceId(long userId, long workspaceId);
}
