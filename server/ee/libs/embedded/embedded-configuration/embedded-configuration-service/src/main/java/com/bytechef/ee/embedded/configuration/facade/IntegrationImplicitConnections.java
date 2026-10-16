/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class IntegrationImplicitConnections {

    private IntegrationImplicitConnections() {
    }

    static List<WorkflowDefinition> addTo(
        List<WorkflowDefinition> workflowDefinitions, String componentName, int componentVersion) {

        ImplicitConnectionRequirement implicitConnectionRequirement = new ImplicitConnectionRequirement(
            componentName, componentVersion);

        return workflowDefinitions.stream()
            .map(workflowDefinition -> (WorkflowDefinition) new ImplicitConnectionWorkflowDefinition(
                workflowDefinition, implicitConnectionRequirement))
            .toList();
    }

    private static WorkflowTaskDefinition toWorkflowTaskDefinition(
        WorkflowTaskDefinition workflowTaskDefinition, ImplicitConnectionRequirement implicitConnectionRequirement) {

        if (workflowTaskDefinition instanceof CompositeTaskDefinition compositeTaskDefinition) {
            return new ImplicitConnectionCompositeTaskDefinition(compositeTaskDefinition,
                implicitConnectionRequirement);
        }

        if (workflowTaskDefinition instanceof TaskDefinition taskDefinition) {
            return toTaskDefinition(taskDefinition, implicitConnectionRequirement);
        }

        return workflowTaskDefinition;
    }

    private static boolean isOwnConnection(
        ConnectionRequirement connectionRequirement, ImplicitConnectionRequirement implicitConnectionRequirement) {

        String componentName = implicitConnectionRequirement.getComponentName();

        return Objects.equals(connectionRequirement.getComponentName(), componentName) ||
            Objects.equals(connectionRequirement.getName(), componentName);
    }

    private static TaskDefinition toTaskDefinition(
        TaskDefinition taskDefinition, ImplicitConnectionRequirement implicitConnectionRequirement) {

        List<? extends ConnectionRequirement> declaredConnectionRequirements = taskDefinition.getConnections()
            .orElseGet(List::of);

        boolean declaresOwnConnection = declaredConnectionRequirements.stream()
            .anyMatch(connectionRequirement -> isOwnConnection(connectionRequirement, implicitConnectionRequirement));

        if (declaresOwnConnection) {
            return taskDefinition;
        }

        List<ConnectionRequirement> connectionRequirements = new ArrayList<>(declaredConnectionRequirements);

        connectionRequirements.add(implicitConnectionRequirement);

        return new ImplicitConnectionTaskDefinition(taskDefinition, List.copyOf(connectionRequirements));
    }

    private static List<TaskDefinition> toTaskDefinitions(
        List<? extends TaskDefinition> taskDefinitions, ImplicitConnectionRequirement implicitConnectionRequirement) {

        return taskDefinitions.stream()
            .map(taskDefinition -> toTaskDefinition(taskDefinition, implicitConnectionRequirement))
            .toList();
    }

    private record ImplicitConnectionRequirement(String componentName, int componentVersion)
        implements ConnectionRequirement {

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public OptionalInt getComponentVersion() {
            return OptionalInt.of(componentVersion);
        }

        @Override
        public String getName() {
            return componentName;
        }
    }

    @SuppressFBWarnings("EI")
    private record ImplicitConnectionTaskDefinition(
        TaskDefinition taskDefinition, List<ConnectionRequirement> connectionRequirements) implements TaskDefinition {

        @Override
        public Optional<List<? extends ConnectionRequirement>> getConnections() {
            return Optional.of(connectionRequirements);
        }

        @Override
        public Optional<String> getDescription() {
            return taskDefinition.getDescription();
        }

        @Override
        public Optional<String> getLabel() {
            return taskDefinition.getLabel();
        }

        @Override
        public String getName() {
            return taskDefinition.getName();
        }

        @Override
        public Optional<Map<String, ?>> getParameters() {
            return taskDefinition.getParameters();
        }

        @Override
        public PerformFunction getPerform() {
            return taskDefinition.getPerform();
        }
    }

    private record ImplicitConnectionCompositeTaskDefinition(
        CompositeTaskDefinition compositeTaskDefinition, ImplicitConnectionRequirement implicitConnectionRequirement)
        implements CompositeTaskDefinition {

        @Override
        public List<? extends List<? extends TaskDefinition>> getBranches() {
            return compositeTaskDefinition.getBranches()
                .stream()
                .map(branch -> toTaskDefinitions(branch, implicitConnectionRequirement))
                .toList();
        }

        @Override
        public Optional<String> getDescription() {
            return compositeTaskDefinition.getDescription();
        }

        @Override
        public Optional<String> getLabel() {
            return compositeTaskDefinition.getLabel();
        }

        @Override
        public String getName() {
            return compositeTaskDefinition.getName();
        }

        @Override
        public List<? extends TaskDefinition> getTasks() {
            return toTaskDefinitions(compositeTaskDefinition.getTasks(), implicitConnectionRequirement);
        }

        @Override
        public Type getType() {
            return compositeTaskDefinition.getType();
        }
    }

    private record ImplicitConnectionWorkflowDefinition(
        WorkflowDefinition workflowDefinition, ImplicitConnectionRequirement implicitConnectionRequirement)
        implements WorkflowDefinition {

        @Override
        public Optional<String> getDescription() {
            return workflowDefinition.getDescription();
        }

        @Override
        public Optional<List<? extends Input>> getInputs() {
            return workflowDefinition.getInputs();
        }

        @Override
        public Optional<String> getLabel() {
            return workflowDefinition.getLabel();
        }

        @Override
        public String getName() {
            return workflowDefinition.getName();
        }

        @Override
        public Optional<List<? extends Output>> getOutputs() {
            return workflowDefinition.getOutputs();
        }

        @Override
        public Optional<List<? extends WorkflowTaskDefinition>> getTasks() {
            return workflowDefinition.getTasks()
                .map(workflowTaskDefinitions -> workflowTaskDefinitions.stream()
                    .map(workflowTaskDefinition -> toWorkflowTaskDefinition(workflowTaskDefinition,
                        implicitConnectionRequirement))
                    .toList());
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return workflowDefinition.getTriggers();
        }
    }
}
