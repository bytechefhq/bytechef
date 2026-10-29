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
 * The connections and inputs of one configuration workflow an administrator can provide, read at the configuration's
 * pinned project version, together with the values already provided.
 *
 * @param connectionSlots the connections the workflow needs, one per component
 * @param inputs          the inputs of the workflow
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
    List<ConnectionSlot> connectionSlots, List<Input> inputs, Map<String, Long> providedConnections,
    Map<String, Object> providedInputs) {

    /**
     * @param providableConnections the connections of the component in the configuration's environment an administrator
     *                              can provide, excluding those owned by connected users
     */
    public record ConnectionSlot(
        String componentName, String title, boolean required, List<ProvidableConnection> providableConnections) {
    }

    public record ProvidableConnection(long id, String name) {
    }

    public record Input(String name, @Nullable String label, @Nullable String type, boolean required) {
    }
}
