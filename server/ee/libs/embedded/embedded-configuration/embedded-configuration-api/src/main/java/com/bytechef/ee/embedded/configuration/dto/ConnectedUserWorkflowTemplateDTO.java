/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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

    public record Component(String name, String title, String icon) {
    }

    public record Input(String name, String label, String type, boolean required) {
    }
}
