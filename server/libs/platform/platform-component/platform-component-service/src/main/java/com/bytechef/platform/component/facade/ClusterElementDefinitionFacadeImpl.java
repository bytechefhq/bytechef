/*
 * Copyright 2025 ByteChef
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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ClusterElementDefinitionFacadeImpl implements ClusterElementDefinitionFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;
    private final TokenRefreshHelper tokenRefreshHelper;

    @SuppressFBWarnings("EI")
    public ClusterElementDefinitionFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectionService connectionService,
        ContextFactory contextFactory, TokenRefreshHelper tokenRefreshHelper) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable Long connectionId) {

        return executeTool(
            componentName, componentVersion, clusterElementName, null, null, null, null, null, inputParameters,
            getComponentConnection(connectionId), false);
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ModeType type, @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId,
        @Nullable Long jobId, @Nullable String workflowId, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        ActionContext context = contextFactory.createActionContext(
            componentName, componentVersion, clusterElementName, type, jobPrincipalId, jobPrincipalWorkflowId,
            jobId, workflowId, componentConnection, editorEnvironment);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, context, ActionDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, actionContext1) -> clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, inputParameters, componentConnection1,
                actionContext1),
            componentConnection1 -> contextFactory.createActionContext(
                componentName, componentVersion, clusterElementName, type, jobPrincipalId, jobPrincipalWorkflowId,
                jobId, workflowId, componentConnection1, editorEnvironment));
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }
}
