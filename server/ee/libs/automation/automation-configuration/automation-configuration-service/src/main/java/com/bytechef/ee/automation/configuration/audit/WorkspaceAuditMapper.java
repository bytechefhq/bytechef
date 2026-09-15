/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps workspace deletion and whole-user membership removal. Both remove members as a side effect, so each writes one
 * event listing the affected ids instead of one row per member.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceAuditMapper implements AuditMapper {

    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceAuditMapper(WorkspaceUserRepository workspaceUserRepository) {
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        return switch (auditInvocation.event()) {
            case WorkspaceAuditEvents.WORKSPACE_DELETED -> distinctIds(
                workspaceUserRepository.findAllByWorkspaceId((Long) auditInvocation.requireArgument("id")),
                WorkspaceUser::getUserId);
            case WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED -> distinctIds(
                workspaceUserRepository.findAllByUserId((Long) auditInvocation.requireArgument("userId")),
                WorkspaceUser::getWorkspaceId);
            default -> null;
        };
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();
        Object captured = auditInvocation.captured();

        if (WorkspaceAuditEvents.WORKSPACE_DELETED.equals(auditInvocation.event())) {
            data.put("workspaceId", String.valueOf(auditInvocation.argument("id")));

            if (captured instanceof List<?> removedUserIds) {
                data.put("removedUserIds", AuditValues.joinBounded(removedUserIds));
                data.put("removedUserCount", String.valueOf(removedUserIds.size()));
            }
        } else {
            data.put("userId", String.valueOf(auditInvocation.argument("userId")));

            if (captured instanceof List<?> workspaceIds) {
                data.put("workspaceIds", AuditValues.joinBounded(workspaceIds));
                data.put("workspaceCount", String.valueOf(workspaceIds.size()));
            }
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        // WorkspaceMembershipAssignerImpl.removeMemberships is also called from a nightly cleanup job and from
        // unauthenticated re-registration, where the user held no membership at all. A row for that no-op merely
        // records that cron ran; a real removal (or any DENIED/ERROR outcome, which the caller should always see) is
        // always worth keeping.
        if (!WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED.equals(auditInvocation.event())) {
            return true;
        }

        if (auditInvocation.outcome() != AuditOutcome.SUCCESS) {
            return true;
        }

        Object captured = auditInvocation.captured();

        return captured instanceof List<?> workspaceIds && !workspaceIds.isEmpty();
    }

    private static List<Long>
        distinctIds(List<WorkspaceUser> workspaceUsers, Function<WorkspaceUser, Long> idFunction) {
        return workspaceUsers.stream()
            .map(idFunction)
            .distinct()
            .toList();
    }
}
