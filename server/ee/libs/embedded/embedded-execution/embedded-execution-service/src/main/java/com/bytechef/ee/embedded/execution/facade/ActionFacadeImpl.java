/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper.ConnectedUserConnection;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ActionFacadeImpl implements ActionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionIdHelper connectionIdHelper;
    private final ToolExecutionRecorder toolExecutionRecorder;

    @SuppressFBWarnings("EI")
    public ActionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ComponentDefinitionService componentDefinitionService,
        ConnectionIdHelper connectionIdHelper, ToolExecutionRecorder toolExecutionRecorder) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionIdHelper = connectionIdHelper;
        this.toolExecutionRecorder = toolExecutionRecorder;
    }

    @Override
    public Object executeAction(
        String externalUserId, String componentName, Integer componentVersion, String actionName,
        Map<String, Object> inputParameters, @Nullable Long instanceId, Environment environment) {

        ConnectedUserConnection connectedUserConnection = connectionIdHelper.getConnectedUserConnection(
            externalUserId, componentName, instanceId, environment);

        Long connectionId = connectedUserConnection.connectionId();

        ToolExecutionEvent.Builder eventBuilder = ToolExecutionEvent
            .builder(
                PlatformType.EMBEDDED, ToolExecutionSurface.EMBEDDED_API_ACTION, ToolExecutionKind.COMPONENT,
                actionName)
            .componentName(componentName)
            .componentVersion(componentVersion)
            .operationName(actionName)
            .connectionId(connectionId)
            .environment(environment.ordinal())
            .externalUserId(externalUserId)
            .connectedUserId(connectedUserConnection.connectedUserId())
            .integrationInstanceId(connectedUserConnection.integrationInstanceId());

        Supplier<Object> execution = () -> actionDefinitionFacade.executePerform(
            componentName, componentVersion, actionName, null, null, null, null, null, inputParameters,
            connectionId == null ? Map.of() : Map.of(componentName, connectionId), Map.of(), null, null, false,
            null, null, null);

        if (connectionId == null && isConnectionRequired(componentName, componentVersion)) {
            toolExecutionRecorder.record(
                eventBuilder
                    .outcome(ToolExecutionOutcome.CONNECTION_REQUIRED)
                    .build());

            return execution.get();
        }

        return toolExecutionRecorder.record(eventBuilder, execution);
    }

    private boolean isConnectionRequired(String componentName, Integer componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        return componentDefinition.isConnectionRequired();
    }
}
