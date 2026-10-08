/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectService {

    AutomationWorkflowProject create(
        long projectId, @Nullable String permissionExpression, boolean automationHubVisible);

    void delete(long projectId);

    Optional<AutomationWorkflowProject> fetchAutomationWorkflowProject(long projectId);

    AutomationWorkflowProject getAutomationWorkflowProject(long projectId);

    List<AutomationWorkflowProject> getAutomationWorkflowProjects();

    Map<UUID, String> getWorkflowPermissionExpressions(long projectId);

    void updateAutomationHubVisible(long projectId, boolean automationHubVisible);

    void updatePermissionExpression(long projectId, @Nullable String permissionExpression);

    void updateWorkflowPermissionExpression(long projectId, UUID workflowUuid, @Nullable String permissionExpression);
}
