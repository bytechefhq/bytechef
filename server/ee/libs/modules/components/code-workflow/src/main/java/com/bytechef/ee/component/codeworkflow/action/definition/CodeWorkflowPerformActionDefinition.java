/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.action.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.component.codeworkflow.task.CodeWorkflowTaskExecutor;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParameterConnection;
import com.bytechef.platform.constant.ModeType;
import java.util.Map;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowPerformActionDefinition extends AbstractActionDefinitionWrapper {

    private final CodeWorkflowTaskExecutor codeWorkflowTaskExecutor;

    public CodeWorkflowPerformActionDefinition(
        ActionDefinition actionDefinition, CodeWorkflowTaskExecutor codeWorkflowTaskExecutor) {

        super(actionDefinition);

        this.codeWorkflowTaskExecutor = codeWorkflowTaskExecutor;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.of((MultipleConnectionsPerformFunction) this::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ? extends ParameterConnection> connectionParameters,
        Parameters extensions, ActionContext actionContext) {

        return codeWorkflowTaskExecutor.executePerform(
            inputParameters.getRequiredString("codeWorkflowContainerReference"),
            inputParameters.getRequiredString("workflowName"), inputParameters.getRequiredString("taskName"),
            inputParameters.getRequired(MetadataConstants.TYPE, ModeType.class));
    }
}
