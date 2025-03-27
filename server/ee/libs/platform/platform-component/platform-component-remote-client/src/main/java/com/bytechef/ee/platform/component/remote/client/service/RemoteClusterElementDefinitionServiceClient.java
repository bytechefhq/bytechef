/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteClusterElementDefinitionServiceClient implements ClusterElementDefinitionService {

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, Context context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getClusterElementObject(
        String componentName, int componentVersion, String clusterElementName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        ClusterElementType clusterElementType) {

        throw new UnsupportedOperationException();
    }
}
