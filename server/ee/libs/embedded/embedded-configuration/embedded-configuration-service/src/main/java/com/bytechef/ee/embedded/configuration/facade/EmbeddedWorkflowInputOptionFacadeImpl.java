/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnEEVersion
public class EmbeddedWorkflowInputOptionFacadeImpl implements EmbeddedWorkflowInputOptionFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedWorkflowInputOptionFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationWorkflowService integrationWorkflowService, TriggerDefinitionFacade triggerDefinitionFacade,
        WorkflowService workflowService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public List<Option> getWorkflowInputOptions(
        long integrationInstanceId, String workflowUuid, String inputName, String propertyName,
        Map<String, ?> lookupDependsOnValues, String searchText) {

        IntegrationInstance integrationInstance =
            integrationInstanceService.getIntegrationInstance(integrationInstanceId);

        long connectionId = integrationInstance.getConnectionId();

        String workflowId = integrationWorkflowService.getWorkflowId(integrationInstanceId, workflowUuid);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Workflow.Input input = workflow.getInputs()
            .stream()
            .filter(currentInput -> inputName.equals(currentInput.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown workflow input: " + inputName));

        Workflow.ComponentInputReference componentReference = input.componentReference();

        if (componentReference == null) {
            throw new IllegalArgumentException("Workflow input does not reference a component: " + inputName);
        }

        String componentName = componentReference.componentName();
        int componentVersion =
            componentReference.componentVersion() == null ? 1 : componentReference.componentVersion();

        List<String> lookupDependsOnPaths = List.copyOf(lookupDependsOnValues.keySet());

        NodeReference nodeReference = findNodeReference(workflow, componentName);

        if (nodeReference.trigger()) {
            return triggerDefinitionFacade.executeOptions(
                componentName, componentVersion, nodeReference.operation(), propertyName, lookupDependsOnValues,
                lookupDependsOnPaths, searchText, connectionId);
        }

        return actionDefinitionFacade.executeOptions(
            componentName, componentVersion, nodeReference.operation(), propertyName, lookupDependsOnValues,
            lookupDependsOnPaths, searchText, connectionId);
    }

    private static NodeReference findNodeReference(Workflow workflow, String componentName) {
        for (WorkflowTask workflowTask : workflow.getTasks(false)) {
            NodeReference nodeReference = toNodeReference(workflowTask.getType(), componentName, false);

            if (nodeReference != null) {
                return nodeReference;
            }
        }

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            NodeReference nodeReference = toNodeReference(workflowTrigger.getType(), componentName, true);

            if (nodeReference != null) {
                return nodeReference;
            }
        }

        throw new IllegalArgumentException("No workflow node uses component: " + componentName);
    }

    private static NodeReference toNodeReference(String type, String componentName, boolean trigger) {
        String[] parts = type.split("/");

        if (parts.length >= 3 && componentName.equals(parts[0])) {
            return new NodeReference(parts[2], trigger);
        }

        return null;
    }

    private record NodeReference(String operation, boolean trigger) {
    }
}
