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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps {@code WorkspaceUserServiceImpl} calls to audit data. Only ids and roles are ever recorded, never a whole
 * argument or the stored entity.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceUserAuditMapper implements AuditMapper {

    private static final String CUSTOM_ROLE_PREFIX = "customRole:";

    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserAuditMapper(WorkspaceUserRepository workspaceUserRepository) {
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    public Set<String> events() {
        return Set.of(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED,
            WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED);
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        // Membership is being created, so there is no before-state to read.
        if (auditInvocation.event()
            .equals(WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED)) {

            return null;
        }

        long userId = (Long) auditInvocation.requireArgument("userId");
        long workspaceId = (Long) auditInvocation.requireArgument("workspaceId");

        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .map(WorkspaceUserAuditMapper::describeRole)
            .orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        data.put("workspaceId", String.valueOf(auditInvocation.requireArgument("workspaceId")));
        data.put("userId", String.valueOf(auditInvocation.requireArgument("userId")));

        if (auditInvocation.argument("workspaceRole") instanceof WorkspaceRole workspaceRole) {
            data.put("role", workspaceRole.name());
        }

        Object captured = auditInvocation.captured();

        if (captured != null) {
            data.put("previousRole", String.valueOf(captured));
        }

        return data;
    }

    private static String describeRole(WorkspaceUser workspaceUser) {
        Long customRoleId = workspaceUser.getCustomRoleId();

        if (customRoleId != null) {
            return CUSTOM_ROLE_PREFIX + customRoleId;
        }

        Integer workspaceRoleOrdinal = workspaceUser.getWorkspaceRole();
        WorkspaceRole[] workspaceRoles = WorkspaceRole.values();

        if (workspaceRoleOrdinal == null || workspaceRoleOrdinal < 0 || workspaceRoleOrdinal >= workspaceRoles.length) {
            return "UNKNOWN";
        }

        return workspaceRoles[workspaceRoleOrdinal].name();
    }
}
