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

package com.bytechef.ai.copilot.config;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.AskUserQuestionToolCallback;
import com.bytechef.ai.copilot.tool.CreateConnectionToolCallback;
import com.bytechef.ai.copilot.tool.ListConnectionsForComponentToolCallback;
import com.bytechef.ai.copilot.tool.LookupActionPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.LookupClusterElementPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.LookupTriggerPropertyOptionsToolCallback;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.SelectClusterElementPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SelectConnectionToolCallback;
import com.bytechef.ai.copilot.tool.SelectPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.SelectTriggerPropertyOptionToolCallback;
import com.bytechef.ai.copilot.tool.ToolStateVisibilityMetrics;
import com.bytechef.ai.copilot.tool.WorkspaceCopilotConnectionLister;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Builds the interactive picker tool callbacks once so every chat surface exposes the same pickers.
 *
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI")
public class InteractivePickerToolCallbacks {

    private final ConnectionDefinitionService connectionDefinitionService;
    private final WorkspaceConnectionFacade workspaceConnectionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final PropertyOptionsResolver propertyOptionsResolver;
    private final ObjectProvider<CopilotConnectionLister> connectionListerProvider;
    private final ScopedConnectionGuard scopedConnectionGuard;

    public InteractivePickerToolCallbacks(
        ConnectionDefinitionService connectionDefinitionService, WorkspaceConnectionFacade workspaceConnectionFacade,
        ComponentDefinitionService componentDefinitionService, ActionDefinitionService actionDefinitionService,
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade, PropertyOptionsResolver propertyOptionsResolver,
        ObjectProvider<CopilotConnectionLister> connectionListerProvider,
        ScopedConnectionGuard scopedConnectionGuard) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.workspaceConnectionFacade = workspaceConnectionFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.propertyOptionsResolver = propertyOptionsResolver;
        this.connectionListerProvider = connectionListerProvider;
        this.scopedConnectionGuard = scopedConnectionGuard;
    }

    public List<ToolCallback> read() {
        return List.of(
            listConnectionsForComponent(),
            lookupActionPropertyOptions(),
            lookupTriggerPropertyOptions(),
            lookupClusterElementPropertyOptions(),
            new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP));
    }

    public List<ToolCallback> write() {
        return List.of(
            listConnectionsForComponent(),
            new SelectConnectionToolCallback(componentDefinitionService),
            lookupActionPropertyOptions(),
            lookupTriggerPropertyOptions(),
            lookupClusterElementPropertyOptions(),
            new SelectPropertyOptionToolCallback(
                actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard),
            new SelectTriggerPropertyOptionToolCallback(
                triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard),
            new SelectClusterElementPropertyOptionToolCallback(
                clusterElementDefinitionService, clusterElementDefinitionFacade, propertyOptionsResolver,
                ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard),
            new AskUserQuestionToolCallback(ToolStateVisibilityMetrics.NOOP),
            new CreateConnectionToolCallback(componentDefinitionService));
    }

    public ToolCallback listConnectionsForComponent() {
        List<CopilotConnectionLister> connectionListers = new ArrayList<>();

        connectionListers.add(new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, propertyOptionsResolver));

        CopilotConnectionLister embeddedConnectionLister = connectionListerProvider.getIfAvailable();

        if (embeddedConnectionLister != null) {
            connectionListers.add(embeddedConnectionLister);
        }

        return new ListConnectionsForComponentToolCallback(
            componentDefinitionService, connectionDefinitionService, ToolStateVisibilityMetrics.NOOP,
            connectionListers);
    }

    private ToolCallback lookupActionPropertyOptions() {
        return new LookupActionPropertyOptionsToolCallback(
            actionDefinitionService, actionDefinitionFacade, propertyOptionsResolver,
            ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard);
    }

    private ToolCallback lookupClusterElementPropertyOptions() {
        return new LookupClusterElementPropertyOptionsToolCallback(
            clusterElementDefinitionService, clusterElementDefinitionFacade, propertyOptionsResolver,
            ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard);
    }

    private ToolCallback lookupTriggerPropertyOptions() {
        return new LookupTriggerPropertyOptionsToolCallback(
            triggerDefinitionService, triggerDefinitionFacade, propertyOptionsResolver,
            ToolStateVisibilityMetrics.NOOP, scopedConnectionGuard);
    }
}
