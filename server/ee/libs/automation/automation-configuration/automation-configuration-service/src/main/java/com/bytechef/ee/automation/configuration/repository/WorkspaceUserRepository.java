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

    void deleteByUserIdAndWorkspaceId(long userId, long workspaceId);

    List<WorkspaceUser> findAllByUserId(long userId);

    List<WorkspaceUser> findAllByWorkspaceId(long workspaceId);

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
