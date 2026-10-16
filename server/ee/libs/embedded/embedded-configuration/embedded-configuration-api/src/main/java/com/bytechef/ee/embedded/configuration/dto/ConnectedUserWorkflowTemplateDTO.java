/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.platform.configuration.domain.WorkflowInput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserWorkflowTemplateDTO(
    String workflowUuid, String label, String description, String lastModifiedDate,
    List<Component> triggers, List<Component> components, List<Input> inputs, String permissionExpression,
    @Nullable String workflowId) {

    public ConnectedUserWorkflowTemplateDTO(
        String workflowUuid, String label, String description, String lastModifiedDate, List<Component> triggers,
        List<Component> components, List<Input> inputs, String permissionExpression) {

        this(
            workflowUuid, label, description, lastModifiedDate, triggers, components, inputs, permissionExpression,
            null);
    }

    public ConnectedUserWorkflowTemplateDTO withIntegrationIds(Map<String, Long> integrationIdsByComponentName) {
        return new ConnectedUserWorkflowTemplateDTO(
            workflowUuid, label, description, lastModifiedDate,
            resolveIntegrationIds(triggers, integrationIdsByComponentName),
            resolveIntegrationIds(components, integrationIdsByComponentName), inputs, permissionExpression,
            workflowId);
    }

    public ConnectedUserWorkflowTemplateDTO withProvided(
        Set<String> providedComponentNames, Set<String> providedInputNames) {

        return new ConnectedUserWorkflowTemplateDTO(
            workflowUuid, label, description, lastModifiedDate, markProvided(triggers, providedComponentNames),
            markProvided(components, providedComponentNames),
            inputs.stream()
                .map(input -> input.withProvided(providedInputNames.contains(input.name())))
                .toList(),
            permissionExpression, workflowId);
    }

    private static List<Component> markProvided(List<Component> components, Set<String> providedComponentNames) {
        return components.stream()
            .map(component -> component.withProvided(
                component.connectionRequired() && providedComponentNames.contains(component.name())))
            .toList();
    }

    private static List<Component> resolveIntegrationIds(
        List<Component> components, Map<String, Long> integrationIdsByComponentName) {

        return components.stream()
            .map(component -> component.connectionRequired()
                ? component.withIntegrationId(integrationIdsByComponentName.get(component.name()))
                : component)
            .toList();
    }

    /**
     * @param integrationId the id of the integration backing this component; null when the component needs a connection
     *                      but no integration visible to the user is configured, and always null when the component
     *                      needs no connection
     */
    public record Component(
        String name, String title, String icon, boolean connectionRequired, @Nullable Long integrationId,
        boolean provided) {

        public Component {
            if (!connectionRequired && integrationId != null) {
                throw new IllegalArgumentException(
                    "Component %s needs no connection and cannot carry an integration id".formatted(name));
            }

            if (!connectionRequired && provided) {
                throw new IllegalArgumentException(
                    "Component %s needs no connection and cannot have a provided one".formatted(name));
            }
        }

        public Component(
            String name, String title, String icon, boolean connectionRequired, @Nullable Long integrationId) {

            this(name, title, icon, connectionRequired, integrationId, false);
        }

        public Component(String name, String title, String icon) {
            this(name, title, icon, false, null, false);
        }

        public Component withIntegrationId(@Nullable Long integrationId) {
            return new Component(name, title, icon, connectionRequired, integrationId, provided);
        }

        public Component withProvided(boolean provided) {
            return new Component(name, title, icon, connectionRequired, integrationId, provided);
        }
    }

    /**
     * @param componentReference the component input group this input takes its value from; null when the input does not
     *                           reference a component
     */
    public record Input(
        String name, String label, String type, boolean required,
        WorkflowInput.@Nullable ComponentInputReference componentReference, boolean provided) {

        public Input(String name, String label, String type, boolean required) {
            this(name, label, type, required, null, false);
        }

        public Input(String name, String label, String type, boolean required, boolean provided) {
            this(name, label, type, required, null, provided);
        }

        public Input(
            String name, String label, String type, boolean required,
            WorkflowInput.@Nullable ComponentInputReference componentReference) {

            this(name, label, type, required, componentReference, false);
        }

        public Input withProvided(boolean provided) {
            return new Input(name, label, type, required, componentReference, provided);
        }
    }
}
