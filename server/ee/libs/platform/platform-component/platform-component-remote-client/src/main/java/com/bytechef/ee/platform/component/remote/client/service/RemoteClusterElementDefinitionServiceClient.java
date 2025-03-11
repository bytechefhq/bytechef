/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteClusterElementDefinitionServiceClient implements ClusterElementDefinitionService {

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementTypeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, ClusterElementType clusterElementType, String name) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getClusterElementObject(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getClusterElementObject(
        String componentName, int componentVersion, ClusterElementType clusterElementType, String name) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, ClusterElementType clusterElementType) {

        throw new UnsupportedOperationException();
    }
}
