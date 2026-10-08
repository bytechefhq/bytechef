/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ResolvedWorkflowConnections(
    List<ProjectDeploymentWorkflowConnection> connections, List<String> missingComponentNames) {

    public ResolvedWorkflowConnections {
        connections = List.copyOf(connections);
        missingComponentNames = List.copyOf(missingComponentNames);
    }

    @Nullable
    public String firstMissingComponentName() {
        return missingComponentNames.isEmpty() ? null : missingComponentNames.getFirst();
    }

    public boolean isComplete() {
        return missingComponentNames.isEmpty();
    }
}
