/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectAuditMapper implements AuditMapper {

    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectAuditMapper(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public Set<String> events() {
        return Set.of(
            ProjectAuditEvents.PROJECT_ACCESS_GRANTED, ProjectAuditEvents.PROJECT_ACCESS_REVOKED,
            ProjectAuditEvents.PROJECT_VISIBILITY_CHANGED);
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        if (!ProjectAuditEvents.PROJECT_VISIBILITY_CHANGED.equals(auditInvocation.event())) {
            return null;
        }

        return projectService.fetchProject((Long) auditInvocation.requireArgument("projectId"))
            .map(Project::getVisibility)
            .orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        data.put("workspaceId", String.valueOf(auditInvocation.requireArgument("workspaceId")));
        data.put("projectId", String.valueOf(auditInvocation.requireArgument("projectId")));

        if (ProjectAuditEvents.PROJECT_VISIBILITY_CHANGED.equals(auditInvocation.event())) {
            if (auditInvocation.captured() instanceof ResourceVisibility fromVisibility) {
                data.put("fromVisibility", fromVisibility.name());
            }

            data.put("toVisibility", ((ResourceVisibility) auditInvocation.requireArgument("visibility")).name());
        } else {
            data.put("targetUserId", String.valueOf(auditInvocation.requireArgument("userId")));
        }

        return data;
    }
}
