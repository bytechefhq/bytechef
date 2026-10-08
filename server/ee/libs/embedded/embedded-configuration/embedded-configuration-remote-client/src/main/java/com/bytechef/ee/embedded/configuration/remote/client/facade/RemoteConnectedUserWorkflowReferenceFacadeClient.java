/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserWorkflowReferenceFacadeClient implements ConnectedUserWorkflowReferenceFacade {

    @Override
    public void deleteReference(String externalUserId, String automationWorkflowUuid, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableReference(
        String externalUserId, String automationWorkflowUuid, boolean enable, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String automationWorkflowUuid, Environment environment,
        Map<String, Long> requestedConnectionIds, @Nullable Map<String, ?> inputs) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateReferenceInputs(
        String externalUserId, String automationWorkflowUuid, Map<String, ?> inputs, Environment environment) {

        throw new UnsupportedOperationException();
    }
}
