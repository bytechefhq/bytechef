/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectAuditMapperTest {

    private ProjectAuditMapper projectAuditMapper;
    private ProjectService projectService;

    @BeforeEach
    void beforeEach() {
        projectService = mock(ProjectService.class);
        projectAuditMapper = new ProjectAuditMapper(projectService);
    }

    @Test
    void testVisibilityChangedRecordsBothRungs() {
        Project project = new Project();

        project.setVisibility(ResourceVisibility.WORKSPACE);

        when(projectService.fetchProject(4L)).thenReturn(Optional.of(project));

        Map<String, Object> arguments = Map.of(
            "workspaceId", 1L, "projectId", 4L, "visibility", ResourceVisibility.PRIVATE);

        Object captured = projectAuditMapper.capture(
            invocation(ProjectAuditEvents.PROJECT_VISIBILITY_CHANGED, arguments, null));

        assertThat(
            projectAuditMapper.map(invocation(ProjectAuditEvents.PROJECT_VISIBILITY_CHANGED, arguments, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of("workspaceId", "1", "projectId", "4", "fromVisibility", "WORKSPACE", "toVisibility",
                        "PRIVATE"));
    }

    @Test
    void testAccessRevokedRecordsTheTargetUser() {
        Map<String, Object> arguments = Map.of("workspaceId", 1L, "projectId", 4L, "userId", 9L);

        assertThat(projectAuditMapper.capture(invocation(ProjectAuditEvents.PROJECT_ACCESS_REVOKED, arguments, null)))
            .isNull();
        assertThat(projectAuditMapper.map(invocation(ProjectAuditEvents.PROJECT_ACCESS_REVOKED, arguments, null)))
            .containsExactlyInAnyOrderEntriesOf(Map.of("workspaceId", "1", "projectId", "4", "targetUserId", "9"));
    }

    private static AuditInvocation invocation(String event, Map<String, Object> arguments, Object captured) {
        return new AuditInvocation(event, arguments, null, captured, AuditOutcome.SUCCESS, null);
    }
}
