/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.ee.platform.component.remote.client.AbstractWorkerClient;
import com.bytechef.ee.remote.client.DefaultRestClient;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteComponentDefinitionFacadeClient extends AbstractWorkerClient
    implements ComponentDefinitionFacade {

    public RemoteComponentDefinitionFacadeClient(
        DefaultRestClient defaultRestClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultRestClient, discoveryClient, objectMapper);
    }

    @Override
    public List<Option> executeWorkflowInputOptions(
        String componentName, int componentVersion, String groupName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId) {

        throw new UnsupportedOperationException();
    }
}
