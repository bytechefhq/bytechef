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

    /**
     * Counts the distinct members who hold {@code workspaceRole} anywhere in the workspace.
     *
     * <p>
     * Distinct members, never rows: a member in explicit mode holds one row per environment they were granted, so one
     * admin who holds ADMIN in three environments is three rows. A row count therefore reads three admins where there
     * is one, and the last-admin guard built on it would wave through the removal that empties the workspace of admins
     * — turning what used to be an {@code IncorrectResultSizeDataAccessException} into a lockout.
     *
     * @param workspaceRole the {@code WorkspaceRole} ordinal
     */
    @Query("""
        SELECT COUNT(DISTINCT user_id) FROM workspace_user
        WHERE workspace_id = :workspaceId
          AND workspace_role = :workspaceRole
        """)
    long countDistinctMembersWithWorkspaceRole(long workspaceId, int workspaceRole);

    /**
     * Counts the distinct members who hold {@code workspaceRole} in one environment.
     *
     * <p>
     * A row with a null environment applies everywhere, so it counts toward every environment; a row naming an
     * environment counts only toward that one. Counting explicit rows alone would report zero admins for an environment
     * an implicit admin already covers, and counting the implicit row alone would miss per-environment admins entirely.
     *
     * <p>
     * Distinct members for the same reason as {@link #countDistinctMembersWithWorkspaceRole(long, int)}: only the
     * service layer keeps a member out of both modes at once, and were a member ever to hold an implicit ADMIN row
     * beside an ADMIN row for this environment, a row count would report them as two admins and the environment could
     * be stranded.
     *
     * @param environment the {@code Environment} ordinal
     */
    @Query("""
        SELECT COUNT(DISTINCT user_id) FROM workspace_user
        WHERE workspace_id = :workspaceId
          AND workspace_role = :workspaceRole
          AND (environment = :environment OR environment IS NULL)
        """)
    long countAdminsForEnvironment(long workspaceId, int workspaceRole, int environment);

    /**
     * Deletes every row the member holds in the workspace — the implicit one and each per-environment one — because
     * neither property of the derived query names the environment.
     */
    void deleteByUserIdAndWorkspaceId(long userId, long workspaceId);

    /**
     * Deletes every row the user holds anywhere, in every workspace and every environment. Used by the user delete: the
     * {@code user_id} foreign key exists only on monolithic deployments, so nothing else would take these rows with the
     * account.
     */
    void deleteByUserId(long userId);

    /**
     * Whether the member holds any row in the workspace, in either mode. This is the membership question: a member in
     * explicit mode is a member of the workspace even though they hold no implicit row. It is deliberately not a
     * single-result finder — a member in explicit mode holds one row per environment they were granted, so no
     * {@code Optional}-returning query over {@code (userId, workspaceId)} alone can be written without risking
     * {@code IncorrectResultSizeDataAccessException}.
     */
    boolean existsByUserIdAndWorkspaceId(long userId, long workspaceId);

    List<WorkspaceUser> findAllByUserId(long userId);

    List<WorkspaceUser> findAllByWorkspaceId(long workspaceId);

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
