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
public record AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
    List<ConnectionSlot> connectionSlots, List<Input> inputs, Map<String, Long> providedConnections,
    Map<String, Object> providedInputs) {

    public record ConnectionSlot(
        String componentName, String title, boolean required, List<ProvidableConnection> providableConnections) {
    }

    public record ProvidableConnection(long id, String name) {
    }

    public record Input(String name, @Nullable String label, @Nullable String type, boolean required) {
    }
}
