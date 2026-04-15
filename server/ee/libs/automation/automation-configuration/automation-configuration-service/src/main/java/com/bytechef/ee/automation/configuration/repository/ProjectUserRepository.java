/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Repository
@ConditionalOnEEVersion
public interface ProjectUserRepository extends ListCrudRepository<ProjectUser, Long> {

    List<ProjectUser> findAllByProjectId(long projectId);

    List<ProjectUser> findAllByUserId(long userId);

    Optional<ProjectUser> findByProjectIdAndUserId(long projectId, long userId);

    void deleteByProjectIdAndUserId(long projectId, long userId);

    /**
     * Deletes every {@code project_user} row for the given user whose project belongs to the specified workspace. Used
     * when a workspace member is removed: without this cascade, stale {@code project_user} rows would keep granting
     * project-level access via the permission cache on the next check. The subquery scopes the delete to the user's
     * rows only — neighboring users' memberships are untouched.
     */
    @Modifying
    @Query("""
            DELETE FROM project_user
            WHERE user_id = :userId
              AND project_id IN (SELECT id FROM project WHERE workspace_id = :workspaceId)
        """)
    int deleteByUserIdAndWorkspaceId(@Param("userId") long userId, @Param("workspaceId") long workspaceId);

    /**
     * Returns the project ids the given user is a member of within the specified workspace. Callers snapshot this list
     * before {@link #deleteByUserIdAndWorkspaceId(long, long)} so they can evict the matching cache entries — querying
     * after the delete returns an empty set, leaving stale {@code (userId, projectId)} ALLOW decisions in the cache
     * until the TTL expires.
     */
    @Query("""
            SELECT pu.project_id
            FROM project_user pu
            JOIN project p ON p.id = pu.project_id
            WHERE pu.user_id = :userId
              AND p.workspace_id = :workspaceId
        """)
    List<Long> findProjectIdsByUserIdAndWorkspaceId(
        @Param("userId") long userId, @Param("workspaceId") long workspaceId);

    long countByCustomRoleId(long customRoleId);

    long countByProjectId(long projectId);

    long countByProjectIdAndProjectRole(long projectId, Integer projectRole);

    /**
     * Counts effective administrators of a project: members with the built-in ADMIN role OR members whose custom role
     * grants the given admin scope (typically {@code PROJECT_MANAGE_USERS}). Used by the last-admin guard — demoting or
     * removing the only effective admin would brick the project by making role management unreachable.
     */
    @Query("""
            SELECT COUNT(DISTINCT pu.id)
            FROM project_user pu
            LEFT JOIN custom_role_scope crs
              ON crs.custom_role_id = pu.custom_role_id AND crs.scope = :scopeName
            WHERE pu.project_id = :projectId
              AND (pu.project_role = :builtInAdminOrdinal OR crs.custom_role_id IS NOT NULL)
        """)
    long countEffectiveAdmins(
        @Param("projectId") long projectId, @Param("builtInAdminOrdinal") int builtInAdminOrdinal,
        @Param("scopeName") String scopeName);

    /**
     * Returns the subset of {@code projectIds} in which {@code userId} is an effective admin AND the total
     * effective-admin count for that project is {@code <= 1} — i.e., removing the user would orphan the project.
     * Performs the check in a single round-trip (instead of two queries per project) so workspace-level cascades across
     * many projects stay bounded.
     */
    @Query("""
            SELECT pu.project_id
            FROM project_user pu
            LEFT JOIN custom_role_scope crs
              ON crs.custom_role_id = pu.custom_role_id AND crs.scope = :scopeName
            WHERE pu.user_id = :userId
              AND pu.project_id IN (:projectIds)
              AND (pu.project_role = :builtInAdminOrdinal OR crs.custom_role_id IS NOT NULL)
              AND (
                SELECT COUNT(DISTINCT pu2.id)
                FROM project_user pu2
                LEFT JOIN custom_role_scope crs2
                  ON crs2.custom_role_id = pu2.custom_role_id AND crs2.scope = :scopeName
                WHERE pu2.project_id = pu.project_id
                  AND (pu2.project_role = :builtInAdminOrdinal OR crs2.custom_role_id IS NOT NULL)
              ) <= 1
        """)
    List<Long> findOrphanRiskProjectIds(
        @Param("userId") long userId, @Param("projectIds") List<Long> projectIds,
        @Param("builtInAdminOrdinal") int builtInAdminOrdinal, @Param("scopeName") String scopeName);
}
