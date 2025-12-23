/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.service;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
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
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters) {

        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getClusterElement(String componentName, int componentVersion, String clusterElementName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(
        ClusterElementType clusterElementType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String componentOperationName, int statusCode, Object body) {

        throw new UnsupportedOperationException();
    }
}
