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
     * Counts the distinct members who hold {@code workspaceRole} in one environment.
     *
     * <p>
     * A row with a null environment applies everywhere, so it counts toward every environment; a row naming an
     * environment counts only toward that one. Counting explicit rows alone would report zero admins for an environment
     * an implicit admin already covers, and counting the implicit row alone would miss per-environment admins entirely.
     *
     * <p>
     * Distinct members, never rows: were a member ever to hold an implicit ADMIN row beside an ADMIN row for this
     * environment, a row count would report them as two admins and the environment could be stranded.
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

    /**
     * Whether the member holds any row naming an environment in the workspace.
     */
    boolean existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(long userId, long workspaceId);

    List<WorkspaceUser> findAllByUserId(long userId);

    List<WorkspaceUser> findAllByWorkspaceId(long workspaceId);

    /**
     * Returns the member's implicit row — the one that applies to every environment — or empty when the member is in
     * explicit mode. The derived {@code IS NULL} predicate matches {@code uk_workspace_user_implicit}'s own predicate,
     * so the partial index serves this lookup.
     */
    Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironmentIsNull(long userId, long workspaceId);

    /**
     * Returns the member's row for one environment, or empty when no row names it.
     *
     * @param environment the {@code Environment} ordinal
     */
    Optional<WorkspaceUser> findByUserIdAndWorkspaceIdAndEnvironment(long userId, long workspaceId, int environment);

    /**
     * Returns every row the member holds in the workspace: exactly one implicit row, or one row per environment they
     * were granted.
     */
    List<WorkspaceUser> findAllByUserIdAndWorkspaceId(long userId, long workspaceId);

    /**
     * Locks the workspace row until the current transaction ends, serializing membership writes in the workspace.
     */
    @Query("SELECT id FROM workspace WHERE id = :workspaceId FOR UPDATE")
    Optional<Long> lockWorkspace(long workspaceId);
}
