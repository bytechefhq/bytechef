/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.action.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.component.codeworkflow.task.CodeWorkflowTaskExecutor;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
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
    public Optional<BasePerformFunction> getPerform() {
        return Optional.of((MultipleConnectionsPerformFunction) this::perform);
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ? extends ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext actionContext) {

        return codeWorkflowTaskExecutor.executePerform(
            inputParameters.getRequiredString("codeWorkflowContainerUuid"),
            inputParameters.getRequiredString("workflowName"), inputParameters.getRequiredString("taskName"),
            inputParameters.getRequired(MetadataConstants.TYPE, ModeType.class));
    }
}
