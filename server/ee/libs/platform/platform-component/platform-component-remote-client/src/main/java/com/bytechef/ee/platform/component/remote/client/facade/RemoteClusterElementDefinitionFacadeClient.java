/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteClusterElementDefinitionFacadeClient implements ClusterElementDefinitionFacade {
    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        throw new UnsupportedOperationException();
    }
}
