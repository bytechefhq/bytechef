/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationProject;
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
public interface AutomationProjectService {

    AutomationProject create(long projectId, @Nullable String permissionExpression);

    void delete(long projectId);

    Optional<AutomationProject> fetchAutomationProject(long projectId);

    AutomationProject getAutomationProject(long projectId);

    List<AutomationProject> getAutomationProjects();

    Map<UUID, String> getWorkflowPermissionExpressions(long projectId);

    void updatePermissionExpression(long projectId, @Nullable String permissionExpression);

    void updateWorkflowPermissionExpression(long projectId, UUID workflowUuid, @Nullable String permissionExpression);
}
