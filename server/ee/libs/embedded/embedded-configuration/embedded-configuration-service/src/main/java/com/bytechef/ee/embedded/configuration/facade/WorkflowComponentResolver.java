/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves the distinct {@link ComponentDefinition}s used by a workflow's triggers and tasks. Shared by
 * {@link AutomationWorkflowProjectFacadeImpl} (catalog project/workflow listings) and
 * {@link ConnectedUserProjectFacadeImpl} (a connected user's own workflow listing, including automation-bridge
 * reference rows).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkflowComponentResolver {

    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public WorkflowComponentResolver(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    /**
     * Distinct components used by the workflow's triggers followed by its tasks, in first-seen order.
     */
    public List<ConnectedUserWorkflowTemplateDTO.Component> getComponents(Workflow workflow) {
        List<ConnectedUserWorkflowTemplateDTO.Component> triggerComponents = getTriggerComponents(workflow);
        List<ConnectedUserWorkflowTemplateDTO.Component> taskComponents = getTaskComponents(workflow);

        List<ConnectedUserWorkflowTemplateDTO.Component> components = new ArrayList<>(triggerComponents);

        for (ConnectedUserWorkflowTemplateDTO.Component taskComponent : taskComponents) {
            if (!components.contains(taskComponent)) {
                components.add(taskComponent);
            }
        }

        return components;
    }

    public List<ConnectedUserWorkflowTemplateDTO.Component> getTaskComponents(Workflow workflow) {
        Map<String, WorkflowNodeType> componentsByName = new LinkedHashMap<>();

        for (WorkflowTask workflowTask : workflow.getTasks(true)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (workflowNodeType.operation() != null
                && !componentsByName.containsKey(workflowNodeType.name())) {

                componentsByName.put(workflowNodeType.name(), workflowNodeType);
            }
        }

        return resolveComponents(componentsByName);
    }

    public List<ConnectedUserWorkflowTemplateDTO.Component> getTriggerComponents(Workflow workflow) {
        Map<String, WorkflowNodeType> componentsByName = new LinkedHashMap<>();

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (workflowNodeType.operation() != null
                && !componentsByName.containsKey(workflowNodeType.name())) {

                componentsByName.put(workflowNodeType.name(), workflowNodeType);
            }
        }

        return resolveComponents(componentsByName);
    }

    private List<ConnectedUserWorkflowTemplateDTO.Component> resolveComponents(
        Map<String, WorkflowNodeType> componentsByName) {

        List<ConnectedUserWorkflowTemplateDTO.Component> components = new ArrayList<>();

        for (WorkflowNodeType workflowNodeType : componentsByName.values()) {
            Optional<ComponentDefinition> componentDefinitionOptional =
                componentDefinitionService.fetchComponentDefinition(
                    workflowNodeType.name(), workflowNodeType.version());

            if (componentDefinitionOptional.isPresent()) {
                ComponentDefinition componentDefinition = componentDefinitionOptional.get();

                components.add(
                    new ConnectedUserWorkflowTemplateDTO.Component(
                        componentDefinition.getName(), componentDefinition.getTitle(), componentDefinition.getIcon()));
            }
        }

        return components;
    }
}
