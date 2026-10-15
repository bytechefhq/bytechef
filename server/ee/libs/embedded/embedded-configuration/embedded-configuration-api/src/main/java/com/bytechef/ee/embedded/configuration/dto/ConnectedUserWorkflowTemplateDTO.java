/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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

    private static List<Component> resolveIntegrationIds(
        List<Component> components, Map<String, Long> integrationIdsByComponentName) {

        return components.stream()
            .map(component -> component.connectionRequired()
                ? component.withIntegrationId(integrationIdsByComponentName.get(component.name()))
                : component)
            .toList();
    }

    public record Component(
        String name, String title, String icon, boolean connectionRequired, @Nullable Long integrationId) {

        public Component(String name, String title, String icon) {
            this(name, title, icon, false, null);
        }

        public Component withIntegrationId(@Nullable Long integrationId) {
            return new Component(name, title, icon, connectionRequired, integrationId);
        }
    }

    public record Input(String name, String label, String type, boolean required) {
    }
}
