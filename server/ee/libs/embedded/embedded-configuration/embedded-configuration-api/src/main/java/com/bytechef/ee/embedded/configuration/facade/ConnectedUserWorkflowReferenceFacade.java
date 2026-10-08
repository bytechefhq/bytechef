/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserWorkflowReferenceFacade {

    void deleteReference(String externalUserId, String automationWorkflowUuid, Environment environment);

    void enableReference(String externalUserId, String automationWorkflowUuid, boolean enable, Environment environment);

    List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs);

    void updateReferenceInputs(
        String externalUserId, String automationWorkflowUuid, Map<String, ?> inputs, Environment environment);
}
