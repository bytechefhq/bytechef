/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.facade;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ClusterElementDefinitionFacadeImpl implements ClusterElementDefinitionFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ContextFactory contextFactory;
    private final TokenRefreshHelper tokenRefreshHelper;

    public ClusterElementDefinitionFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory,
        TokenRefreshHelper tokenRefreshHelper) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.contextFactory = contextFactory;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        Context context = contextFactory.createContext(componentName, componentConnection, editorEnvironment);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, context, ActionDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, actionContext1) -> clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, inputParameters, componentConnection1,
                actionContext1),
            componentConnection1 -> contextFactory.createContext(
                componentName, componentConnection, editorEnvironment));

    }
}
