/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.registry.remote.client.facade;

import com.bytechef.ee.platform.workflow.task.dispatcher.registry.remote.client.service.RemoteTaskDispatcherDefinitionServiceClient;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.facade.TaskDispatcherDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskDispatcherDefinitionFacadeClient implements TaskDispatcherDefinitionFacade {

    private final RemoteTaskDispatcherDefinitionServiceClient remoteTaskDispatcherDefinitionServiceClient;

    @SuppressFBWarnings("EI")
    public RemoteTaskDispatcherDefinitionFacadeClient(
        RemoteTaskDispatcherDefinitionServiceClient remoteTaskDispatcherDefinitionServiceClient) {

        this.remoteTaskDispatcherDefinitionServiceClient = remoteTaskDispatcherDefinitionServiceClient;
    }

    @Override
    public OutputResponse executeOutput(String name, int version, Map<String, ?> inputParameters) {
        return remoteTaskDispatcherDefinitionServiceClient.executeOutput(name, version, inputParameters);
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, @Nullable Integer version) {
        return remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinition(name, version);
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return remoteTaskDispatcherDefinitionServiceClient.getTaskDispatcherDefinitions();
    }
}
