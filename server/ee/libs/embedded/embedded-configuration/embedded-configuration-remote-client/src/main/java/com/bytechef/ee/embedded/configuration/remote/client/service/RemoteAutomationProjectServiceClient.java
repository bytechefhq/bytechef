/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.AutomationProject;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAutomationProjectServiceClient implements AutomationProjectService {

    @Override
    public AutomationProject create(long projectId, @Nullable String permissionExpression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AutomationProject> fetchAutomationProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProject getAutomationProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProject> getAutomationProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<UUID, String> getWorkflowPermissionExpressions(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updatePermissionExpression(long projectId, @Nullable String permissionExpression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflowPermissionExpression(
        long projectId, UUID workflowUuid, @Nullable String permissionExpression) {

        throw new UnsupportedOperationException();
    }
}
