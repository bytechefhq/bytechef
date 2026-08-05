/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
    private final ConnectionIdHelper connectionIdHelper;
    private final ToolExecutionRecorder toolExecutionRecorder;

    @SuppressFBWarnings("EI")
    public ActionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ConnectionIdHelper connectionIdHelper,
        ToolExecutionRecorder toolExecutionRecorder) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.connectionIdHelper = connectionIdHelper;
        this.toolExecutionRecorder = toolExecutionRecorder;
    }

    @Override
    public Object executeAction(
        String externalUserId, String componentName, Integer componentVersion, String actionName,
        Map<String, Object> inputParameters, @Nullable Long instanceId, Environment environment) {

        Long connectionId = connectionIdHelper.getConnectionId(externalUserId, componentName, instanceId, environment);

        return toolExecutionRecorder.record(
            ToolExecutionEvent
                .builder(ToolExecutionSurface.EMBEDDED_API_ACTION, ToolExecutionKind.COMPONENT, actionName)
                .componentName(componentName)
                .componentVersion(componentVersion)
                .operationName(actionName)
                .connectionId(connectionId)
                .externalUserId(externalUserId)
                .integrationInstanceId(instanceId)
                .environment(environment == null ? null : environment.ordinal()),
            () -> actionDefinitionFacade.executePerform(
                componentName, componentVersion, actionName, null, null, null, null, null, inputParameters,
                connectionId == null ? Map.of() : Map.of(componentName, connectionId), Map.of(), null, null, false,
                null, null, null));
    }
}
