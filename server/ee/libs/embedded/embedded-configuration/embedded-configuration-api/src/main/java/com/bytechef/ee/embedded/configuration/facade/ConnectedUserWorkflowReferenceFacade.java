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

    void deleteReference(String externalUserId, String automationUuid, Environment environment);

    /**
     * Turns the connected user's workflow on or off.
     *
     * @param externalUserId the connected user's external id
     * @param automationUuid the automation workflow uuid
     * @param enable         whether to turn the workflow on
     * @param environment    the environment
     * @return the stored reference after the change
     */
    ConnectedUserProjectWorkflow enableReference(
        String externalUserId, String automationUuid, boolean enable, Environment environment);

    List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment,
        Map<String, Long> requestedConnectionIds);

    ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs);

    void updateReferenceInputs(
        String externalUserId, String automationUuid, Map<String, ?> inputs, Environment environment);
}
