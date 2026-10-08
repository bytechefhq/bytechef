/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps {@code WorkspaceUserServiceImpl} calls to audit data. Never records the invitee's email: the user id comes from
 * the stored membership instead.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceUserAuditMapper implements AuditMapper {

    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserAuditMapper(WorkspaceUserRepository workspaceUserRepository) {
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    public Set<String> events() {
        return Set.of(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED,
            WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED,
            WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED,
            WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED);
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        // Membership is being created, so there is no before-state to read.
        if (isAdded(auditInvocation)) {
            return null;
        }

        long userId = (Long) auditInvocation.requireArgument("userId");
        long workspaceId = (Long) auditInvocation.requireArgument("workspaceId");

        return switch (auditInvocation.event()) {
            case WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED -> AuditRoles.describeAll(
                workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId));
            case WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED -> workspaceUserRepository
                .findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
                .map(AuditRoles::describe)
                .orElse(null);
            case WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED -> fetchEnvironmentOrWideRow(
                userId, workspaceId, (Environment) auditInvocation.requireArgument("environment"))
                    .map(AuditRoles::describe)
                    .orElse(null);
            case WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED -> captureEnvironmentRemoval(
                userId, workspaceId, (Environment) auditInvocation.requireArgument("environment"));
            default -> null;
        };
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        data.put("workspaceId", String.valueOf(auditInvocation.requireArgument("workspaceId")));

        Object userId = resolveUserId(auditInvocation);

        if (userId != null) {
            data.put("userId", String.valueOf(userId));
        }

        Object environment = auditInvocation.argument("environment");

        if (environment != null) {
            data.put("environment", ((Environment) environment).name());
        }

        String role = AuditRoles.describe(
            (WorkspaceRole) auditInvocation.argument("workspaceRole"), (Long) auditInvocation.argument("customRoleId"));

        if (role != null) {
            data.put("role", role);
        }

        if (auditInvocation.arguments()
            .containsKey("email")) {

            data.put("invited", "true");
        }

        Object captured = auditInvocation.captured();

        if (captured instanceof EnvironmentRemoval environmentRemoval) {
            if (environmentRemoval.previousRole() != null) {
                data.put("previousRole", environmentRemoval.previousRole());
            }

            data.put("widenedToWorkspaceWide", String.valueOf(environmentRemoval.widenedToWorkspaceWide()));
        } else if (captured != null) {
            data.put("previousRole", String.valueOf(captured));
        }

        return data;
    }

    private EnvironmentRemoval captureEnvironmentRemoval(long userId, long workspaceId, Environment environment) {
        String previousRole = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal())
            .map(AuditRoles::describe)
            .orElse(null);
        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        return new EnvironmentRemoval(previousRole, workspaceUsers.size() <= 1);
    }

    private Optional<WorkspaceUser> fetchEnvironmentOrWideRow(long userId, long workspaceId, Environment environment) {
        Optional<WorkspaceUser> environmentWorkspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

        if (environmentWorkspaceUser.isPresent()) {
            return environmentWorkspaceUser;
        }

        return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId);
    }

    private static boolean isAdded(AuditInvocation auditInvocation) {
        return WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED.equals(auditInvocation.event());
    }

    private static @Nullable Object resolveUserId(AuditInvocation auditInvocation) {
        // Invites carry no userId argument; every other event must, so a renamed parameter fails loudly.
        if (!isAdded(auditInvocation)) {
            return auditInvocation.requireArgument("userId");
        }

        Object userId = auditInvocation.argument("userId");

        if (userId != null) {
            return userId;
        }

        if (auditInvocation.result() instanceof WorkspaceUser workspaceUser) {
            return workspaceUser.getUserId();
        }

        return null;
    }

    private record EnvironmentRemoval(@Nullable String previousRole, boolean widenedToWorkspaceWide) {
    }
}
